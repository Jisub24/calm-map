package com.calmmap.ui.model

import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng
import java.time.LocalDate

/**
 * 화면에 그릴 장소 하나.
 *
 * DB 의 places 를 그대로 옮긴 것이 아니라 **화면이 필요한 것만** 담는다.
 * 예를 들어 '도보 6분' 은 DB 에 없다. 내 위치와 장소 좌표로 그때그때 계산한다.
 */
data class PlaceUi(
    val id: Long,
    val name: String,
    val category: String,
    val position: LatLng,
    val level: SensoryLevel,
    /** 목록 한 줄에 들어가는 설명. "도보 6분 · 지금부터 18시까지 · 실측 확인" */
    val description: String,
    /** 내 위치에서 걸어서 몇 분. 위치를 아직 안 쓰면 null 이고 화면에서 뺀다 */
    val walkMinutes: Int?,
    /** 지금 영업 중인지. 영업시간 정보가 없으면 null 이고 화면에서 뺀다 */
    val openNow: Boolean?,
    /** 카드에서 가장 크게 보이는 문구. 점수보다 무던한 시간을 앞에 둔다 */
    val recommendation: String,
    /** 그 문구의 근거. 어디까지가 실측이고 어디부터가 추정인지 */
    val recommendationBasis: String,
    /** 오늘 영업시간을 1시간 단위로. 비어 있으면 그래프를 그리지 않는다 */
    val hours: List<HourBar> = emptyList(),
    /** 사업장 웹에서 등록한 무던한 시간. 없으면 null. 점수에는 넣지 않는다 */
    val businessNote: String? = null,
    /** 오늘 8~22시 주변 지역 혼잡 1(여유)~4(붐빔). 서울시 예측이 없는 시각은 null. 비어 있으면 예시 값으로 그린다 */
    val areaCrowd: List<Int?> = emptyList(),
    /** 이 장소에 쌓인 직접 측정 횟수 (hourly_forecast.measurement_count 합). 3회부터 확정 단계 */
    val measureCount: Int = 0,
    /** 지도에서 고른 때. null 이면 지금 기준이다. level · description 이 이 시각 값이다 */
    val slot: TimeSlot? = null,
    /** hours · 결론 문구가 어느 날 값인지. null 이면 오늘 */
    val date: LocalDate? = null,
) {
    /** 그 날 점수가 한 칸이라도 있는지. 없으면 카드 대신 정보 부족 화면으로 보낸다 */
    val hasForecast: Boolean
        get() = hours.any { it.level != null }

    /** 카드 · 상세 제목 아래 한 줄. "카페 · 도보 6분 · 영업 중". 모르는 것은 뺀다 */
    val infoLine: String
        get() = buildList {
            add(category)
            walkMinutes?.let { add("도보 ${it}분") }
            openNow?.let { add(if (it) "영업 중" else "영업 종료") }
        }.joinToString(" · ")
}

/**
 * 10/18 홍대 답사 전까지 쓰는 임시 데이터.
 *
 * places 테이블이 비어 있어서 화면을 확인할 방법이 없다.
 * 답사에서 가게를 정하면 이 파일을 지우고 DB 에서 읽어온다.
 *
 * 좌표는 홍대입구역 주변의 빈 자리를 임의로 찍은 것이고 실제 가게가 아니다.
 */
