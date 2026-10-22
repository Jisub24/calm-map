package com.calmmap.ui.model

import com.calmmap.ui.theme.SensoryLevel
import java.time.LocalDateTime

/** 한 번 측정하는 시간 */
const val MEASURE_SECONDS = 60

/** 소리 종류 하나와 그 점수(0~1). YAMNet 이 내는 값 */
data class SoundScore(val label: String, val score: Float)

/** '조명이 깜빡이나요?' 수동 입력 */
enum class FlickerAnswer(val label: String) {
    YES("깜빡여요"),
    NO("아니요"),
    UNKNOWN("모르겠어요"),
}

/**
 * '방송 나왔어요'를 한 번도 안 눌렀을 때 결과 화면에서 고르는 답.
 *
 * 안 눌렀다고 0번으로 넣으면 '방송이 없는 곳'이 된다. 신경 쓰지 못한 측정과 구분해야 한다.
 */
enum class AnnounceAnswer(val label: String) {
    YES("있었어요"),
    NO("없었어요"),
    UNKNOWN("잘 모르겠어요"),
}

/** 측정이 끝난 뒤 결과 화면에 보여주고 저장할 값 */
data class MeasureResult(
    val place: PlaceUi,
    /** 이번 측정 전까지 이 장소의 측정 횟수 */
    val countBefore: Int,
    /** 측정하는 동안의 평균 소리 크기. 보정 전 상대값 */
    val db: Int,
    /** 소리 종류 5개의 평균 점수. 점수 높은 순. 화면용 */
    val sounds: List<SoundScore>,
    /** YAMNet 원본 라벨의 평균 점수. 저장할 때는 이것을 보낸다 */
    val rawLabels: Map<String, Float>,
    val flicker: FlickerAnswer?,
    val endedAt: LocalDateTime,
    val seconds: Int,
    /** 측정 중에 '방송 나왔어요'를 누른 횟수. YAMNet 은 안내 목소리와 대화를 구분하지 못한다 */
    val announceTaps: Int = 0,
    /** 한 번도 안 눌렀을 때만 결과 화면에서 받는 답 */
    val announceAnswer: AnnounceAnswer? = null,
) {
    /**
     * 저장할 안내방송 횟수. null 이면 미측정.
     * '있었어요'는 몇 번인지 몰라서 최소인 1번으로 센다
     */
    val announceCount: Int?
        get() = when {
            announceTaps > 0 -> announceTaps
            announceAnswer == AnnounceAnswer.YES -> 1
            announceAnswer == AnnounceAnswer.NO -> 0
            else -> null
        }

    // TODO: 실제 점수 계산은 batch 쪽 규칙을 따른다. 지금은 화면 확인용으로 소리 크기만 본다
    val level: SensoryLevel
        get() = when {
            db < 60 -> SensoryLevel.CALM
            db < 70 -> SensoryLevel.MID
            else -> SensoryLevel.HIGH
        }
}

// TODO: 측정 횟수는 measurements 테이블에서 센다. 지금은 장소 id 로 정한 예시 값
fun sampleMeasureCount(placeId: Long): Int = when (placeId % 4) {
    0L -> 0
    1L -> 12
    2L -> 2
    else -> 5
}

/** 측정 횟수가 이보다 적으면 '데이터 부족' */
const val ENOUGH_MEASURES = 3

/** 미리보기용 소리 점수 */
val sampleSoundBase = listOf(
    SoundScore("안내방송", 0.62f),
    SoundScore("말소리", 0.45f),
    SoundScore("음악", 0.22f),
    SoundScore("기계음", 0.10f),
    SoundScore("갑작스러운 소리", 0.05f),
)
