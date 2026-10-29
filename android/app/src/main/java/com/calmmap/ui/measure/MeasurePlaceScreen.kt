package com.calmmap.ui.measure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.TopBar
import com.calmmap.data.KakaoPlaceRepository
import com.calmmap.ui.search.KakaoPlaceRow
import com.calmmap.ui.search.KakaoSectionTitle
import com.calmmap.ui.search.SearchField
import com.calmmap.ui.search.rememberKakaoSearch
import com.calmmap.ui.model.ENOUGH_MEASURES
import com.calmmap.ui.model.MEASURE_SECONDS
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/**
 * 측정할 장소 고르기. 지도의 '측정' 버튼에서 들어온다 (장소 상세에서는 장소가 정해져 있어 바로 측정 중으로).
 * 최종안 15-0. 검색 · 기록 화면과 같은 틀이다.
 * 목록에 없는 가게는 위 검색칸으로 카카오에서 찾아 고른다. 저장할 때 그 가게가 지도에 생긴다.
 * 검색어는 밖([query])에 둔다. 카카오 가게를 확인하러 지도에 갔다 돌아와도 남아 있게.
 */
// TODO: 지금은 지도 장소를 가까운 순으로. 내 위치를 붙이면 실제 거리로
@Composable
fun MeasurePlaceScreen(
    onBack: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
) {
    val q = query.trim()
    val sorted = places
        .filter { q.isEmpty() || it.name.contains(q) || it.category.contains(q) }
        .sortedBy { it.walkMinutes ?: Int.MAX_VALUE }
    val kakao = rememberKakaoSearch(query, places)
    val showKakao = q.isNotEmpty() && KakaoPlaceRepository.enabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "어디서 측정할까요?", onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onBack = null,
                autoFocus = false,
            )
            if (sorted.isNotEmpty()) {
                Text(
                    text = if (q.isEmpty()) "지금 위치 근처" else "지도에 있는 장소",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                )
            }
            if (sorted.isNotEmpty()) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                sorted.forEachIndexed { i, place ->
                    val count = place.measureCount
                    // 측정이 모자란 곳은 한 번 더 재면 도움이 된다는 걸 알린다
                    val need = if (count < ENOUGH_MEASURES) " · 부족" else ""
                    PlaceRow(
                        name = place.name,
                        // 걸음 1분 ≈ 70m. 위치를 모르면 거리는 뺀다
                        description = (place.walkMinutes?.let { "${it * 70}m · " } ?: "") + "측정 ${count}회$need",
                        level = place.level,
                        onClick = { onPlaceClick(place) },
                        showDivider = i < sorted.lastIndex,
                    )
                }
            }

            if (showKakao) {
                KakaoSectionTitle(kakao)
                if (kakao.places.isNotEmpty()) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius.card)
                        .background(BgSurface)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    kakao.places.forEachIndexed { i, place ->
                        KakaoPlaceRow(place, onMeasure = { onPlaceClick(place.toPlaceUi()) }, showDivider = i < kakao.places.lastIndex)
                    }
                }
            }
        }

        // 측정 전에 알아둘 것: 시간 · 원음 저장 안 함. 회색 바탕 위라 흰 카드로
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clip(Radius.card)
                .background(BgSurface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalmIcon(CalmIcons.Lock, size = 18.dp, tint = TextSecondary)
            Text(
                text = "${MEASURE_SECONDS / 60}분 동안 폰을 들고 있으면 돼요. 원음은 저장하지 않아요.",
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MeasurePlaceScreenPreview() {
    CalmMapTheme {
        MeasurePlaceScreen(onBack = {}, onPlaceClick = {})
    }
}
