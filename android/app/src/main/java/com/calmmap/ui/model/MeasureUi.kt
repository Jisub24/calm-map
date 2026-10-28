package com.calmmap.ui.model

import java.time.LocalDateTime

/** 한 번 측정하는 시간. 최종안 3분 */
const val MEASURE_SECONDS = 180

/** 이보다 짧은 측정은 저장하지 않는다. DB 가 60초 미만을 받지 않고(013), 배경 소음을 내려면 1분은 있어야 한다 */
const val MIN_SAVE_SECONDS = 60

/** 소리 종류 하나와 그 점수(0~1). YAMNet 이 내는 값 */
data class SoundScore(val label: String, val score: Float)

/**
 * 측정이 끝난 뒤 안내방송이 몇 번쯤 나왔는지 고르는 답. measurements.announce_level
 * YAMNet 은 안내 목소리와 대화를 구분하지 못해서 사람이 답한다.
 *
 * 답을 안 했다고 0번으로 넣으면 '방송이 없는 곳'이 된다. 신경 쓰지 못한 측정(잘 모르겠어요)은 비워서 구분한다.
 */
enum class AnnounceLevel(val label: String, val dbValue: Int?) {
    NONE("없었어요", 0),
    FEW("1~2번", 1),
    SOME("3~5번", 2),
    MANY("6번 이상", 3),
    UNKNOWN("잘 모르겠어요", null),
}

/** 측정하는 동안 사람이 얼마나 있었나. measurements.crowd_level (0~3, 023). 모르면 비운다 */
enum class CrowdAnswer(val label: String, val dbValue: Int?) {
    CALM("한가함", 0),
    NORMAL("보통", 1),
    BUSY("붐빔", 2),
    VERY_BUSY("매우 붐빔", 3),
    UNKNOWN("잘 모르겠어요", null),
}

/** 라벨 하나가 측정 중 얼마나 자주(ratio), 얼마나 확실하게(avgConfidence) 들렸나. measurement_labels 한 줄 */
data class LabelStat(val ratio: Float, val avgConfidence: Float)

/** 측정이 끝난 뒤 결과 화면에 보여주고 저장할 값 */
data class MeasureResult(
    val place: PlaceUi,
    /** 이번 측정 전까지 이 장소의 측정 횟수 */
    val countBefore: Int,
    /** 측정하는 동안의 소리 크기(Leq, 에너지 평균). 보정 전 상대값. 화면용 정수 */
    val db: Int,
    /** 소리 종류 7개의 평균 점수. 점수 높은 순. 화면용 */
    val sounds: List<SoundScore>,
    /** YAMNet 원본 라벨의 평균 점수. 화면용 */
    val rawLabels: Map<String, Float>,
    val endedAt: LocalDateTime,
    val seconds: Int,
    /** 결과 화면에서 받는 안내방송 답 */
    val announceLevel: AnnounceLevel? = null,
    /** 결과 화면에서 받는 사람 혼잡 답 */
    val crowd: CrowdAnswer? = null,

    // ---- 아래는 저장용. 화면에는 안 나온다 ----
    /** 측정 시작 시각. "토요일 14시대"로 묶이는 기준 */
    val startedAt: LocalDateTime = endedAt.minusSeconds(seconds.toLong()),
    /** 소리 크기 Leq. db 는 이것을 반올림한 값 */
    val leq: Double = db.toDouble(),
    /** 1초 조각 중 가장 컸던 값 */
    val maxDb: Int = db,
    /** 배경 소음 LA90. 1초 값을 작은 순으로 늘어놨을 때 아래에서 10% 자리 */
    val backgroundDb: Double = db.toDouble(),
    val alertCount: Int = 0,
    val suddenCount: Int = 0,
    val suddenStrongCount: Int = 0,
    val suddenMaxRise: Double? = null,
    /** 조도 중앙값(lux). 센서가 없거나 값을 못 받았으면 null */
    val lux: Double? = null,
    /** 미측정 / 측정유효 / 측정실패 (measurements.lux_status) */
    val luxStatus: String = "미측정",
    /** 라벨별 통계. 저장할 때 measurement_labels 로 보낸다 */
    val labelStats: Map<String, LabelStat> = emptyMap(),
) {
    /** 1분이 안 되는 측정은 저장할 수 없다 */
    val canSave: Boolean
        get() = seconds >= MIN_SAVE_SECONDS

    // 감각부하 점수 · 단계는 앱이 내지 않는다. 저장 뒤 batch(calculate_score.py)가 같은 규칙으로 계산한다
}

/** 측정 횟수가 이보다 적으면 '데이터 부족' */
const val ENOUGH_MEASURES = 3

/** 미리보기용 소리 점수 */
val sampleSoundBase = listOf(
    SoundScore("말소리", 0.45f),
    SoundScore("음악", 0.22f),
    SoundScore("기계음", 0.10f),
    SoundScore("알림음", 0.05f),
    SoundScore("부딪힘", 0.03f),
)
