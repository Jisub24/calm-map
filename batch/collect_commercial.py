"""상권 정보 수집 — 20분마다.

서울시 실시간 도시데이터에서 지역별 상권 정보를 받아 commercial /
commercial_categories 에 쌓는다.

혼잡도(collect_congestion.py)와 무엇이 다른가
  혼잡도  통신사 기지국 신호 → 그 지역에 몇 명 "있나"
  상권    신한카드 결제      → 그 지역 매장에서 몇 번 "샀나"

길에 있는 사람과 가게에 들어간 사람은 다르다. 우리가 알고 싶은 것은
"이 매장 안이 시끄러운가"라서 결제 쪽이 더 가깝다.

그리고 상권은 업종별로 나온다. 지역 단위로는 이걸 구분할 수 없다.

    홍대입구역 지역 전체   보통
      한식                분주한    ← 지역보다 2단계 높다
      제과/커피            한산한

지역 값만 쓰면 식당에 "보통"을 붙이게 된다. 실제로는 분주한데.
부담을 낮게 잘못 안내하는 오류다 (docs/12_답변서_반영.md).

실행:
  python batch/collect_commercial.py
"""

import sys
import time
from urllib.parse import quote

import requests

from common import KST, SEOUL_API_KEY, get_settings, require_env, sb_count, sb_upsert

# API 가 주는 단계 글자를 숫자로 바꾼다.
# 혼잡도(여유/보통/약간 붐빔/붐빔)와 이름이 다르니 섞지 않는다.
LEVELS = {"한산한": 1, "보통": 2, "바쁜": 3, "분주한": 4}

SLEEP_SEC = 0.3
TIMEOUT_SEC = 90  # citydata 는 127KB 라 느리다


def to_timestamptz(text):
    """'20261014 1650' → '2026-10-14T16:50:00+09:00'

    서울시는 한국 시각을 시간대 표시 없이 준다. 그대로 저장하면 UTC 로 읽혀
    9시간 틀어진다. 오류가 안 나서 더 위험하다.
    """
    t = text.strip()
    return f"{t[0:4]}-{t[4:6]}-{t[6:8]}T{t[9:11]}:{t[11:13]}:00{KST}"


def fetch_area(area_nm):
    """지역 하나의 상권 정보를 받는다.

    혼잡도는 가벼운 citydata_ppltn(3KB)을 쓰는데, 상권은 거기 없고
    citydata(127KB)에만 있다. 그래서 40배 무겁고 느리다.
    """
    url = (
        f"http://openapi.seoul.go.kr:8088/{SEOUL_API_KEY}"
        f"/json/citydata/1/5/{quote(area_nm)}"
    )
    response = requests.get(url, timeout=TIMEOUT_SEC)
    response.raise_for_status()
    body = response.json()

    city = body.get("CITYDATA")
    if not city:
        # 키가 틀려도 200 이 온다. 사유는 본문에만 있다.
        result = body.get("RESULT") or {}
        raise RuntimeError(
            f"데이터 없음 (CODE={result.get('CODE', '?')} {result.get('MESSAGE', '')})"
        )

    cmrcl = city.get("LIVE_CMRCL_STTS")
    if not cmrcl or not cmrcl.get("AREA_CMRCL_LVL"):
        # 공원·고궁은 상권 정보가 아예 없다. 오류가 아니라 정상이다.
        return None
    return cmrcl


def to_rows(area_cd, cmrcl):
    """응답을 commercial 1줄 + commercial_categories 여러 줄로 바꾼다."""
    base_at = to_timestamptz(cmrcl["CMRCL_TIME"])

    level_text = cmrcl["AREA_CMRCL_LVL"]
    if level_text not in LEVELS:
        raise RuntimeError(f"모르는 상권 단계: {level_text}")

    def as_int(value):
        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    area_row = {
        "area_cd": area_cd,
        "base_at": base_at,
        "level": LEVELS[level_text],
        "level_text": level_text,
        "payment_cnt": as_int(cmrcl.get("AREA_SH_PAYMENT_CNT")),
        "payment_amt_min": as_int(cmrcl.get("AREA_SH_PAYMENT_AMT_MIN")),
        "payment_amt_max": as_int(cmrcl.get("AREA_SH_PAYMENT_AMT_MAX")),
    }

    cat_rows = []
    unknown = set()
    for item in cmrcl.get("CMRCL_RSB") or []:
        text = item.get("RSB_PAYMENT_LVL")
        if text not in LEVELS:
            unknown.add(text)
            continue
        cat_rows.append(
            {
                "lrg_ctgr": item["RSB_LRG_CTGR"],
                "mid_ctgr": item["RSB_MID_CTGR"],
                "level": LEVELS[text],
                "level_text": text,
                "payment_cnt": as_int(item.get("RSB_SH_PAYMENT_CNT")),
            }
        )
    if unknown:
        raise RuntimeError(f"모르는 업종 단계: {sorted(unknown)}")

    return area_row, cat_rows


