package com.calmmap.ui.model

import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng

/**
 * 카카오 장소 검색 결과 하나. 아직 우리 DB(places)에 없는 가게다.
 * 여기서 측정하면 저장할 때 places 에 새로 넣고(027 add_kakao_place) 그 장소로 저장한다.
 */
data class KakaoPlace(
    /** 카카오 장소 id. places.kakao_id 와 같은 값이라 DB 에 이미 있는 가게를 걸러낼 때 쓴다 */
    val id: String,
    val name: String,
    /** 카카오 업종 코드 (CE7 카페 · FD6 음식점 …). 슈퍼마켓 · 백화점은 비어 있다 */
    val categoryGroup: String,
    /** "음식점 > 카페 > 커피전문점 > 스타벅스" */
    val categoryName: String,
    /** 도로명 주소. 없으면 지번 주소 */
    val address: String,
    val position: LatLng,
    /** 검색 기준 위치에서 몇 m */
    val distanceM: Int?,
) {
    /** 목록에 보이는 업종. "음식점 > 카페 > 커피전문점" 이면 "카페" */
    val categoryLabel: String
        get() = categoryName.split(" > ").let { it.getOrNull(1) ?: it.first() }

    /** "320m · 카페 · 서울 마포구 양화로 166". 1km 넘으면 "1.2km" */
    val summary: String
        get() = listOfNotNull(
            distanceM?.let { if (it < 1000) "${it}m" else "%.1fkm".format(it / 1000.0) },
            categoryLabel,
            address,
        ).joinToString(" · ")

    /** 측정 화면에 넘길 장소. id 0 은 아직 DB 에 없다는 뜻이다 */
    fun toPlaceUi(): PlaceUi = PlaceUi(
        id = 0,
        name = name,
        category = categoryLabel,
        position = position,
        level = SensoryLevel.NO_DATA,
        description = "",
        walkMinutes = null,
        openNow = null,
        recommendation = "",
        recommendationBasis = "",
        kakao = this,
    )
}
