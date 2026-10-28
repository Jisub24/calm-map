package com.calmmap.data.sound

/**
 * YAMNet 라벨 521개를 소리 종류 7개로 묶는 표.
 *
 * DB 의 sound_groups(seed/sound_groups.sql)와 같은 종류 · 같은 라벨이어야 한다.
 * 앱은 측정 중에 알림음 · 돌발을 실시간으로 세야 해서 표를 안에 들고 있고,
 * batch 는 저장된 라벨을 이 표로 묶어 비율을 낸다. 둘이 다르면 횟수와 비율이 어긋난다.
 *
 * 저장할 때는 묶은 값이 아니라 YAMNet 원본 라벨을 보낸다. 묶은 값은 화면용이다.
 * YAMNet 에는 '안내방송' 라벨이 없다. 안내 목소리는 말소리로 잡혀서 사람이 직접 표시한다.
 */
enum class SoundGroup(val label: String, val dbType: String, val yamnetLabels: Set<String>) {
    ALERT(
        "알림음", "alert",
        setOf(
            "Chime", "Wind chime", "Bell", "Jingle bell", "Tubular bells", "Mallet percussion",
            "Marimba, xylophone", "Glockenspiel", "Vibraphone", "Ding", "Ding-dong", "Doorbell", "Steelpan",
        ),
    ),
    IMPACT(
        "부딪힘", "impact",
        setOf(
            "Dishes, pots, and pans", "Cutlery, silverware", "Chink, clink", "Glass", "Clang", "Bang",
            "Slam", "Smash, crash", "Shatter", "Breaking", "Crack", "Clatter",
        ),
    ),
    ALARM(
        "경보", "alarm",
        setOf("Alarm", "Siren", "Civil defense siren", "Beep, bleep", "Buzzer", "Smoke detector, smoke alarm", "Fire alarm"),
    ),
    LOUD_VOICE(
        "사람의 큰 소리", "loud_voice",
        setOf("Screaming", "Yell", "Shout", "Children shouting", "Crying, sobbing", "Baby cry, infant cry", "Whimper"),
    ),
    SPEECH(
        "말소리", "speech",
        setOf(
            "Speech", "Child speech, kid speaking", "Conversation", "Narration, monologue",
            "Hubbub, speech noise, speech babble", "Crowd", "Chatter", "Laughter",
        ),
    ),
    MUSIC(
        "음악", "music",
        setOf("Music", "Pop music", "Background music", "Singing", "Electronic music", "Jingle (music)", "Musical instrument"),
    ),
    MACHINE(
        "기계음", "machine",
        setOf(
            "Mechanical fan", "Air conditioning", "Engine", "Vacuum cleaner", "Printer", "Cash register",
            "Mechanisms", "Hum", "Buzz", "Whir", "Drill", "Hammer", "Jackhammer", "Power tool", "Sawing",
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

/**
 * 측정 중에 돌발 소리를 센다. analysis/sound_analysis.py 의 find_events 와 같은 규칙이다.
 * 답사 7건 점수가 이 규칙으로 나왔으니 앱도 똑같이 세야 같은 점수가 된다.
 *
 * 돌발은 소리 크기가 아니라 '무슨 소리인지'로 센다. 크기로 세면 측정하는 사람의
 * 말소리와 폰 만지는 소리가 돌발로 잡힌다 (10/19 답사 녹음으로 확인).
 *
 *   알림음      벨 · 차임 · 실로폰. 점수가 낮게 나와서 0.05 부터. 앞 10초에 음악이 계속 깔려
 *               있으면 노래 속 벨소리라서 안 센다 (홍대마트 노래)
 *   부딪힘      그릇 · 식기 · 유리 · 쾅. 0.2 부터
 *   경보 · 울음  경보음과 울음 · 비명. 0.2 부터
 *
 * 같은 종류가 3초 안에 다시 잡히면 같은 소리로 본다. 폰 만지는 소리는 세지 않는다.
 * 1초 조각이 들어올 때마다 add() 를 부르고, 끝나면 배경 소음(LA90)을 넣어 결과를 뽑는다.
 */
class SoundEventCounter {
    enum class Kind { ALERT, IMPACT, ALARM }

    /** 돌발 한 번. [peakDb] 는 그 1초 안에서 0.1초 단위로 가장 컸던 크기 */
    data class Event(val kind: Kind, val second: Int, val peakDb: Int)

    val events = mutableListOf<Event>()
    private val lastAt = mutableMapOf<Kind, Int>()
    private val recentMusic = ArrayDeque<Float>()

    /** [second] 는 측정 시작부터 몇 번째 1초 조각인지 */
    fun add(second: Int, reading: SoundReading) {
        val scores = reading.labelScores
        // 분석 스크립트는 앞뒤 5초의 음악 중앙값을 보는데, 실시간이라 뒤는 모른다. 앞 10초로 본다
        val musicNow = scores["Music"] ?: 0f
        val musicAround = if (recentMusic.isEmpty()) 0f else recentMusic.sorted()[recentMusic.size / 2]
        recentMusic.addLast(musicNow)
        if (recentMusic.size > MUSIC_WINDOW) recentMusic.removeFirst()

        val hits = buildList {
            if (top(scores, SoundGroup.ALERT.yamnetLabels) >= ALERT_MIN_SCORE && musicAround < MUSIC_MAX) add(Kind.ALERT)
            if (top(scores, SoundGroup.IMPACT.yamnetLabels) >= EVENT_MIN_SCORE) add(Kind.IMPACT)
            if (top(scores, ALARM_EVENT_LABELS) >= EVENT_MIN_SCORE) add(Kind.ALARM)
        }
        hits.forEach { kind ->
            val last = lastAt[kind]
            if (last != null && second - last <= MERGE_SEC) return@forEach
            lastAt[kind] = second
            events += Event(kind, second, reading.peakDb)
        }
    }

    val alertCount: Int get() = events.count { it.kind == Kind.ALERT }

    /** 돌발 = 알림음 + 부딪힘 + 경보 · 울음 */
    val suddenCount: Int get() = events.size

    /** 배경(LA90)보다 [riseDb] 이상 튄 돌발 수 (settings.sudden_rise_db = 10) */
    fun strongCount(backgroundDb: Double, riseDb: Double): Int =
        events.count { it.peakDb - backgroundDb >= riseDb }

    /** 돌발 중 배경 대비 가장 크게 튄 정도. 돌발이 없으면 null */
    fun maxRise(backgroundDb: Double): Double? =
        events.maxOfOrNull { it.peakDb - backgroundDb }

    private fun top(scores: Map<String, Float>, labels: Set<String>): Float =
        labels.maxOf { scores[it] ?: 0f }

    companion object {
        const val ALERT_MIN_SCORE = 0.05f
        const val EVENT_MIN_SCORE = 0.2f
        const val MERGE_SEC = 3
        private const val MUSIC_WINDOW = 10
        private const val MUSIC_MAX = 0.2f

        /** 경보 · 울음. 분석 스크립트의 ALARM_LABELS. 울음 · 비명은 화면 묶음에선 '사람의 큰 소리'지만 돌발로 센다 */
        val ALARM_EVENT_LABELS: Set<String> = SoundGroup.ALARM.yamnetLabels +
            setOf("Crying, sobbing", "Baby cry, infant cry", "Screaming")
    }
}
