package com.calmmap.data.sound

/**
 * YAMNet 라벨 521개를 화면에 보여줄 소리 종류 5개로 묶는 임시 표.
 *
 * DB 의 sound_groups 와 같은 종류를 쓴다 (announcement · speech · music · machine · sudden).
 * 저장할 때는 묶은 값이 아니라 YAMNet 원본 라벨을 보낸다. 이 표는 화면용이다.
 *
 * TODO: analysis/01 녹음 실험 결과로 고친다.
 * 특히 YAMNet 에는 '안내방송' 라벨이 없어서, 안내방송 앞뒤의 차임 · 울림 소리로 짐작하고 있다.
 */
enum class SoundGroup(val label: String, val yamnetLabels: Set<String>) {
    ANNOUNCEMENT(
        "안내방송",
        setOf("Speech synthesizer", "Chime", "Ding-dong", "Echo", "Reverberation", "Television", "Radio"),
    ),
    SPEECH(
        "말소리",
        setOf(
            "Speech", "Child speech, kid speaking", "Conversation", "Narration, monologue",
            "Hubbub, speech noise, speech babble", "Crowd", "Chatter", "Shout", "Children shouting", "Laughter",
        ),
    ),
    MUSIC(
        "음악",
        setOf("Music", "Pop music", "Background music", "Singing", "Electronic music", "Jingle (music)", "Musical instrument"),
    ),
    MACHINE(
        "기계음",
        setOf(
            "Mechanical fan", "Air conditioning", "Engine", "Vacuum cleaner", "Printer",
            "Cash register", "Mechanisms", "Hum", "Buzz", "Whir",
        ),
    ),
    SUDDEN(
        "갑작스러운 소리",
        setOf(
            "Alarm", "Siren", "Beep, bleep", "Bang", "Slam", "Crying, sobbing", "Baby cry, infant cry",
            "Screaming", "Smoke detector, smoke alarm", "Fire alarm", "Buzzer", "Clang",
        ),
    ),
    ;

    companion object {
        /**
         * 라벨별 점수를 종류별 점수로. 종류 안에서 가장 높은 라벨 점수를 쓴다.
         * 더하면 비슷한 라벨이 많은 종류(말소리)가 부풀려진다.
         */
        fun scoresOf(labelScores: Map<String, Float>): Map<SoundGroup, Float> =
            entries.associateWith { group ->
                group.yamnetLabels.maxOf { labelScores[it] ?: 0f }
            }
    }
}
