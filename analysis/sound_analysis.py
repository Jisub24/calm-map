"""녹음 파일을 앱과 같은 방식으로 분석한다.

앱(android/.../data/sound/SoundMeter.kt)과 똑같이 계산해야
녹음으로 정한 기준선을 앱 측정값에 그대로 쓸 수 있다.

  - 16kHz 모노로 바꿔서 0.975초(15,600개)씩 자른다. 끝에 남는 조각은 버린다 (앱도 버린다)
  - 소리 크기: RMS → dBFS + 90. 보정 전 상대값이다
  - 모델: 앱이 쓰는 android/app/src/main/assets/yamnet.tflite. 엔진도 앱과 같은 MediaPipe
  - 소리 종류: SoundGroups.kt 의 묶음표를 그대로 읽는다. 종류 점수는 묶인 라벨 중 가장 높은 점수

앱은 아직 평균 소리 크기와 라벨 평균만 낸다. 배경 소음(LA90) · 돌발은 여기서 먼저 정하고
앱이 나중에 같은 식을 따른다.

실행
  python analysis/sound_analysis.py 1        측정기록.csv 의 1번 측정
"""

import csv
import re
import sys
import time
from pathlib import Path

import av
import numpy as np
from mediapipe.tasks import python as mp_tasks
from mediapipe.tasks.python import audio as mp_audio
from mediapipe.tasks.python.components.containers import audio_data

ROOT = Path(__file__).resolve().parent.parent
MODEL = ROOT / "android/app/src/main/assets/yamnet.tflite"
SOUND_GROUPS_KT = ROOT / "android/app/src/main/java/com/calmmap/data/sound/SoundGroups.kt"
RECORDINGS = ROOT / "analysis/recordings"
RECORD_SHEET = ROOT / "analysis/측정기록.csv"

SAMPLE_RATE = 16_000
CLIP_SAMPLES = 15_600
CLIP_SEC = CLIP_SAMPLES / SAMPLE_RATE
DB_OFFSET = 90

# 돌발은 소리 크기가 아니라 '무슨 소리인지'로 센다 (10/19 답사 녹음으로 정함).
# 크기로 세면 측정하는 사람의 말소리와 폰 만지는 소리가 돌발로 잡힌다.
# 폰 만지는 소리가 오히려 가장 커서(배경 +14~18dB) 그릇 소리(+4~15dB)보다 먼저 잡혔다.
#
# 화면에 보여주는 소리 종류(SoundGroups.kt)와는 따로 둔다. 이건 횟수를 세는 규칙이다.
ALERT_LABELS = [  # 알림음. 주문 · 메뉴 알림의 벨 · 차임 · 실로폰 소리
    "Chime", "Wind chime", "Bell", "Jingle bell", "Tubular bells", "Mallet percussion",
    "Marimba, xylophone", "Glockenspiel", "Vibraphone", "Ding", "Ding-dong", "Doorbell", "Steelpan",
]
IMPACT_LABELS = [  # 부딪힘. 그릇 · 식기 · 유리 · 쾅
    "Dishes, pots, and pans", "Cutlery, silverware", "Chink, clink", "Glass", "Clang", "Bang",
    "Slam", "Smash, crash", "Shatter", "Breaking", "Crack", "Clatter",
]
ALARM_LABELS = [  # 경보 · 울음
    "Alarm", "Siren", "Beep, bleep", "Crying, sobbing", "Baby cry, infant cry", "Screaming",
    "Smoke detector, smoke alarm", "Fire alarm", "Buzzer",
]
HANDLING_LABELS = [  # 폰 만짐. 마이크 쪽을 만지면 나는 소리. 세지 않고 따로 알려만 준다
    "Wind noise (microphone)", "Heart sounds, heartbeat", "Thump, thud", "Rustle", "Scratch",
]

