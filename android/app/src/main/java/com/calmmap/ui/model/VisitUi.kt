package com.calmmap.ui.model

import java.time.LocalDate

/** 다녀온 뒤 아이 반응 */
enum class VisitFeeling(val label: String) {
    OK("괜찮았어요"),
    HARD("힘들었어요"),
}

/** 방문 기록 한 줄 */
data class VisitUi(
    val placeName: String,
    /** "10/14 (화) 15:00" */
    val visitedAt: String,
    val feeling: VisitFeeling,
    /** 힘들었을 때 원인으로 고른 자극. 없으면 null */
    val trigger: String? = null,
    /** 방문한 날 (서울). '이번 달 외출' 을 셀 때 쓴다. 예시 기록은 null */
    val visitedOn: LocalDate? = null,
)

// 미리보기용 예시. 앱에서는 visits 에서 읽은 기록을 쓴다
val sampleVisits = listOf(
    VisitUi("OO마트 성수점", "10/14 (화) 15:00", VisitFeeling.OK),
    VisitUi("OO도서관", "10/12 (일) 10:00", VisitFeeling.OK),
    VisitUi("OO역 광장", "10/11 (토) 17:00", VisitFeeling.HARD, trigger = "안내방송"),
    VisitUi("OO공원", "10/9 (목) 16:00", VisitFeeling.OK),
)
