"""감각부하 점수 계산 — 측정 1건을 점수로 바꾼다.

측정 1건(measurements 한 줄)에서 하위 항목 점수 3개를 내고, 그중 최댓값을 종합으로 한다.

    소리 크기      Leq(에너지 평균 dB)            → 0~100
    소리 종류·돌발  돌발 횟수 · 튄 정도 · 안내방송  → 0~100   (셋 중 높은 쪽)
    밝기           조도 중앙값(lux)                → 0~100   (미측정이면 비움)
    장소 혼잡      측정자 설문 3단계               → 25 · 60 · 90 (모름이면 비움)
    종합           위 넷의 최댓값. 평균이 아니다
    지역 혼잡      서울시 4단계                    → 종합에 넣지 않고 그래프 아래 띠로만

기준값(몇 dB 가 100점인지 등)은 코드에 적지 않고 settings 테이블에서 읽는다.
안드로이드 기기로 다시 재서 기준이 바뀌면 DB 값만 고치고 batch 를 다시 돌린다.
그래서 측정별 점수를 저장하지 않는다 — 저장해 두면 옛 기준으로 낸 숫자가 남는다.
시간대 칸으로 모아 hourly_forecast 에 저장하는 것은 forecast.py 가 한다.

실행
    python batch/calculate_score.py                     DB 의 측정 전부를 점수로 바꿔 출력
    python batch/calculate_score.py --sheet 파일.csv    답사 녹음 분석 결과(analysis/측정결과.csv)로 출력
"""

import argparse
import csv
import math
import sys
from pathlib import Path

from common import get_settings, require_env, sb_select

# 단계 이름. hourly_forecast.level 과 같은 글자를 쓴다
CALM, NORMAL, HIGH = "calm", "normal", "high"



# -----------------------------------------------------
# 값 → 점수 변환
# -----------------------------------------------------

def clamp(x, low=0.0, high=100.0):
    return max(low, min(high, x))


def piecewise(points, x):
    """꺾은선으로 바꾼다. points 는 [[입력, 점수], ...] 오름차순.

    두 점 사이는 직선으로 잇고, 양 끝 바깥은 끝 점수로 고정한다.
    예) [[0,0],[2,40],[5,70],[8,100]] 에서 x=3.5 → 40 + (3.5-2)/(5-2)×30 = 55
    """
    if x <= points[0][0]:
        return float(points[0][1])
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        if x <= x1:
            return float(y0 + (x - x0) / (x1 - x0) * (y1 - y0))
    return float(points[-1][1])


def score_loudness(leq_db, s):
    """소리 크기. Leq 가 db_zero 면 0점, db_full 이면 100점, 사이는 직선.

    Leq 를 쓰는 이유: dB 는 로그 눈금이라 숫자를 그냥 평균 내면 시끄러운 순간이
    조용한 시간에 묻힌다. 9분 55dB + 1분 85dB 가 산술 평균 58, Leq 75 다.
    소음 측정 표준(KS/ISO 1996)도 등가소음도 Leq 를 쓴다.
    """
    if leq_db is None:
        return None
    zero, full = s["score_sound_level"]["db_zero"], s["score_sound_level"]["db_full"]
    return clamp((leq_db - zero) / (full - zero) * 100)


def rate_per(count, duration_sec, rule):
    """10분당 횟수. 짧은 측정은 floor_sec(5분)으로 친다.

    1분 측정에서 그릇 소리 한 번이면 10분당 10번이 되어 바로 100점이 된다.
    짧게 재서 운 나쁘게 한 번 걸린 것이 긴 측정보다 높게 나오면 안 된다.
    """
    effective = max(duration_sec, rule["floor_sec"])
    return count / effective * (rule["per_min"] * 60)


def announce_times(announce_count, announce_level, s):
    """안내방송이 몇 번이었나. 버튼 횟수가 있으면 그것, 없으면 설문 답을 횟수로.

    설문 답 0~3 (없었어요 / 1~2번 / 3~5번 / 6번 이상)은 settings 의 level_counts
    (0 · 1.5 · 4 · 7)로 바꾼다. 둘 다 비어 있으면 "모름" → None.
    """
    if announce_count is not None and announce_count > 0:
        return float(announce_count)
    if announce_level is not None:
        return float(s["score_announce_rate"]["level_counts"][announce_level])
    if announce_count == 0:
        return 0.0
    return None


def weighted_sudden(sudden_count, sudden_strong_count):
    """돌발 횟수. 배경보다 10dB 이상 튄 것은 1번, 그보다 작은 것은 0.5번.

    라벨로 찾되 답변서의 "10dB 이상" 기준을 무게로 살린다. 컵 내려놓는 소리(+5dB)
    세 번으로 부담 높음이 나오면 안 되고, 그릇이 크게 부딪힌 소리(+15dB)는 그대로 센다.
    """
    strong = sudden_strong_count or 0
    return strong + ((sudden_count or 0) - strong) * 0.5