def save(area_row, cat_rows):
    """지역 줄을 먼저 넣고, 돌아온 id 로 업종 줄을 붙인다.

    commercial_categories 가 commercial_id 를 필요로 해서 순서가 정해져 있다.
    measurements → measurement_labels 와 같은 구조다.
    """
    from common import SUPABASE_SERVICE_KEY, SUPABASE_URL

    response = requests.post(
        f"{SUPABASE_URL}/rest/v1/commercial?on_conflict=area_cd,base_at",
        headers={
            "apikey": SUPABASE_SERVICE_KEY,
            "Authorization": f"Bearer {SUPABASE_SERVICE_KEY}",
            "Content-Type": "application/json",
            "Prefer": "resolution=merge-duplicates,return=representation",
        },
        json=[area_row],
        timeout=30,
    )
    if response.status_code >= 300:
        raise RuntimeError(f"commercial 저장 실패 {response.status_code}: {response.text[:200]}")

    commercial_id = response.json()[0]["id"]
    for row in cat_rows:
        row["commercial_id"] = commercial_id

    sb_upsert(
        "commercial_categories", cat_rows, "commercial_id,lrg_ctgr,mid_ctgr"
    )
    return 1 + len(cat_rows)


def main():
    require_env()

    # 어느 지역을 모을지는 DB 에 둔다. 코드를 고치지 않고 바꾸기 위해서다.
    # 82곳 전부 모으면 하루 70,000줄이라 권역만 모은다.
    settings = get_settings()
    area_cds = settings.get("commercial_areas")
    if not area_cds:
        print("[중단] settings 에 commercial_areas 가 없습니다.")
        print("       supabase/migrations/015_commercial.sql 을 먼저 실행하세요.")
        sys.exit(1)

    from common import sb_select

    areas = sb_select(
        "congestion_areas",
        {"select": "area_cd,area_nm", "area_cd": f"in.({','.join(area_cds)})",
         "order": "area_cd"},
    )
    if len(areas) != len(area_cds):
        found = {a["area_cd"] for a in areas}
        print("[경고] congestion_areas 에 없는 코드:", sorted(set(area_cds) - found))

    before = sb_count("commercial")
    print(f"수집 대상 {len(areas)}곳 · 현재 commercial {before:,}줄")

    saved = 0
    failed = []
    no_data = []

    for area in areas:
        try:
            cmrcl = fetch_area(area["area_nm"])
            if cmrcl is None:
                no_data.append(area["area_nm"])
                print(f"  {area['area_nm']:<18} 상권 정보 없음")
                continue
            area_row, cat_rows = to_rows(area["area_cd"], cmrcl)
            saved += save(area_row, cat_rows)
            print(
                f"  {area['area_nm']:<18} {area_row['level_text']:<5}"
                f" 업종 {len(cat_rows)}개"
            )
        except Exception as error:
            # 한 곳이 실패해도 나머지는 저장되어야 한다
            failed.append(f"{area['area_cd']} {area['area_nm']}: {error}")
            print(f"  {area['area_nm']:<18} 실패")
        time.sleep(SLEEP_SEC)

    after = sb_count("commercial")
    print()
    print(f"성공 {len(areas) - len(failed) - len(no_data)}곳 / 상권 없음 {len(no_data)}곳"
          f" / 실패 {len(failed)}곳")
    print(f"보낸 줄 {saved:,}개 · commercial {before:,} → {after:,}줄")

    if failed:
        print("\n실패")
        for line in failed:
            print("  ", line)

    # 전부 실패했을 때만 오류로 끝낸다.
    # 몇 곳 실패로 오류를 내면 실패 알림이 잦아져서 진짜 고장도 묻힌다.
    if failed and len(failed) == len(areas):
        sys.exit(1)


if __name__ == "__main__":
    main()
