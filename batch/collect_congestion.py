"""혼잡도 수집 — 1시간마다.

서울시 실시간 인구데이터 API 에서 지역별 현재 혼잡도와 향후 12시간 예측을 받아
congestion 테이블에 쌓는다. 혼잡도는 가게가 아니라 지역 단위다.

한 지역을 한 번 호출하면 줄 13개가 나온다.
  실측  1줄  is_forecast = false,  target_at = base_at
  예측 12줄  is_forecast = true,   target_at = 앞으로의 매시 정각

실행:
  cd batch
  python collect_congestion.py
"""

import sys
import time
from urllib.parse import quote

import requests

from common import KST, SEOUL_API_KEY, require_env, sb_count, sb_select, sb_upsert

# API 가 주는 단계 글자를 congestion.level 숫자로 바꾼다.
# 글자(level_text)도 함께 저장하므로, 서울시가 단계 이름을 바꿔도 다시 매길 수 있다.
LEVELS = {"여유": 1, "보통": 2, "약간 붐빔": 3, "붐빔": 4}

# 호출 사이에 쉬는 시간. 121곳을 한꺼번에 쏘면 서울시 서버에 부담이 되고,
# 짧은 시간에 몰린 요청으로 보여 막힐 수 있다.
SLEEP_SEC = 0.3
TIMEOUT_SEC = 20


def to_timestamptz(text):
    """'2026-10-12 21:35' → '2026-10-12T21:35:00+09:00'

    시간대를 안 붙이면 Supabase 가 UTC 로 읽어서 9시간 틀어진다.
    밤 21시 데이터가 낮 12시 데이터로 저장되는 셈이라, 요일·시간대별
    예측이 전부 어긋난다.
    """
    return text.strip().replace(" ", "T") + ":00" + KST


def fetch_area(area_nm):
    """지역 하나를 호출해 응답 본문을 돌려준다.

    서울시 API 는 지역을 코드가 아니라 '이름'으로 받는다.
    이름에 공백이 있어 quote 로 주소에 넣을 수 있는 형태로 바꾼다.
    """
    url = (
        f"http://openapi.seoul.go.kr:8088/{SEOUL_API_KEY}"
        f"/json/citydata_ppltn/1/5/{quote(area_nm)}"
    )
    response = requests.get(url, timeout=TIMEOUT_SEC)
    response.raise_for_status()
    body = response.json()

    items = body.get("SeoulRtd.citydata_ppltn")
    if not items:
        # 키가 틀렸거나 지역 이름이 목록에 없을 때 여기로 온다.
        # 상태 코드는 200 이고 본문에만 사유가 들어 있어 따로 확인해야 한다.
        result = body.get("RESULT") or {}
        raise RuntimeError(
            f"데이터 없음 (CODE={result.get('CODE', '?')} {result.get('MESSAGE', '')})"
        )
    return items[0]


def to_rows(area_cd, item):
    """응답 하나를 congestion 테이블 줄 13개로 바꾼다."""
    base_at = to_timestamptz(item["PPLTN_TIME"])
    rows = []

    level_text = item["AREA_CONGEST_LVL"]
    rows.append(
        {
            "area_cd": area_cd,
            "base_at": base_at,
            "target_at": base_at,  # 실측은 받은 시각이 곧 대상 시각
            "level": LEVELS.get(level_text),
            "level_text": level_text,
            "ppltn_min": int(item["AREA_PPLTN_MIN"]),
            "ppltn_max": int(item["AREA_PPLTN_MAX"]),
            "is_forecast": False,
        }
    )

    for forecast in item.get("FCST_PPLTN") or []:
        level_text = forecast["FCST_CONGEST_LVL"]
        rows.append(
            {
                "area_cd": area_cd,
                "base_at": base_at,
                "target_at": to_timestamptz(forecast["FCST_TIME"]),
                "level": LEVELS.get(level_text),
                "level_text": level_text,
                "ppltn_min": int(forecast["FCST_PPLTN_MIN"]),
                "ppltn_max": int(forecast["FCST_PPLTN_MAX"]),
                "is_forecast": True,
            }
        )

    # 모르는 단계 글자가 오면 level 이 비어서 not null 제약에 막힌다.
    # 그때 DB 오류를 보는 것보다, 여기서 어떤 글자였는지 알려주는 편이 고치기 쉽다.
    unknown = {row["level_text"] for row in rows if row["level"] is None}
    if unknown:
        raise RuntimeError(f"모르는 혼잡도 단계: {sorted(unknown)}")
    return rows


def main():
    require_env()

    # 수집할 지역만 고른다. collect_interval_hours 를 0 으로 바꾸면 그 지역은 빠진다.
    areas = sb_select(
        "congestion_areas",
        {
            "select": "area_cd,area_nm",
            "collect_interval_hours": "gt.0",
            "order": "area_cd",
        },
    )
    if not areas:
        print("[중단] congestion_areas 가 비어 있습니다.")
        print("       supabase/seed/congestion_areas.csv 를 먼저 넣으세요.")
        sys.exit(1)

    before = sb_count("congestion")
    print(f"수집 대상 {len(areas)}곳 · 현재 congestion {before:,}줄")
    print("진행 (. 성공 / X 실패)")

    saved = 0
    failed = []
    mismatched = []

    for index, area in enumerate(areas, start=1):
        try:
            item = fetch_area(area["area_nm"])

            # 우리 CSV 의 코드와 API 가 주는 코드가 다르면, 엉뚱한 지역 이름으로
            # 저장될 수 있다. 저장은 하되 끝에 모아서 알려준다.
            api_cd = item.get("AREA_CD")
            if api_cd and api_cd != area["area_cd"]:
                mismatched.append(
                    f"{area['area_nm']} — 우리 {area['area_cd']} / API {api_cd}"
                )

            rows = to_rows(area["area_cd"], item)
            saved += sb_upsert("congestion", rows, "area_cd,target_at,is_forecast")
            mark = "."
        except Exception as error:
            # 한 곳이 실패해도 나머지 120곳은 저장되어야 한다.
            failed.append(f"{area['area_cd']} {area['area_nm']}: {error}")
            mark = "X"

        print(mark, end="", flush=True)
        if index % 40 == 0 or index == len(areas):
            print(f"  {index}/{len(areas)}")
        time.sleep(SLEEP_SEC)

    after = sb_count("congestion")
    print()
    print(f"성공 {len(areas) - len(failed)}곳 / 실패 {len(failed)}곳")
    print(f"보낸 줄 {saved:,}개 · congestion {before:,} → {after:,}줄 (+{after - before:,})")

    if mismatched:
        print(f"\n코드가 다른 지역 {len(mismatched)}곳 — CSV 를 고쳐야 합니다")
        for line in mismatched[:10]:
            print("  ", line)

    if failed:
        print(f"\n실패 {len(failed)}곳")
        for line in failed[:10]:
            print("  ", line)
        if len(failed) > 10:
            print(f"   … 그 외 {len(failed) - 10}곳")

    # 전부 실패했을 때만 오류로 끝낸다.
    # 몇 곳 실패로 오류를 내면 GitHub Actions 가 매시간 실패 메일을 보내는데,
    # 서울시 서버가 잠깐 느린 것만으로도 그렇게 되어 알림이 쓸모없어진다.
    if len(failed) == len(areas):
        sys.exit(1)


if __name__ == "__main__":
    main()