val samplePlaces = listOf(
    PlaceUi(
        id = 1,
        name = "OO도서관",
        category = "도서관",
        position = LatLng(37.5590, 126.9205),
        level = SensoryLevel.CALM,
        description = "도보 6분 · 18시까지 무던해요",
        walkMinutes = 6,
        openNow = true,
        recommendation = "지금부터 18시까지 무던해요",
        recommendationBasis = "10~18시는 측정으로 확인했어요 · 19시 이후는 문을 닫아요",
        hours = listOf(
            HourBar(8, null, 0f),
            HourBar(9, SensoryLevel.CALM, 0.28f),
            HourBar(10, SensoryLevel.CALM, 0.3f),
            HourBar(11, SensoryLevel.CALM, 0.32f),
            HourBar(12, SensoryLevel.MID, 0.52f),
            HourBar(13, SensoryLevel.CALM, 0.34f),
            HourBar(14, SensoryLevel.CALM, 0.28f),
            HourBar(15, SensoryLevel.CALM, 0.3f),
            HourBar(16, SensoryLevel.CALM, 0.27f),
            HourBar(17, SensoryLevel.CALM, 0.31f),
            HourBar(18, SensoryLevel.CALM, 0.33f, estimated = true),
            HourBar(19, null, 0f),
            HourBar(20, null, 0f),
            HourBar(21, null, 0f),
            HourBar(22, null, 0f),
        ),
    ),
    PlaceUi(
        id = 2,
        name = "OO마트",
        category = "마트",
        position = LatLng(37.5572, 126.9252),
        level = SensoryLevel.MID,
        description = "도보 4분 · 14시부터 무던해요",
        walkMinutes = 4,
        openNow = true,
        recommendation = "오늘은 14시 이후가 무던해요",
        recommendationBasis = "14~17시는 측정으로 확인했고, 18시 이후는 추정이에요",
        hours = listOf(
            HourBar(8, SensoryLevel.MID, 0.5f),
            HourBar(9, SensoryLevel.MID, 0.55f),
            HourBar(10, SensoryLevel.MID, 0.55f),
            HourBar(11, SensoryLevel.HIGH, 0.78f),
            HourBar(12, SensoryLevel.HIGH, 0.85f),
            HourBar(13, SensoryLevel.MID, 0.59f),
            HourBar(14, SensoryLevel.CALM, 0.33f),
            HourBar(15, SensoryLevel.CALM, 0.3f),
            HourBar(16, SensoryLevel.CALM, 0.28f),
            HourBar(17, SensoryLevel.CALM, 0.31f),
            HourBar(18, SensoryLevel.CALM, 0.35f, estimated = true),
            HourBar(19, SensoryLevel.MID, 0.46f, estimated = true),
            HourBar(20, SensoryLevel.CALM, 0.28f, estimated = true),
            HourBar(21, null, 0f),
            HourBar(22, null, 0f),
        ),
        businessNote = "화 10~12시 음악 끔 · 조명 낮춤 (운영 계획)",
    ),
    PlaceUi(
        id = 3,
        name = "OO카페",
        category = "카페",
        position = LatLng(37.5548, 126.9220),
        level = SensoryLevel.HIGH,
        description = "도보 9분 · 갑작스러운 소리가 잦아요",
        walkMinutes = 9,
        openNow = true,
        recommendation = "오늘은 무던한 시간대가 확인되지 않았어요",
        recommendationBasis = "6번 측정했는데 모든 시간대가 보통 이상이었어요",
        hours = listOf(
            HourBar(8, SensoryLevel.MID, 0.52f),
            HourBar(9, SensoryLevel.MID, 0.56f),
            HourBar(10, SensoryLevel.MID, 0.58f),
            HourBar(11, SensoryLevel.HIGH, 0.82f),
            HourBar(12, SensoryLevel.HIGH, 0.88f),
            HourBar(13, SensoryLevel.HIGH, 0.8f),
            HourBar(14, SensoryLevel.MID, 0.62f),
            HourBar(15, SensoryLevel.HIGH, 0.76f),
            HourBar(16, SensoryLevel.HIGH, 0.79f),
            HourBar(17, SensoryLevel.MID, 0.64f),
            HourBar(18, SensoryLevel.HIGH, 0.81f, estimated = true),
            HourBar(19, SensoryLevel.HIGH, 0.84f, estimated = true),
            HourBar(20, null, 0f),
            HourBar(21, null, 0f),
            HourBar(22, null, 0f),
        ),
    ),
    PlaceUi(
        id = 4,
        name = "OO베이커리",
        category = "카페",
        position = LatLng(37.5588, 126.9270),
        level = SensoryLevel.CANDIDATE,
        description = "도보 8분 · 16시부터 무던할 것 같아요",
        walkMinutes = 8,
        openNow = true,
        recommendation = "16시 이후가 무던할 것 같아요",
        recommendationBasis = "측정은 1번뿐이고 나머지 시간은 추정이에요",
        hours = listOf(
            HourBar(8, SensoryLevel.CALM, 0.3f, estimated = true),
            HourBar(9, SensoryLevel.MID, 0.46f, estimated = true),
            HourBar(10, SensoryLevel.MID, 0.5f, estimated = true),
            HourBar(11, SensoryLevel.MID, 0.56f, estimated = true),
            HourBar(12, SensoryLevel.MID, 0.6f),
            HourBar(13, SensoryLevel.MID, 0.54f, estimated = true),
            HourBar(14, SensoryLevel.MID, 0.48f, estimated = true),
            HourBar(15, SensoryLevel.MID, 0.45f, estimated = true),
            HourBar(16, SensoryLevel.CALM, 0.34f, estimated = true),
            HourBar(17, SensoryLevel.CALM, 0.32f, estimated = true),
            HourBar(18, null, 0f),
            HourBar(19, null, 0f),
            HourBar(20, null, 0f),
            HourBar(21, null, 0f),
            HourBar(22, null, 0f),
        ),
    ),
    PlaceUi(
        id = 5,
        name = "OO공원",
        category = "공원",
        position = LatLng(37.5542, 126.9268),
        level = SensoryLevel.NO_DATA,
        description = "도보 12분 · 소리 · 빛 정보가 아직 없어요",
        walkMinutes = 12,
        openNow = true,
        recommendation = "아직 측정이 부족해서 안내하기 어려워요",
        recommendationBasis = "주변 지역 혼잡만 있고 이 장소의 소리 · 빛 정보가 없어요",
    ),
)
