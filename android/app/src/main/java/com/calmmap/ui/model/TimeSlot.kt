package com.calmmap.ui.model

/**
 * 지도에서 고른 때. 평일 · 주말과 시각 하나.
 *
 * 점수는 장소 × 평일/주말 × 1시간 칸으로 모은다 (답변서1 질의2). 그래서 날짜가 아니라
 * 평일 · 주말만 고르게 한다. 값은 지금까지 쌓인 그 묶음 기록으로 낸 slot_forecast 에서 읽는다.
 */
/**
 * 시간 선택 중에 측정 버튼 대신 보여주는 말.
 * 측정은 잰 시각(지금)으로 저장돼서, '주말 15시' 를 보다가 재면 그 칸에 들어간다고 헷갈린다
 */
const val MEASURE_NOW_ONLY = "측정은 '지금'에서만 할 수 있어요"

data class TimeSlot(val weekend: Boolean, val hour: Int) {
    /** "주말 15시" */
    val label: String get() = "$groupLabel ${hour}시"

    /** "주말" · "평일" */
    val groupLabel: String get() = if (weekend) "주말" else "평일"

    /** slot_forecast.day_group 값 */
    val dayGroup: String get() = if (weekend) "weekend" else "weekday"
}
