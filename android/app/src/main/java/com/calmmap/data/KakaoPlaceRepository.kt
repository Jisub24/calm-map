package com.calmmap.data

import com.calmmap.BuildConfig
import com.calmmap.ui.model.KakaoPlace
import com.naver.maps.geometry.LatLng
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 카카오 장소 검색과, 고른 가게를 우리 DB(places)에 넣는 일.
 *
 *   검색   카카오 Local 키워드 검색. 검색 기준 위치에서 가까운 순으로 15곳
 *   넣기   027 add_kakao_place 함수. 지역 코드 · 업종은 DB 가 정한다. 이미 있으면 그 id 를 돌려준다
 *
 * 네이버 지역 검색은 내 위치 기준 정렬이 안 되고 5곳까지만 줘서 카카오를 쓴다.
 */
object KakaoPlaceRepository {

    private const val SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json"

    private val http by lazy { HttpClient(OkHttp) }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SearchResponse(val documents: List<Document> = emptyList())

    /** 카카오가 주는 장소 한 줄. 좌표 · 거리도 글자로 온다 */
    @Serializable
    private data class Document(
        val id: String,
        val place_name: String,
        val category_name: String = "",
        val category_group_code: String = "",
        val address_name: String = "",
        val road_address_name: String = "",
        val x: String,
        val y: String,
        val distance: String = "",
    )

    /** 키가 없으면(local.properties 에 KAKAO_REST_KEY 를 안 넣었으면) 검색하지 않는다 */
    val enabled: Boolean get() = BuildConfig.KAKAO_REST_KEY.isNotBlank()

    suspend fun search(query: String, origin: LatLng): List<KakaoPlace> {
        if (!enabled) return emptyList()
        val response = http.get(SEARCH_URL) {
            header(HttpHeaders.Authorization, "KakaoAK ${BuildConfig.KAKAO_REST_KEY}")
            parameter("query", query)
            parameter("x", origin.longitude)
            parameter("y", origin.latitude)
            parameter("sort", "distance")
            parameter("size", 15)
        }
        check(response.status.isSuccess()) { "카카오 검색 ${response.status.value}" }
        return json.decodeFromString<SearchResponse>(response.bodyAsText()).documents.map { d ->
            KakaoPlace(
                id = d.id,
                name = d.place_name,
                categoryGroup = d.category_group_code,
                categoryName = d.category_name,
                address = d.road_address_name.ifBlank { d.address_name },
                position = LatLng(d.y.toDouble(), d.x.toDouble()),
                distanceM = d.distance.toIntOrNull(),
            )
        }
    }

    /** 고른 가게를 places 에 넣고 id 를 돌려준다. 로그인해야 부를 수 있다 */
    suspend fun register(place: KakaoPlace): Long = withFreshSession {
        supabase.postgrest.rpc(
            "add_kakao_place",
            buildJsonObject {
                put("p_kakao_id", place.id)
                put("p_name", place.name)
                put("p_category_group", place.categoryGroup)
                put("p_category_name", place.categoryName)
                put("p_address", place.address)
                put("p_lat", place.position.latitude)
                put("p_lng", place.position.longitude)
            },
        ).decodeAs<Long>()
    }
}