ALERT_MIN_SCORE = 0.05   # 알림음은 점수가 낮게 나온다 (확인된 알림음 0.06~0.67)
EVENT_MIN_SCORE = 0.2
MERGE_SEC = 3            # 이 안에 다시 잡히면 같은 소리로 본다


def load_sound_groups():
    """SoundGroups.kt 에서 소리 종류별 라벨 목록을 읽는다.

    파이썬에 묶음표를 따로 적으면 앱 쪽을 고쳤을 때 둘이 어긋난다.
    """
    src = SOUND_GROUPS_KT.read_text(encoding="utf-8")
    groups = {}
    for m in re.finditer(r'\w+\(\s*"([^"]+)",\s*setOf\((.*?)\)\s*,?\s*\)', src, re.S):
        groups[m.group(1)] = re.findall(r'"([^"]+)"', m.group(2))
    if len(groups) != 5:
        raise RuntimeError(f"SoundGroups.kt 에서 소리 종류를 {len(groups)}개 읽었다. 5개여야 한다. 파일 모양이 바뀌었는지 확인")
    return groups


def load_audio(path):
    """m4a 를 16kHz 모노 float(-1~1) 배열로 읽는다."""
    with av.open(str(path)) as container:
        stream = container.streams.audio[0]
        resampler = av.AudioResampler(format="flt", layout="mono", rate=SAMPLE_RATE)
        parts = []
        for frame in container.decode(stream):
            for out in resampler.resample(frame):
                parts.append(out.to_ndarray().reshape(-1))
        for out in resampler.resample(None):
            parts.append(out.to_ndarray().reshape(-1))
    return np.concatenate(parts).astype(np.float32)


def relative_db(clip):
    """앱의 relativeDb 와 같다. 소수점은 버린다."""
    rms = float(np.sqrt(np.mean(clip.astype(np.float64) ** 2)))
    dbfs = 20 * np.log10(max(rms, 1e-6))
    return int(min(max(dbfs + DB_OFFSET, 0), 120))


# 0.1초. 그릇 부딪히는 소리처럼 짧은 소리는 1초 평균에 묻혀서 따로 본다
PEAK_SAMPLES = SAMPLE_RATE // 10


def peak_db(clip):
    """1초 조각 안에서 0.1초 단위로 가장 컸던 순간의 크기. relative_db 와 같은 식이다."""
    n = len(clip) // PEAK_SAMPLES
    windows = clip[:n * PEAK_SAMPLES].astype(np.float64).reshape(n, PEAK_SAMPLES)
    return max(relative_db(w) for w in windows)


def make_classifier():
    options = mp_audio.AudioClassifierOptions(
        base_options=mp_tasks.BaseOptions(model_asset_path=str(MODEL)),
        running_mode=mp_audio.RunningMode.AUDIO_CLIPS,
    )
    return mp_audio.AudioClassifier.create_from_options(options)


