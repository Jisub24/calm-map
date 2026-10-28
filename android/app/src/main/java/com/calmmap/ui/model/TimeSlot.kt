package com.calmmap.ui.model

/**
 * 지도에서 고른 때. 평일 · 주말과 시각 하나.
 *
 * 점수는 장소 × 평일/주말 × 1시간 칸으로 모은다 (답변서1 질의2). 그래서 날짜가 아니라
 * 평일 · 주말만 고르게 한다. 어느 날 줄을 읽는지는 PlaceRepository 가 정한다.
 */
data class TimeSlot(val weekend: Boolean, val hour: Int) {
    /** "주말 15시" */
    val label: String get() = "${if (weekend) "주말" else "평일"} ${hour}시"
}
