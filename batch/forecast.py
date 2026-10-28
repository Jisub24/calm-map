"""시간대별 예측 + 상태 판정 — 30분마다.

측정 여러 건을 장소 × 시간대 칸으로 모아 hourly_forecast 에 쓴다.
앱의 그래프 막대 · 결론 문구 · 지도 마커가 전부 이 테이블을 읽는다.

칸 하나를 채우는 순서
    1. 장소의 오늘 영업시간을 1시간 칸으로 자른다 (place_hours)
    2. 같은 요일 묶음(평일/주말) · 같은 시각의 과거 측정을 모은다
       측정의 칸 = 측정 시작 + 길이/2 가 들어가는 시각
    3. 측정마다 calculate_score 로 점수를 내고, 칸 안에서 항목별 중앙값 → 종합은 그 최댓값
    4. 상태 판정 (CLAUDE.md 원칙 6 — 측정이 부족하면 지어내지 않는다)
         측정 3건 이상                         → 단계 (무던함 · 보통 · 부담 높음)
           단, 무던함은 측정 시간 합이 10분 이상일 때만. 모자라면 '후보'
         측정 1~2건                            → 후보 (점수는 보여주되 점선)
         측정 없음 · 앞뒤 실측 사이를 이을 수 있음 → 추정 (점선)
         둘 다 안 됨                           → 정보 부족
    5. 실측 없는 칸은 지역 혼잡 예측의 "모양"을 빌려 추정한다 (답변서 질의 2)
         실측이 있었던 시각의 지역 혼잡 단계와 점수를 짝지어 두고,
         추정할 시각의 혼잡 예측 단계가 그 사이에 들어오면 직선으로 잇는다.
         범위 밖이면 지어내지 않고 정보 부족.
    6. 칸마다 지역 혼잡 1~4 를 붙인다 (선 그래프용. 점수에는 안 들어간다)

실행
    python batch/forecast.py                          오늘 (한국 시각) · DB 측정으로 · 저장
    python batch/forecast.py --date 2026-10-27        그 날짜로
    python batch/forecast.py --sheet analysis/측정결과.csv --dry-run
                                                      답사 녹음 분석 결과로 돌려 보고 저장은 안 함
"""

import argparse
import csv
import statistics
from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path

from calculate_score import CALM, KOR, level_of, rows_from_sheet, score_measurement
from common import get_settings, require_env, sb_select, sb_upsert

KST = timezone(timedelta(hours=9))
ROOT = Path(__file__).resolve().parent.parent
RECORD_SHEET = ROOT / "analysis/측정기록.csv"

FACTORS = ("loudness", "sound", "light", "crowd")


# -----------------------------------------------------
# 읽기
# -----------------------------------------------------

def parse_ts(s):
    return datetime.fromisoformat(s.replace("Z", "+00:00"))


def weekday_group(d):
    """평일 / 주말. 장소당 측정이 적어 요일별로는 칸을 못 채운다 (답변서 질의 2)."""
    return "weekend" if d.isoweekday() >= 6 else "weekday"


def open_hours(hours_row):
    """place_hours 한 줄 → 영업 중인 시각 목록. 09:00~22:30 이면 9~22시."""
    if hours_row is None or hours_row["is_closed"] or not hours_row["open_time"]:
        return []
    o = time.fromisoformat(hours_row["open_time"])
    if hours_row["closes_next_day"] or hours_row["close_time"].startswith("24:"):
        last = 23          # 자정 넘김 · 24시간 영업 (PostgreSQL 은 24:00:00 을 그대로 돌려준다)
    else:
        c = time.fromisoformat(hours_row["close_time"])
        last = c.hour if c.minute > 0 else c.hour - 1
    return list(range(o.hour, last + 1))


def load_measurements_db():
    rows = sb_select("measurements", {"select": "*", "order": "measured_at"})
    for m in rows:
        m["measured_at"] = parse_ts(m["measured_at"])
    return rows