def score_sound(sudden_count, sudden_strong_count, sudden_max_rise,
                announce_count, announce_level, duration_sec, s):
    """소리 종류·돌발. 세 가지 중 가장 높은 점수.

      ① 돌발 횟수  10분당 몇 번 (알림음 + 부딪힘 + 경보 + 울음. 앱이 라벨로 센다. 작은 것은 0.5)
      ② 튄 정도    돌발 중 배경(LA90)보다 가장 크게 튄 dB
      ③ 안내방송   10분당 몇 번 (측정자가 버튼 또는 설문으로. 모름이면 안 넣음)

    더하지 않고 최댓값을 쓰는 이유: 알림음 뒤에 안내방송이 나오면 ①과 ③이
    같은 사건을 두 번 세게 된다. 종합을 최댓값으로 내는 것과 같은 원칙이다.
    """
    parts = {}
    parts["돌발 횟수"] = piecewise(s["score_sudden_rate"]["points"],
                                rate_per(weighted_sudden(sudden_count, sudden_strong_count),
                                         duration_sec, s["score_sudden_rate"]))
    if sudden_max_rise is not None:
        parts["튄 정도"] = piecewise(s["score_sudden_rise"]["points"], sudden_max_rise)
    times = announce_times(announce_count, announce_level, s)
    if times is not None:
        rule = s["score_announce_rate"]
        parts["안내방송"] = piecewise(rule["points"], rate_per(times, duration_sec, rule))
    best = max(parts, key=parts.get)
    return parts[best], best, parts


def score_light(lux_median, lux_status, s):
    """밝기. lux_zero 면 0점, lux_full 이면 100점, 사이는 로그.

    로그로 하는 이유: 눈은 밝기를 배수로 느낀다 (300→600 과 1000→2000 이 비슷하게
    "두 배"). 직선으로 하면 카페(300~500)가 전부 0점 근처에 몰린다.
    미측정 · 측정실패면 점수를 내지 않는다. 0점이 아니라 "모름"이다.
    """
    if lux_status != "측정유효" or lux_median is None or lux_median <= 0:
        return None
    zero, full = s["score_light"]["lux_zero"], s["score_light"]["lux_full"]
    return clamp(math.log(lux_median / zero) / math.log(full / zero) * 100)


def score_crowd(crowd_level, s):
    """장소 혼잡. 측정자 설문 0 · 1 · 2 (빈자리 많음 / 반쯤 / 거의 다 참) → 25 · 60 · 90.

    서울시 지역 혼잡을 쓰지 않는 이유: 동네 단위라 같은 동네 가게가 전부 같은 값이 된다.
    10/22 에 넣어 보니 홍대입구역 "약간 붐빔" 때문에 매장 측정과 상관없이 전부 부담 높음이 됐다.
    모름(비움)이면 점수를 내지 않는다.
    """
    if crowd_level is None:
        return None
    return float(s["score_crowd_level"][crowd_level])


def level_of(total, s):
    """점수 → 단계. 40 미만 무던함, 70 미만 보통, 그 위 부담 높음."""
    if total is None:
        return None
    t = s["level_thresholds"]
    return CALM if total < t["calm"] else NORMAL if total < t["normal"] else HIGH


def combine(loudness, sound, light, crowd):
    """종합 = 넷의 최댓값. 비어 있는 항목은 뺀다.

    평균이 아닌 이유: 아이마다 민감한 자극이 다르다. 소리 종류 80 · 밝기 30 을
    평균 내면 55(보통)가 되어 안내방송에 민감한 아이가 겪을 부담이 사라진다.
    소리 크기 · 소리 종류가 둘 다 비면 종합을 내지 않는다 (밝기 · 혼잡만으로는 모른다).
    """
    scores = {"loudness": loudness, "sound": sound, "light": light, "crowd": crowd}
    present = {k: v for k, v in scores.items() if v is not None}
    if loudness is None and sound is None:
        return None, None
    main = max(present, key=present.get)
    return present[main], main


# -----------------------------------------------------
# 측정 1건 → 점수 묶음
# -----------------------------------------------------

_warned_models = set()


def device_offset(device_model, s):
    """기종 보정값 (db 는 더하고, lux 는 곱해서 기준 기종 단위로). 모르는 기종은 0 · 1.

    기준은 갤럭시 SM-F766 (앱이 쓰는 안드로이드 마이크 · 조도센서). 아이폰으로 잰 답사 7건처럼
    다른 방식의 측정은 먼저 그 단위로 바꿔야 한다. 키는 device_model 의 앞부분과 맞으면 된다.
    """
    table = s.get("device_offsets", {})
    for key, off in table.items():
        if device_model and device_model.startswith(key):
            return off.get("db", 0.0), off.get("lux", 1.0)
    if device_model not in _warned_models:
        _warned_models.add(device_model)
        print(f"[경고] 기종 '{device_model}' 의 보정값이 settings.device_offsets 에 없어 보정 없이 계산합니다")
    return 0.0, 1.0


