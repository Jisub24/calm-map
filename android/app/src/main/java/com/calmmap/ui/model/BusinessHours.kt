package com.calmmap.ui.model

/** 사업장 무던한 시간 · 영업시간을 글자로 바꾸는 규칙. 사업장 웹(business-web/app.js)과 같게 쓴다 */
private val WEEKDAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

/** 요일 묶음. [1, 2, 3, 4, 5] → "월–금", [2, 4] → "화 · 목" (1=월 … 7=일) */
fun weekdaysLabel(days: Collection<Int>): String {
    val sorted = days.distinct().sorted()
    val run = sorted.size >= 3 && sorted.zipWithNext().all { (a, b) -> b == a + 1 }
    return if (run) "${WEEKDAY_NAMES[sorted.first() - 1]}–${WEEKDAY_NAMES[sorted.last() - 1]}"
    else sorted.joinToString(" · ") { WEEKDAY_NAMES[it - 1] }
}

/** "10:00:00" → "10", "10:30:00" → "10:30" */
fun clockShort(time: String): String {
    val (h, m) = time.split(":")
    val hour = h.trimStart('0').ifEmpty { "0" }
    return if (m == "00") hour else "$hour:$m"
}

/** place_hours 한 줄. 시각은 "10:00:00", 자정은 "24:00:00" */
class DayHours(val weekday: Int, val closed: Boolean, val open: String?, val close: String?, val nextDay: Boolean)

/** 영업시간 한 줄. "월–금" · "10:00 – 22:00" */
data class OpenHoursLine(val days: String, val time: String)

/** 같은 시간인 요일끼리 묶는다. 웹 영업시간 요약(renderHoursSummary)과 같은 글자. 등록 안 된 요일은 뺀다 */
fun openHoursLines(days: List<DayHours>): List<OpenHoursLine> =
    days.sortedBy { it.weekday }
        .groupBy { hoursText(it) }
        .map { (text, list) -> OpenHoursLine(weekdaysLabel(list.map { it.weekday }), text) }

private fun hoursText(d: DayHours): String {
    if (d.closed || d.open == null || d.close == null) return "쉬는 날"
    val open = d.open.take(5)
    val close = d.close.take(5)
    if (open == "00:00" && close == "24:00") return "24시간"
    return "$open – ${if (d.nextDay) "다음날 " else ""}$close"
}
