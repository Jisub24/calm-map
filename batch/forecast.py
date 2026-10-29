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
         측정 없음 · 앞뒤 실측 사이를 이을 수 있음 → 후보 (점선)
         둘 다 안 됨                           → 정보 부족
       후보라도 점수가 부담 높음이면 '부담 높음' 으로 표시한다 (점선은 그대로).
         답변서2 질의1: '무던함' 은 실측으로 확인된 시간대에만, '부담 높음' 주의는 추정이어도 표시.
         표에는 status='level' · level='high' · is_estimated=true 로 쓴다
    5. 실측 없는 칸은 지역 혼잡 예측의 "모양"을 빌려 추정한다 (답변서2 질의1)
         실측이 있었던 시각의 지역 혼잡 단계와 점수를 짝지어 두고,
         추정할 시각의 혼잡 예측 단계가 그 사이에 들어오면 직선으로 잇는다.
         범위 밖이면 지어내지 않고 정보 부족.
         시각마다 바뀌는 건 장소 혼잡 · 소리 크기뿐이다. 밝기 · 소리 종류(배경음악 · 안내방송)는
         장소 고유값이라 그 장소 측정 전체의 중앙값으로 고정한다
    6. 칸마다 지역 혼잡 1~4 를 붙인다 (선 그래프용. 점수에는 안 들어간다)

slot_forecast — 지도 '시간 선택' 용. 날짜 없이 장소 × 평일/주말 × 시각 (026)
    점수     위 2~4 와 같다. 빈 칸은 채우지 않는다 (그날의 혼잡 예측이 없다)
    주변 혼잡 congestion 실측을 그 묶음 날들에서 같은 시각끼리 모은 중앙값.
             하루 한 값(같은 시각 두 값이면 더 붐빈 쪽), 두 단계 사이면 올림(낮게 안내하지 않게)

실행
    python batch/forecast.py                          오늘(한국 시각) + slot_forecast · DB 측정으로 · 저장
    python batch/forecast.py --date 2026-10-27        hourly_forecast 를 그 날짜로
    python batch/forecast.py --sheet analysis/측정결과.csv --dry-run
                                                      답사 녹음 분석 결과로 돌려 보고 저장은 안 함