def analyze_file(path, classifier, groups):
    """파일 하나를 1초 조각으로 나눠 조각마다 소리 크기 · 라벨 점수 · 종류 점수를 낸다."""
    samples = load_audio(path)
    clips = []
    for i in range(len(samples) // CLIP_SAMPLES):
        clip = samples[i * CLIP_SAMPLES:(i + 1) * CLIP_SAMPLES]
        result = classifier.classify(audio_data.AudioData.create_from_array(clip, SAMPLE_RATE))
        scores = {c.category_name: c.score for c in result[0].classifications[0].categories}
        clips.append({
            "file": path.name,
            "t": i * CLIP_SEC,
            "db": relative_db(clip),
            "peak_db": peak_db(clip),
            "labels": scores,
            "groups": {g: max(scores.get(l, 0.0) for l in labels) for g, labels in groups.items()},
        })
    return clips


def _top(clip, labels):
    label = max(labels, key=lambda l: clip["labels"].get(l, 0.0))
    return label, clip["labels"].get(label, 0.0)


def _is_alert(clips, i):
    """알림음. 앞뒤 5초에 음악이 계속 깔려 있으면 노래 속 벨소리라서 뺀다 (홍대마트 노래)."""
    label, score = _top(clips[i], ALERT_LABELS)
    if score < ALERT_MIN_SCORE:
        return None
    near = [c["labels"]["Music"] for c in clips[max(0, i - 5):i + 6]
            if c["file"] == clips[i]["file"] and c is not clips[i]]
    return (label, score) if np.median(near) < 0.2 else None


def _over(labels):
    def test(clips, i):
        label, score = _top(clips[i], labels)
        return (label, score) if score >= EVENT_MIN_SCORE else None
    return test


EVENT_KINDS = {
    "알림음": _is_alert,
    "부딪힘": _over(IMPACT_LABELS),
    "경보·울음": _over(ALARM_LABELS),
    "폰 만짐": _over(HANDLING_LABELS),
}


def find_events(clips):
    """종류별로 소리가 난 순간을 찾는다. 3초 안에 다시 잡히면 하나로 센다."""
    found = {}
    for kind, test in EVENT_KINDS.items():
        events = []
        for i, clip in enumerate(clips):
            hit = test(clips, i)
            if not hit:
                continue
            last = events[-1]["clip"] if events else None
            if last and last["file"] == clip["file"] and clip["t"] - last["t"] <= MERGE_SEC:
                continue
            events.append({"clip": clip, "label": hit[0], "score": hit[1]})
        found[kind] = events
    return found


def summarize(clips):
    db = np.array([c["db"] for c in clips], dtype=float)
    la90 = float(np.percentile(db, 10))  # 90% 의 시간 동안 넘는 크기 = 아래에서 10%
    events = find_events(clips)
    # 돌발 = 알림음 + 부딪힘 + 경보 · 울음. 폰 만짐은 측정하는 쪽 소리라 뺀다
    sudden = events["알림음"] + events["부딪힘"] + events["경보·울음"]
    rises = [e["clip"]["peak_db"] - la90 for e in sudden]

    label_names = clips[0]["labels"].keys()
    label_mean = {l: float(np.mean([c["labels"][l] for c in clips])) for l in label_names}
    top_count = {}
    for c in clips:
        top = max(c["labels"], key=c["labels"].get)
        top_count[top] = top_count.get(top, 0) + 1

    return {
        "seconds": len(clips) * CLIP_SEC,
        "clips": len(clips),
        "avg_db_app": int(np.mean([c["db"] for c in clips])),   # 앱과 같은 산술 평균
        "leq": float(10 * np.log10(np.mean(10 ** (db / 10)))),   # 에너지 평균
        "la90": la90,
        "la50": float(np.percentile(db, 50)),
        "la10": float(np.percentile(db, 90)),
        "max_db": float(db.max()),
        "events": events,
        "sudden_count": len(sudden),
        "alert_count": len(events["알림음"]),
        # 돌발 중 가장 크게 튄 정도. 0.1초 단위 최대값으로 본다
        "sudden_max_rise": max(rises) if rises else None,
        "group_mean": {g: float(np.mean([c["groups"][g] for c in clips])) for g in clips[0]["groups"]},
        "label_mean": sorted(label_mean.items(), key=lambda x: -x[1]),
        "top_count": sorted(top_count.items(), key=lambda x: -x[1]),
    }


def read_measurement(no):
    with open(RECORD_SHEET, encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            if int(row["measurement_no"]) == no:
                return row
    raise SystemExit(f"측정기록.csv 에 {no}번 측정이 없다")


def mmss(sec):
    # 조각이 0.975초라 소수점을 버리면 같은 초가 두 번 찍힌다. 반올림한다
    sec = int(round(sec))
    return f"{sec // 60}:{sec % 60:02d}"


def print_report(row, clips, s):
    print(f"\n■ {row['measurement_no']}번 · {row['place_name']} · {row['started_at'][11:16]} 시작 · {row['slot_hour']}시 칸")
    print(f"  분석한 길이 {mmss(s['seconds'])} (1초 조각 {s['clips']}개)")

    print("\n[소리 크기 dB · 보정 전 상대값]")
    print(f"  평균 (앱과 같은 산술 평균)  {s['avg_db_app']}")
    print(f"  에너지 평균 (Leq)           {s['leq']:.1f}")
    print(f"  배경 소음 (LA90)            {s['la90']:.1f}")
    print(f"  중간 (LA50)                 {s['la50']:.1f}")
    print(f"  시끄러운 순간 (LA10)        {s['la10']:.1f}")
    print(f"  최대                        {s['max_db']:.0f}")

    rise = "없음" if s["sudden_max_rise"] is None else f"+{s['sudden_max_rise']:.0f}dB"
    print(f"\n[돌발] {s['sudden_count']}번 (그중 알림음 {s['alert_count']}번) · 가장 크게 튄 정도 {rise}")
    for kind, events in s["events"].items():
        note = "  ← 세지 않음" if kind == "폰 만짐" else ""
        print(f"  {kind} {len(events)}번{note}")
        for e in events:
            c = e["clip"]
            print(f"     {c['file']} {mmss(c['t'])}  {e['label']} {e['score']:.2f}  "
                  f"순간 최대 {c['peak_db']}dB (배경 +{c['peak_db'] - s['la90']:.0f})")

    print("\n[소리 종류 평균 점수 · 앱 측정 화면과 같은 방식]")
    for g, v in sorted(s["group_mean"].items(), key=lambda x: -x[1]):
        print(f"  {g:10} {v:.2f}")

    print("\n[많이 나온 라벨 · 평균 점수 상위 12]")
    for l, v in s["label_mean"][:12]:
        print(f"  {v:.3f}  {l}")

    print("\n[1초마다 1위였던 라벨 · 횟수 상위 8]")
    for l, n in s["top_count"][:8]:
        print(f"  {n:4}번  {l}")

    # 안내방송 단서: YAMNet 에 안내방송 라벨이 없어서, 차임 · 울림 · 큰 공간 라벨을 따로 본다
    cue_labels = ["Chime", "Ding-dong", "Ding", "Doorbell", "Speech synthesizer",
                  "Reverberation", "Echo", "Inside, large room or hall", "Inside, public space",
                  "Narration, monologue", "Television", "Radio"]
    print("\n[안내방송 단서 라벨 · 평균 점수와 가장 높았던 순간]")
    for l in cue_labels:
        best = max(clips, key=lambda c: c["labels"][l])
        mean = np.mean([c["labels"][l] for c in clips])
        print(f"  {l:28} 평균 {mean:.3f}  최고 {best['labels'][l]:.2f} ({best['file']} {mmss(best['t'])})")

    print("\n[30초마다 · 평균 dB 와 가장 높은 라벨]")
    for f in dict.fromkeys(c["file"] for c in clips):
        part = [c for c in clips if c["file"] == f]
        for i in range(0, len(part), 31):
            chunk = part[i:i + 31]
            mean_labels = {l: np.mean([c["labels"][l] for c in chunk]) for l in chunk[0]["labels"]}
            top3 = sorted(mean_labels.items(), key=lambda x: -x[1])[:3]
            labels = " · ".join(f"{l} {v:.2f}" for l, v in top3)
            print(f"  {f} {mmss(chunk[0]['t']):>5}  {np.mean([c['db'] for c in chunk]):5.1f}dB  {labels}")


def main():
    no = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    row = read_measurement(no)
    groups = load_sound_groups()
    classifier = make_classifier()

    started = time.time()
    clips = []
    for name in row["recordings"].split(";"):
        clips += analyze_file(RECORDINGS / name, classifier, groups)
    classifier.close()

    print_report(row, clips, summarize(clips))
    print(f"\n(분석에 걸린 시간 {time.time() - started:.0f}초)")


if __name__ == "__main__":
    main()
