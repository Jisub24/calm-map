package com.calmmap.ui.model

/**
 * 소리 · 빛 측정이 없는 장소에서 보여줄 것.
 *
 * 종합 점수를 내지 않는다. 최댓값 방식에서 소리 · 빛이 비면 실제 최댓값을
 * 알 수 없어, 혼잡만으로 점수를 내면 **부담을 낮게 잘못 안내**하게 된다.
 *
 * 대신 두 가지를 준다.
 *   ① 주변 지역 혼잡 — 장소가 아니라 지역 값이라는 것을 분명히 적는다
 *   ② 측정 요청 버튼 — 정보가 없는 곳에서 보호자가 할 수 있는 일 (요청 수는 measurement_requests)
 */
data class NoDataUi(
    val areaName: String,
    val nowCongestion: String,
    /** 앞으로 몇 시간의 지역 혼잡. 장소 영업시간과 무관하게 지역 기준이다 */
    val forecast: List<AreaHour>,
    val reviewGood: Int,
    val reviewHard: Int,
) {
    val reviewTotal: Int get() = reviewGood + reviewHard
}

data class AreaHour(val hour: Int, val label: String)

/** 10/18 답사 전까지 쓰는 임시 데이터 */
val sampleParkNoData = NoDataUi(
    areaName = "홍대 관광특구",
    nowCongestion = "보통",
    forecast = listOf(
        AreaHour(15, "보통"),
        AreaHour(16, "보통"),
        AreaHour(17, "약간 붐빔"),
        AreaHour(18, "붐빔"),
    ),
    reviewGood = 2,
    reviewHard = 1,
)

/** 장소 번호로 정보 부족 화면 자료를 찾는다. 없으면 null */
fun sampleNoDataFor(placeId: Long): NoDataUi? = when (placeId) {
    5L -> sampleParkNoData
    else -> null
}