"""

import argparse
import csv
import math
import statistics
from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path

from calculate_score import CALM, HIGH, KOR, level_of, rows_from_sheet, score_measurement
from common import get_settings, require_env, sb_select, sb_upsert

KST = timezone(timedelta(hours=9))
ROOT = Path(__file__).resolve().parent.parent
RECORD_SHEET = ROOT / "analysis/측정기록.csv"

FACTORS = ("loudness", "sound", "light", "crowd")
GROUPS = ("weekday", "weekend")
GROUP_WEEKDAYS = {"weekday": (1, 2, 3, 4, 5), "weekend": (6, 7)}
# 장소 고유값 (답변서2 질의1). 보간 칸에서도 시각마다 바꾸지 않는다
PLACE_FIXED = ("light", "sound")
# 영업시간을 모르는 장소(앱 검색으로 넣은 곳)에 쓰는 하루. 앱 그래프 범위 8~22시와 같다
DEFAULT_HOURS = {"is_closed": False, "open_time": "08:00:00", "close_time": "23:00:00", "closes_next_day": False}


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


def load_area_history(area_cds):
    """지역 혼잡 실측 전부 → {(area, 평일/주말, 시각): (대표 단계, 날 수)}. slot_forecast 의 주변 혼잡.

    같은 날 같은 시각에 두 번 받았으면 더 붐빈 쪽을 그날 값으로 하고,
    날들의 중앙값이 두 단계 사이(2.5)면 올린다. 낮게 안내하지 않으려는 것.
    """
    rows = select_all("congestion", {
        "select": "area_cd,target_at,level",
        "area_cd": "in.(" + ",".join(area_cds) + ")", "is_forecast": "eq.false", "order": "target_at",
    })
    per_day = {}
    for r in rows:
        t = parse_ts(r["target_at"]).astimezone(KST)
        key = (r["area_cd"], t.date(), t.hour)
        per_day[key] = max(per_day.get(key, 0), r["level"])
    by = defaultdict(list)
    for (area, d, h), level in per_day.items():
        by[(area, weekday_group(d), h)].append(level)
    return {k: (math.ceil(statistics.median(v)), len(v)) for k, v in by.items()}


# -----------------------------------------------------
# 칸 채우기
# -----------------------------------------------------

def slot_hour(m):
    """측정 시작 + 길이/2 가 들어가는 시각. 14:50 에 시작한 20분 측정은 15시 칸."""
    mid = m["measured_at"].astimezone(KST) + timedelta(seconds=m["duration_sec"] / 2)
    return mid.hour


def with_total(out):
    """항목 점수 → 종합(최댓값) · 주 요인. 소리 크기 · 소리 종류가 둘 다 없으면 종합을 내지 않는다."""
    present = {f: out[f"score_{f}"] for f in FACTORS if out[f"score_{f}"] is not None}
    if out["score_loudness"] is None and out["score_sound"] is None:
        out["total_score"], out["main_factor"] = None, None
    else:
        main = max(present, key=present.get)
        out["total_score"], out["main_factor"] = present[main], main
    return out


def median_scores(scored):
    """칸 안의 측정 점수들 → 항목별 중앙값, 종합 = 중앙값들의 최댓값."""
    out = {}
    for f in FACTORS:
        vals = [r[f"score_{f}"] for r in scored if r[f"score_{f}"] is not None]
        out[f"score_{f}"] = statistics.median(vals) if vals else None
    return with_total(out)


def place_fixed(scored_place):
    """장소 고유값 — 밝기 · 소리 종류. 그 장소 측정 전체(평일 · 주말 모두)의 중앙값 (답변서2 질의1)."""
    out = {}
    for f in PLACE_FIXED:
        vals = [r[f"score_{f}"] for r in scored_place if r[f"score_{f}"] is not None]
        out[f"score_{f}"] = statistics.median(vals) if vals else None
    return out


def with_place_light(agg, fixed):
    """칸 측정 대신 장소 전체(평일 · 주말 모두)의 밝기로 바꾸고 종합을 다시 낸다.

    밝기는 장소 고유값이라 평일 · 주말 · 시각으로 나누지 않는다 (답변서2 질의1).
    batch 가 돌 때마다 다시 내므로 측정이 쌓이면 가운데 값도 따라 바뀐다.
    """
    if fixed["score_light"] is None:
        return agg
    return with_total({**agg, "score_light": fixed["score_light"]})


def judge(n, total_sec, total, s):
    """실측이 있는 칸의 상태 · 단계 · 점선 여부.

    무던함은 확인된 때만, 부담 높음은 추정이어도 — 이 비대칭이 답변서 핵심 (답변서2 질의1).
    """
    if total is None:
        return "insufficient", None, False
    lv = level_of(total, s)
    if n >= s["min_measurements"]:
        if lv == CALM and total_sec < s["calm_min_total_sec"]:
            return "estimate", None, True        # 조용했지만 합 10분 미만 → 후보
        return "level", lv, False
    return estimated(lv)                         # 1~2건 → 후보 (부담 높음은 표시)


def estimated(lv):
    """후보 칸. 부담 높음이면 단계로 알리되 점선(추정)으로 둔다. 나머지는 단계 이름 없이 후보."""
    if lv == HIGH:
        return "level", HIGH, True
    return "estimate", None, True


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
    return with_total(out)


def score_all(measurements, s):
    """측정마다 점수 · 칸(평일/주말 · 시각)을 붙인다. 1분 미만 같은 버리는 측정은 빠진다."""
    scored = []
    for m in measurements:
        r = score_measurement(m, s)
        if r is None:
            continue
        r["place_id"] = m["place_id"]; r["measured_at"] = m["measured_at"]
        r["duration_sec"] = m["duration_sec"]; r["hour"] = slot_hour(m)
        r["group"] = weekday_group(m["measured_at"].astimezone(KST))
        scored.append(r)
    return scored


def round_scores(row):
    for k in ("total_score",) + tuple(f"score_{f}" for f in FACTORS):
        if row[k] is not None:
            row[k] = round(row[k], 1)


def build_rows(target_date, places, hours_by_place, scored, congestion, s):
    group = weekday_group(target_date)
    weekday = target_date.isoweekday()
    rows = []

    for p in places:
        pid, area = p["id"], p["congestion_area_cd"]
        hours = open_hours(hours_by_place.get((pid, weekday)))
        # 같은 요일 묶음(평일 / 주말)의 측정만 쓴다. 시연 장소 예외(022)는 없앴다 (025)
        mine = [r for r in scored if r["place_id"] == pid and r["group"] == group]
        fixed = place_fixed([r for r in scored if r["place_id"] == pid])
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
                agg = with_place_light(median_scores(got), fixed)
                status, level, est = judge(len(got), sum(r["duration_sec"] for r in got), agg["total_score"], s)
                row.update(agg)
                row.update({"measurement_count": len(got),
                            "last_measured_at": max(r["measured_at"] for r in got).isoformat()})
            else:
                agg = interpolate(points, area_level)
                if agg:
                    agg = with_total({**agg, **fixed})   # 밝기 · 소리 종류는 장소 고유값
                    row.update(agg); status, level, est = estimated(level_of(agg["total_score"], s))
                else:
                    # 종합은 내지 않고 밝기만 둔다. 빛 탭이 평일에도 밝기 단계를 보여줄 수 있게
                    row["score_light"] = fixed["score_light"]
                    status, level, est = "insufficient", None, False
            row.update({"status": status, "level": level, "is_estimated": est})
            round_scores(row)
            rows.append(row)
    return rows


def group_hours(hours_by_place, pid, group):
    """그 묶음 요일 중 하루라도 여는 시각. 지금 장소들은 묶음 안 요일끼리 영업시간이 같다."""
    return sorted({h for d in GROUP_WEEKDAYS[group] for h in open_hours(hours_by_place.get((pid, d)))})


def build_slot_rows(places, hours_by_place, scored, area_history, s):
    """slot_forecast 줄. 날짜 없이 장소 × 평일/주말 × 시각. 측정한 칸만 점수, 빈 칸은 정보 부족.
    밝기는 어느 칸이든 장소 전체 가운데 값 (with_place_light)."""
    rows = []
    computed_at = datetime.now(KST).isoformat()
    for p in places:
        pid, area = p["id"], p["congestion_area_cd"]
        fixed = place_fixed([r for r in scored if r["place_id"] == pid])
        for group in GROUPS:
            by_hour = defaultdict(list)
            for r in scored:
                if r["place_id"] == pid and r["group"] == group:
                    by_hour[r["hour"]].append(r)
            for h in group_hours(hours_by_place, pid, group):
                area_level, area_days = area_history.get((area, group, h), (None, 0))
                row = {"place_id": pid, "day_group": group, "hour": h,
                       "area_crowd_level": area_level, "area_days": area_days,
                       "measurement_count": 0, "last_measured_at": None, "computed_at": computed_at,
                       "total_score": None, "main_factor": None}
                for f in FACTORS:
                    row[f"score_{f}"] = None
                got = by_hour.get(h)
                if got:
                    agg = with_place_light(median_scores(got), fixed)
                    status, level, est = judge(len(got), sum(r["duration_sec"] for r in got), agg["total_score"], s)
                    row.update(agg)
                    row.update({"measurement_count": len(got),
                                "last_measured_at": max(r["measured_at"] for r in got).isoformat()})
                else:
                    row["score_light"] = fixed["score_light"]
                    status, level, est = "insufficient", None, False
                row.update({"status": status, "level": level, "is_estimated": est})
                round_scores(row)
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
    ap.add_argument("--date", help="YYYY-MM-DD. hourly_forecast 를 이 날짜로. 기본은 오늘(한국 시각)")
    ap.add_argument("--sheet", help="DB 대신 읽을 측정 csv (analysis/측정결과.csv)")
    ap.add_argument("--dry-run", action="store_true", help="저장하지 않고 출력만")
    args = ap.parse_args()

    require_env()
    s = get_settings()
    target = date.fromisoformat(args.date) if args.date else datetime.now(KST).date()

    places = sb_select("places", {"select": "id,name,congestion_area_cd", "order": "id"})
    hours_by_place = {(h["place_id"], h["weekday"]): h for h in sb_select("place_hours", {"select": "*"})}
    # 영업시간 칸이 없으면 측정해도 점수 칸이 안 생겨서 8~22시를 연 것으로 본다
    has_hours = {pid for pid, _ in hours_by_place}
    for p in places:
        if p["id"] not in has_hours:
            hours_by_place.update({(p["id"], d): DEFAULT_HOURS for d in range(1, 8)})
    measurements = load_measurements_sheet(args.sheet) if args.sheet else load_measurements_db()
    scored = score_all(measurements, s)
    # 1km 안에 혼잡 지역이 없는 장소는 지역이 비어 있다
    area_cds = sorted({p["congestion_area_cd"] for p in places if p["congestion_area_cd"]})

    earliest = min([m["measured_at"] for m in measurements] + [datetime.combine(target, time(0), KST)])
    congestion = load_congestion(area_cds, earliest - timedelta(hours=1))

    rows = build_rows(target, places, hours_by_place, scored, congestion, s)
    print(f"{target} ({'주말' if weekday_group(target) == 'weekend' else '평일'}) · 측정 {len(measurements)}건 · 칸 {len(rows)}개")
    print_rows(rows, places)

    slot_rows = build_slot_rows(places, hours_by_place, scored, load_area_history(area_cds), s)
    for group in GROUPS:
        print(f"\n── slot_forecast · {'평일' if group == 'weekday' else '주말'} ──")
        print_rows([r for r in slot_rows if r["day_group"] == group], places)

    if args.dry_run:
        print("\n(저장하지 않음)")
        return
    n = sb_upsert("hourly_forecast", rows, on_conflict="place_id,date,hour")
    print(f"\nhourly_forecast 에 {n}줄 저장")
    n = sb_upsert("slot_forecast", slot_rows, on_conflict="place_id,day_group,hour")
    print(f"slot_forecast 에 {n}줄 저장")


if __name__ == "__main__":
    main()
