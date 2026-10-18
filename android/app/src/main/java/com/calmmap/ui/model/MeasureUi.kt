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

/** 측정이 끝난 뒤 결과 화면에 보여주고 저장할 값 */
data class MeasureResult(
    val place: PlaceUi,
    /** 이번 측정 전까지 이 장소의 측정 횟수 */
    val countBefore: Int,
    /** 측정하는 동안의 평균 소리 크기. 보정 전 상대값 */
    val db: Int,
    /** 점수 높은 순 */
    val sounds: List<SoundScore>,
    val flicker: FlickerAnswer?,
    val endedAt: LocalDateTime,
    val seconds: Int,
) {
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

/**
 * 측정 중 화면이 시작할 때의 소리 점수.
 * TODO: YAMNet 을 붙이면 지운다. 지금은 이 값 근처에서 조금씩 흔들리게 보여준다
 */
val sampleSoundBase = listOf(
    SoundScore("안내방송", 0.62f),
    SoundScore("말소리", 0.45f),
    SoundScore("음악", 0.22f),
    SoundScore("아기 울음", 0.05f),
)
