package com.calmmap.ui.search

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.calmmap.data.KakaoPlaceRepository
import com.calmmap.data.MyLocation
import com.calmmap.data.brief
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.SmallOutlineButton
import com.calmmap.ui.model.KakaoPlace
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** 카카오 검색 결과. [places] 는 우리 DB 에 이미 있는 가게를 뺀 것이다 */
data class KakaoSearchResult(
    val places: List<KakaoPlace> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

/**
 * [query] 로 카카오 장소를 찾는다. 글자를 칠 때마다 부르지 않고 잠깐 멈추면 부른다.
 * [known] 은 우리 DB 장소. 같은 가게는 위쪽 목록에 이미 나오니 카카오 결과에서 뺀다
 */
@Composable
fun rememberKakaoSearch(query: String, known: List<PlaceUi>): KakaoSearchResult {
    val context = LocalContext.current
    var result by remember { mutableStateOf(KakaoSearchResult()) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty() || !KakaoPlaceRepository.enabled) {
            result = KakaoSearchResult()
            return@LaunchedEffect
        }
        result = result.copy(loading = true, failed = false)
        delay(350)
        result = try {
            KakaoSearchResult(KakaoPlaceRepository.search(q, MyLocation.searchOrigin(context)))
        } catch (e: CancellationException) {
            // 검색어가 바뀌어 이전 검색을 버린 것. 실패가 아니다
            throw e
        } catch (e: Exception) {
            Log.w("CalmMap", "카카오 장소 검색 실패: ${e.brief()}")
            KakaoSearchResult(failed = true)
        }
    }
    return remember(result, known) {
        // 홍대 5곳처럼 카카오 id 가 없던 장소는 이름으로 맞춘다 (이름을 카카오 표기로 넣어 뒀다)
        val ids = known.mapNotNullTo(HashSet()) { it.kakaoId }
        val names = known.mapTo(HashSet()) { it.name }
        result.copy(places = result.places.filterNot { it.id in ids || it.name in names })
    }
}

/** 카카오 결과 묶음 제목. 측정하면 지도에 생긴다는 걸 같이 알린다 */
@Composable
fun KakaoSectionTitle(result: KakaoSearchResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "측정 기록이 없는 장소" + if (result.places.isEmpty()) "" else " ${result.places.size}",
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
        )
        Text(
            text = when {
                result.loading -> "찾는 중…"
                result.failed -> "장소를 찾지 못했어요. 인터넷 연결을 확인해 주세요"
                result.places.isEmpty() -> "맞는 장소가 없어요"
                else -> "골라서 측정하면 지도에 장소가 생겨요"
            },
            style = MaterialTheme.typography.caption,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 카카오 장소 한 줄. "320m · 카페 · 서울 마포구 양화로 166" + 여기서 측정. 누르면 지도에서 맞는 가게인지 먼저 확인한다 */
@Composable
fun KakaoPlaceRow(place: KakaoPlace, onMeasure: () -> Unit, showDivider: Boolean = true) {
    PlaceRow(
        name = place.name,
        description = place.summary,
        level = SensoryLevel.NO_DATA,
        onClick = onMeasure,
        showDivider = showDivider,
        trailing = { SmallOutlineButton(text = "여기서 측정", onClick = onMeasure, leadingIcon = CalmIcons.Mic) },
    )
}
