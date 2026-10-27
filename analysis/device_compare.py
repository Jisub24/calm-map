"""같은 자리에서 동시에 녹음한 두 기종의 소리 크기 · 라벨 차이를 잰다 (기종 보정값).

앱의 dB 는 마이크 신호 세기(dBFS) + 90 인 상대값이라 폰마다 다르다.
기준값(몇 dB 가 100점인지)은 아이폰 15 녹음으로 정했으므로, 안드로이드 앱에
그대로 쓰려면 "갤럭시가 아이폰보다 몇 dB 다르게 적나" 를 알아야 한다.

방법
    1. 두 파일을 앱과 같은 16kHz 모노로 읽는다
    2. 0.1초 단위 소리 크기 곡선을 서로 맞춰 시작 시각 차이(lag)를 찾는다
    3. 맞춘 뒤 1초마다 dB 차이 (갤럭시 − 아이폰) 를 내고,
       차이가 일정한지(조용할 때 · 시끄러울 때 같은지) 본다
    4. YAMNet 라벨이 두 폰에서 같게 잡히는지 본다

실행
    python analysis/device_compare.py "테스트(아이폰).m4a" "테스트(갤럭시).m4a"
"""

import sys
import time
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sound_analysis import (CLIP_SAMPLES, DB_OFFSET, RECORDINGS, SAMPLE_RATE, load_audio,
                            load_sound_groups, make_classifier, analyze_file)

HOP = SAMPLE_RATE // 10          # 0.1초


def envelope_db(x):
    n = len(x) // HOP
    frames = x[:n * HOP].reshape(n, HOP).astype(np.float64)
    rms = np.sqrt(np.mean(frames ** 2, axis=1))
    return 20 * np.log10(np.maximum(rms, 1e-6)) + DB_OFFSET


def find_lag(a, b, max_lag_sec=60):
    """b 가 a 보다 몇 초 늦게 시작했나 (양수면 b 가 늦음). 0.1초 단위 상관."""
    max_lag = max_lag_sec * 10
    a0 = a - a.mean(); b0 = b - b.mean()
    best, best_corr = 0, -np.inf
    for lag in range(-max_lag, max_lag + 1):
        if lag >= 0:
            x, y = a0[lag:], b0[:len(a0) - lag]
        else:
            x, y = a0[:lag], b0[-lag:]
        n = min(len(x), len(y))
        if n < 600:
            continue
        c = np.dot(x[:n], y[:n]) / n
        if c > best_corr:
            best_corr, best = c, lag
    return best / 10.0


def per_second(env):
    n = len(env) // 10
    e = env[:n * 10].reshape(n, 10)
    return 10 * np.log10(np.mean(10 ** (e / 10), axis=1))   # 1초 Leq


def main():
    ref_name, other_name = sys.argv[1], sys.argv[2]
    ref = load_audio(RECORDINGS / ref_name)
    oth = load_audio(RECORDINGS / other_name)
    env_r, env_o = envelope_db(ref), envelope_db(oth)
    lag = find_lag(env_r, env_o)
    print(f"기준 {ref_name} {len(ref)/SAMPLE_RATE:.0f}s · 비교 {other_name} {len(oth)/SAMPLE_RATE:.0f}s")
    print(f"시작 시각 차이: 비교 파일이 기준보다 {lag:+.1f}초 {'늦게' if lag < 0 else '일찍'} 시작")

    # 맞추기: 기준 시각 t 에 해당하는 비교 샘플 = t + lag
    shift = int(round(-lag * SAMPLE_RATE))
    if shift >= 0:
        r, o = ref[shift:], oth
    else:
        r, o = ref, oth[-shift:]
    n = min(len(r), len(o)); r, o = r[:n], o[:n]
    sr, so = per_second(envelope_db(r)), per_second(envelope_db(o))
    diff = so - sr
    print(f"\n겹치는 길이 {n/SAMPLE_RATE/60:.1f}분 · 1초 표본 {len(diff)}개")
    print(f"[소리 크기 차이 = 비교 − 기준]  평균 {diff.mean():+.2f} dB · 중앙값 {np.median(diff):+.2f} · 표준편차 {diff.std():.2f}")
    print(f"  기준 Leq {10*np.log10(np.mean(10**(sr/10))):.1f} / 비교 Leq {10*np.log10(np.mean(10**(so/10))):.1f}")
    print(f"  기준 LA90 {np.percentile(sr,10):.1f} / 비교 LA90 {np.percentile(so,10):.1f}")
    print("\n[기준 소리 크기 구간별 차이 — 일정해야 보정값 하나로 쓸 수 있다]")
    edges = np.percentile(sr, [0, 20, 40, 60, 80, 100])
    for lo, hi in zip(edges, edges[1:]):
        m = (sr >= lo) & (sr <= hi)
        print(f"  기준 {lo:5.1f}~{hi:5.1f} dB : 차이 {diff[m].mean():+.2f} (표준편차 {diff[m].std():.2f}, {m.sum()}초)")
    corr = np.corrcoef(sr, so)[0, 1]
    print(f"\n1초 dB 상관계수 {corr:.3f} (1에 가까울수록 같은 소리를 들음)")

    # 라벨 비교
    print("\n[YAMNet 라벨 — 1초마다 1위 라벨이 같은 비율]")
    groups = load_sound_groups(); clf = make_classifier()
    t0 = time.time()
    cr = analyze_file(RECORDINGS / ref_name, clf, groups)
    co = analyze_file(RECORDINGS / other_name, clf, groups)
    clf.close()
    off = int(round(-lag / (CLIP_SAMPLES / SAMPLE_RATE)))   # 조각 단위 이동
    if off >= 0:
        cr, co = cr[off:], co
    else:
        cr, co = cr, co[-off:]
    m = min(len(cr), len(co)); cr, co = cr[:m], co[:m]
    same = np.mean([max(a["labels"], key=a["labels"].get) == max(b["labels"], key=b["labels"].get) for a, b in zip(cr, co)])
    print(f"  1위 라벨 일치 {same*100:.0f}% ({m}초, 분석 {time.time()-t0:.0f}초)")
    for lab in ["Speech", "Music", "Television", "Dishes, pots, and pans", "Cutlery, silverware", "Chime", "Inside, small room"]:
        a = np.mean([c["labels"][lab] for c in cr]); b = np.mean([c["labels"][lab] for c in co])
        print(f"  {lab:26} 기준 평균 {a:.3f} / 비교 {b:.3f}")


if __name__ == "__main__":
    main()
