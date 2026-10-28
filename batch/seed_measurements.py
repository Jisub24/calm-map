"""답사 녹음 분석 결과를 measurements · measurement_labels 에 넣는다 (한 번만).

앱이 측정을 저장하기 전에 10/18 홍대 답사 7건을 DB 에 넣어 점수 · 예측이 돌게 한다.
읽는 파일
    analysis/측정기록.csv   장소 · 시작 시각 · 길이 · 기종 · 조도
    analysis/측정결과.csv   Leq · 배경 · 돌발 · 알림음 · 설문
    analysis/측정라벨.csv   YAMNet 라벨 비율 · 평균 확신도 (소리 탭용)

같은 장소 · 같은 시작 시각 · 같은 사용자 줄이 이미 있으면 건너뛴다. 두 번 돌려도 안 쌓인다.

실행
    python batch/seed_measurements.py --user <측정자 auth.users id>
    python batch/seed_measurements.py --user ... --dry-run
"""

import argparse
import csv
from pathlib import Path

import requests

from calculate_score import rows_from_sheet
from common import SUPABASE_URL, _headers, require_env, sb_select

ROOT = Path(__file__).resolve().parent.parent
RECORD = ROOT / "analysis/측정기록.csv"
RESULT = ROOT / "analysis/측정결과.csv"
LABELS = ROOT / "analysis/측정라벨.csv"


def sb_insert(table, rows):
    """줄을 넣고 넣어진 줄(id 포함)을 돌려받는다."""
    r = requests.post(f"{SUPABASE_URL}/rest/v1/{table}", headers=_headers({"Prefer": "return=representation"}),
                      json=rows, timeout=60)
    if r.status_code >= 300:
        raise RuntimeError(f"{table} 저장 실패 {r.status_code}: {r.text[:300]}")
    return r.json()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--user", required=True, help="측정한 사람의 auth.users id")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    require_env()

    with open(RECORD, encoding="utf-8-sig") as f:
        record = {int(r["measurement_no"]): r for r in csv.DictReader(f)}
    labels = {}
    with open(LABELS, encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            labels.setdefault(int(r["measurement_no"]), []).append(r)

    existing = {(m["place_id"], m["measured_at"][:19]) for m in
                sb_select("measurements", {"select": "place_id,measured_at", "user_id": f"eq.{args.user}"})}

    for m in rows_from_sheet(RESULT):
        rec = record[m["id"]]
        measured_at = rec["started_at"]
        key = (m["place_id"], measured_at.replace("+09:00", "")[:19])
        row = {
            "place_id": m["place_id"],
            "user_id": args.user,
            "device_model": m["device_model"],
            "measured_at": measured_at,
            "duration_sec": m["duration_sec"],
            "avg_db": m["avg_db"],                     # Leq
            "background_db": m["background_db"],
            "avg_lux": m["avg_lux"],
            "lux_status": m["lux_status"],
            "flicker_status": "미측정",
            "sudden_count": m["sudden_count"],
            "sudden_strong_count": m["sudden_strong_count"],
            "sudden_max_rise": m["sudden_max_rise"],
            "alert_count": m["alert_count"],
            "announce_count": m["announce_count"],
            "announce_level": m["announce_level"],
            "crowd_level": m["crowd_level"],
        }
        tag = f"{m['id']}번 {m['place_name'][:8]} {measured_at[11:16]}"
        if args.dry_run:
            print(tag, row); continue
        # 이미 있으면 건너뛴다 (시각은 서버가 UTC 로 돌려줘서 자리수만 맞춰 비교)
        saved = [x for x in sb_select("measurements", {"select": "id", "user_id": f"eq.{args.user}",
                                                        "place_id": f"eq.{m['place_id']}",
                                                        "measured_at": f"eq.{measured_at}"})]
        if saved:
            print(tag, "이미 있음 → 건너뜀"); continue
        inserted = sb_insert("measurements", [row])[0]
        label_rows = [{"measurement_id": inserted["id"], "yamnet_label": l["yamnet_label"],
                       "ratio": float(l["ratio"]), "avg_confidence": float(l["avg_confidence"])}
                      for l in labels.get(m["id"], [])]
        if label_rows:
            sb_insert("measurement_labels", label_rows)
        print(tag, f"→ measurements id {inserted['id']}, 라벨 {len(label_rows)}줄")


if __name__ == "__main__":
    main()
