"""같은 자리에서 짝지어 잰 두 기종의 조도 값으로 보정값을 구한다.

입력 csv (analysis/조도기종비교.csv)
    light,galaxy_lux,iphone_lux
    조명 끔,12,25
    스탠드만,85,170
    ...

보는 것
    1. 쌍마다 비율 아이폰 ÷ 갤럭시
    2. 비율이 밝기와 무관하게 일정한가 (일정하면 곱셈 보정값 하나)
    3. 로그-로그 직선 맞춤: iphone = a × galaxy^b. b 가 1 에 가까우면 비율 하나로 충분

실행
    python analysis/lux_compare.py analysis/조도기종비교.csv
"""

import csv
import sys

import numpy as np


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "analysis/조도기종비교.csv"
    rows = [r for r in csv.DictReader(open(path, encoding="utf-8-sig")) if r.get("galaxy_lux") and r.get("iphone_lux")]
    g = np.array([float(r["galaxy_lux"]) for r in rows])
    i = np.array([float(r["iphone_lux"]) for r in rows])
    ratio = i / g
    print(f"{'조명':12} {'갤럭시':>8} {'아이폰':>8} {'아이폰÷갤럭시':>10}")
    for r, a, b, q in zip(rows, g, i, ratio):
        print(f"{r['light']:12} {a:8.0f} {b:8.0f} {q:10.2f}")
    print(f"\n비율 중앙값 {np.median(ratio):.2f} · 평균 {ratio.mean():.2f} · 최소 {ratio.min():.2f} · 최대 {ratio.max():.2f}")

    if len(rows) >= 3:
        b, loga = np.polyfit(np.log(g), np.log(i), 1)
        a = np.exp(loga)
        pred = a * g ** b
        err = np.abs(pred - i) / i * 100
        print(f"로그 직선 맞춤: 아이폰 ≈ {a:.2f} × 갤럭시^{b:.2f}  (쌍마다 오차 평균 {err.mean():.0f}%)")
        if 0.9 <= b <= 1.1:
            print(f"→ 지수가 1에 가까워 곱셈 보정값 하나면 된다: lux × {np.median(ratio):.2f}")
        else:
            print("→ 밝기에 따라 비율이 달라진다. settings 에 a · b 두 값(거듭제곱)으로 넣는다")
    else:
        print("쌍이 3개 미만이라 곡선 모양은 못 본다. 비율 중앙값만 쓴다")


if __name__ == "__main__":
    main()