def score_measurement(m, s):
    """measurements 한 줄(사전)을 점수로 바꾼다. 너무 짧으면 None.

    쓰는 칸: device_model, duration_sec, avg_db(=Leq), sudden_count, sudden_strong_count,
            sudden_max_rise, announce_count, announce_level, crowd_level,
            avg_lux(=조도 중앙값), lux_status
    """
    if m["duration_sec"] < s["measure_min_sec"]:
        return None
    db_add, lux_mul = device_offset(m.get("device_model"), s)
    loudness = score_loudness(m["avg_db"] + db_add, s)
    sound, sound_from, sound_parts = score_sound(
        m.get("sudden_count"), m.get("sudden_strong_count"), m.get("sudden_max_rise"),
        m.get("announce_count"), m.get("announce_level"),
        m["duration_sec"], s)
    lux = None if m.get("avg_lux") is None else m["avg_lux"] * lux_mul
    light = score_light(lux, m.get("lux_status"), s)
    crowd = score_crowd(m.get("crowd_level"), s)
    total, main = combine(loudness, sound, light, crowd)
    return {
        "score_loudness": loudness,
        "score_sound": sound,
        "score_sound_from": sound_from,     # 돌발 횟수 / 튄 정도 / 안내방송 중 무엇이 점수를 정했나
        "score_sound_parts": sound_parts,
        "score_light": light,
        "score_crowd": crowd,
        "total_score": total,
        "main_factor": main,
        "level": level_of(total, s),
    }


# -----------------------------------------------------
# 실행
# -----------------------------------------------------

KOR = {"loudness": "소리 크기", "sound": "소리 종류·돌발", "light": "밝기", "crowd": "장소 혼잡",
       CALM: "무던함", NORMAL: "보통", HIGH: "부담 높음", None: "-"}


def _num(x):
    return None if x in (None, "") else float(x)


def rows_from_sheet(path):
    """analysis/측정결과.csv — 녹음 분석 숫자를 measurements 줄 모양으로 바꾼다."""
    with open(path, encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            yield {
                "id": int(r["measurement_no"]),
                "place_id": int(r["place_id"]),
                "place_name": r["place_name"],
                "duration_sec": int(r["duration_sec"]),
                "device_model": r.get("device_model", "iPhone 15"),
                "avg_db": _num(r["leq_db"]),
                "background_db": _num(r["la90_db"]),
                "sudden_count": int(r["sudden_count"]),
                "sudden_strong_count": int(r["sudden_strong_count"]),
                "alert_count": int(r["alert_count"]),
                "sudden_max_rise": _num(r["sudden_max_rise"]),
                "announce_count": None if r["announce_count"] == "" else int(r["announce_count"]),
                "announce_level": None if r["announce_level"] == "" else int(r["announce_level"]),
                "crowd_level": None if r["crowd_level"] == "" else int(r["crowd_level"]),
                "avg_lux": _num(r["lux_median"]),
                "lux_status": "측정유효" if r["lux_median"] else "미측정",
            }


def rows_from_db():
    require_env()
    places = {p["id"]: p["name"] for p in sb_select("places", {"select": "id,name"})}
    for m in sb_select("measurements", {"select": "*", "order": "measured_at"}):
        m["place_name"] = places.get(m["place_id"], f"장소 {m['place_id']}")
        yield m


def fmt(x):
    return "  -" if x is None else f"{x:3.0f}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sheet", help="DB 대신 읽을 csv (analysis/측정결과.csv)")
    args = parser.parse_args()

    settings = get_settings() if not args.sheet or Path("batch/.env").exists() or Path(".env").exists() else None
    if settings is None:
        print("[중단] settings 를 읽을 수 없습니다. batch/.env 가 필요합니다.")
        sys.exit(1)

    rows = rows_from_sheet(args.sheet) if args.sheet else rows_from_db()

    print(f"{'번호':>4} {'장소':<16} {'길이':>5}  {'크기':>4} {'종류':>4} {'밝기':>4} {'혼잡':>4} | {'종합':>4}  단계      주 요인          종류 점수는 어디서")
    for m in rows:
        r = score_measurement(m, settings)
        name = m["place_name"][:8]
        if r is None:
            print(f"{m['id']:>4} {name:<16} {m['duration_sec']:>4}s  너무 짧아 계산하지 않음 ({settings['measure_min_sec']}초 미만)")
            continue
        parts = " · ".join(f"{k} {v:.0f}" for k, v in r["score_sound_parts"].items())
        print(f"{m['id']:>4} {name:<16} {m['duration_sec']:>4}s  {fmt(r['score_loudness'])} {fmt(r['score_sound'])} {fmt(r['score_light'])} {fmt(r['score_crowd'])} | "
              f"{fmt(r['total_score'])}  {KOR[r['level']]:<8} {KOR[r['main_factor']]:<14} {parts}")


if __name__ == "__main__":
    main()