def load_measurements_sheet(path):
    """답사 녹음 분석 결과(측정결과.csv) + 시작 시각(측정기록.csv)."""
    started = {}
    with open(RECORD_SHEET, encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            started[int(r["measurement_no"])] = parse_ts(r["started_at"])
    out = []
    for m in rows_from_sheet(path):
        m["measured_at"] = started[m["id"]]
        out.append(m)
    return out


def select_all(table, params):
    """Supabase 는 한 번에 최대 1000줄만 준다. 넘어가면 이어서 받는다."""
    out, offset = [], 0
    while True:
        page = sb_select(table, {**params, "limit": "1000", "offset": str(offset)})
        out += page
        if len(page) < 1000:
            return out
        offset += 1000


def load_congestion(area_cds, since):
    """지역 혼잡. {(area, 시각 KST 정각): level}.

    지난 시각은 실측(is_forecast=false), 앞으로의 시각은 가장 최근에 받은 예측을 쓴다.
    예측은 30분마다 12줄씩 쌓여서 오래된 예측까지 다 읽으면 수천 줄이라 최근 것만 읽는다.
    """
    areas = "in.(" + ",".join(area_cds) + ")"
    actual = select_all("congestion", {
        "select": "area_cd,target_at,level",
        "area_cd": areas, "is_forecast": "eq.false",
        "target_at": "gte." + since.isoformat(), "order": "target_at",
    })
    recent = (datetime.now(timezone.utc) - timedelta(hours=2)).isoformat()
    forecast = select_all("congestion", {
        "select": "area_cd,target_at,level",
        "area_cd": areas, "is_forecast": "eq.true",
        "base_at": "gte." + recent, "order": "base_at",
    })
    table = {}
    for r in forecast + actual:          # 실측을 뒤에 넣어 같은 시각이면 실측이 이긴다
        t = parse_ts(r["target_at"]).astimezone(KST).replace(minute=0, second=0, microsecond=0)
        table[(r["area_cd"], t)] = r["level"]
    return table


# -----------------------------------------------------
# 칸 채우기
# -----------------------------------------------------

def slot_hour(m):
    """측정 시작 + 길이/2 가 들어가는 시각. 14:50 에 시작한 20분 측정은 15시 칸."""
    mid = m["measured_at"].astimezone(KST) + timedelta(seconds=m["duration_sec"] / 2)
    return mid.hour


def median_scores(scored):
    """칸 안의 측정 점수들 → 항목별 중앙값, 종합 = 중앙값들의 최댓값."""
    out = {}
    for f in FACTORS:
        vals = [r[f"score_{f}"] for r in scored if r[f"score_{f}"] is not None]
        out[f"score_{f}"] = statistics.median(vals) if vals else None
    present = {f: out[f"score_{f}"] for f in FACTORS if out[f"score_{f}"] is not None}
    if out["score_loudness"] is None and out["score_sound"] is None:
        out["total_score"], out["main_factor"] = None, None
    else:
        main = max(present, key=present.get)
        out["total_score"], out["main_factor"] = present[main], main
    return out


def judge(n, total_sec, total, s, demo=False):
    """상태 · 단계 · 점선 여부.

    무던함은 확인된 때만, 부담 높음은 추정이어도 — 이 비대칭이 답변서 핵심.
    demo(시연 장소)는 측정이 적어도 1건부터 단계로 둔다 (settings.demo_places, 022).
    """
    if total is None:
        return "insufficient", None, False
    if demo:
        return "level", level_of(total, s), False
    if n >= s["min_measurements"]:
        lv = level_of(total, s)
        if lv == CALM and total_sec < s["calm_min_total_sec"]:
            return "estimate", None, True        # 조용했지만 합 10분 미만 → 후보
        return "level", lv, False
    return "estimate", None, True                # 1~2건 → 후보


def interpolate(points, c_target):
    """지역 혼잡 단계 → 점수. 실측 (단계, 점수들) 사이에 들어오면 직선, 밖이면 None.

    두 실측이 같은 단계면 그 단계에서만 쓴다. 점수가 혼잡과 반대로 움직이는
    쌍(혼잡 높은데 점수 낮음)은 잇지 않는다 (답변서: 반대 방향이면 정보 부족).
    """
    if c_target is None or not points:
        return None
    exact = [p for p in points if p[0] == c_target]
    if exact:
        return median_scores([p[1] for p in exact])
    lower = [p for p in points if p[0] < c_target]
    upper = [p for p in points if p[0] > c_target]
    if not lower or not upper:
        return None
    lo = max(lower, key=lambda p: p[0]); hi = min(upper, key=lambda p: p[0])
    a = median_scores([p[1] for p in points if p[0] == lo[0]])
    b = median_scores([p[1] for p in points if p[0] == hi[0]])
    if a["total_score"] is None or b["total_score"] is None or b["total_score"] < a["total_score"]:
        return None
    w = (c_target - lo[0]) / (hi[0] - lo[0])
    out = {}
    for f in FACTORS:
        x, y = a[f"score_{f}"], b[f"score_{f}"]
        out[f"score_{f}"] = None if x is None or y is None else x + (y - x) * w
    present = {f: out[f"score_{f}"] for f in FACTORS if out[f"score_{f}"] is not None}
    main = max(present, key=present.get)
    out["total_score"], out["main_factor"] = present[main], main
    return out


def build_rows(target_date, places, hours_by_place, measurements, congestion, s):
    group = weekday_group(target_date)
    weekday = target_date.isoweekday()
    rows = []

    scored = []
    for m in measurements:
        r = score_measurement(m, s)
        if r is None:
            continue
        r["place_id"] = m["place_id"]; r["measured_at"] = m["measured_at"]
        r["duration_sec"] = m["duration_sec"]; r["hour"] = slot_hour(m)
        r["group"] = weekday_group(m["measured_at"].astimezone(KST))
        scored.append(r)

    for p in places:
        pid, area = p["id"], p["congestion_area_cd"]
        hours = open_hours(hours_by_place.get((pid, weekday)))
        # 시연 장소는 평일 · 주말을 가리지 않고 모든 측정을 쓴다 (022)
        demo = pid in set(s.get("demo_places", []))
        mine = [r for r in scored if r["place_id"] == pid and (demo or r["group"] == group)]
        by_hour = defaultdict(list)
        for r in mine:
            by_hour[r["hour"]].append(r)

        # 실측 시각의 지역 혼잡 단계 ↔ 점수 (추정에 쓴다)
        points = []
        for r in mine:
            t = r["measured_at"].astimezone(KST).replace(minute=0, second=0, microsecond=0)
            c = congestion.get((area, t))
            if c is not None:
                points.append((c, r))

        for h in hours:
            at = datetime.combine(target_date, time(h), KST)
            area_level = congestion.get((area, at))
            row = {"place_id": pid, "date": target_date.isoformat(), "hour": h,
                   "area_crowd_level": area_level, "measurement_count": 0,
                   "last_measured_at": None, "computed_at": datetime.now(KST).isoformat()}
            for f in FACTORS:
                row[f"score_{f}"] = None
            row.update({"total_score": None, "main_factor": None})

            got = by_hour.get(h)
            if got:
                agg = median_scores(got)
                status, level, est = judge(len(got), sum(r["duration_sec"] for r in got), agg["total_score"], s, demo)
                row.update(agg)
                row.update({"measurement_count": len(got),
                            "last_measured_at": max(r["measured_at"] for r in got).isoformat()})
            elif demo:
                # 시연 장소: 측정한 시각만 단계, 나머지 시각은 지어내지 않고 정보 부족 (022)
                status, level, est = "insufficient", None, False
            else:
                agg = interpolate(points, area_level)
                if agg:
                    row.update(agg); status, level, est = "estimate", None, True
                else:
                    status, level, est = "insufficient", None, False
            row.update({"status": status, "level": level, "is_estimated": est})
            for k in ("total_score",) + tuple(f"score_{f}" for f in FACTORS):
                if row[k] is not None:
                    row[k] = round(row[k], 1)
            rows.append(row)
    return rows


# -----------------------------------------------------
# 실행
# -----------------------------------------------------

STATUS_KOR = {"level": "단계", "estimate": "추정", "insufficient": "정보 부족"}


def print_rows(rows, places):
    names = {p["id"]: p["name"] for p in places}
    for pid in dict.fromkeys(r["place_id"] for r in rows):
        print(f"\n■ {names[pid]}")
        for r in (x for x in rows if x["place_id"] == pid):
            if r["status"] == "insufficient":
                what = "정보 부족"
            else:
                tag = KOR[r["level"]] if r["level"] else ("후보" if r["measurement_count"] else "추정")
                what = f"{r['total_score']:5.1f}  {tag:<5} 주요인 {KOR[r['main_factor']]:<8} (측정 {r['measurement_count']})"
            crowd = f"지역 {r['area_crowd_level']}" if r["area_crowd_level"] else "지역 -"
            print(f"  {r['hour']:2d}시  {what:<40} {crowd}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--date", help="YYYY-MM-DD. 기본은 오늘(한국 시각)")
    ap.add_argument("--sheet", help="DB 대신 읽을 측정 csv (analysis/측정결과.csv)")
    ap.add_argument("--dry-run", action="store_true", help="저장하지 않고 출력만")
    args = ap.parse_args()

    require_env()
    s = get_settings()
    target = date.fromisoformat(args.date) if args.date else datetime.now(KST).date()

    places = sb_select("places", {"select": "id,name,congestion_area_cd", "order": "id"})
    hours_by_place = {(h["place_id"], h["weekday"]): h for h in sb_select("place_hours", {"select": "*"})}
    measurements = load_measurements_sheet(args.sheet) if args.sheet else load_measurements_db()

    earliest = min([m["measured_at"] for m in measurements] + [datetime.combine(target, time(0), KST)])
    congestion = load_congestion(sorted({p["congestion_area_cd"] for p in places}), earliest - timedelta(hours=1))

    rows = build_rows(target, places, hours_by_place, measurements, congestion, s)
    print(f"{target} ({'주말' if weekday_group(target) == 'weekend' else '평일'}) · 측정 {len(measurements)}건 · 칸 {len(rows)}개")
    print_rows(rows, places)

    if args.dry_run:
        print("\n(저장하지 않음)")
        return
    n = sb_upsert("hourly_forecast", rows, on_conflict="place_id,date,hour")
    print(f"\nhourly_forecast 에 {n}줄 저장")


if __name__ == "__main__":
    main()
