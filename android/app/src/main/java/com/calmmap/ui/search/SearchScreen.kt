package com.calmmap.ui.search

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.caption
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.SmallOutlineButton
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.pressHighlight
import com.calmmap.data.KakaoPlaceRepository
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.SensoryLevel

/**
 * 와이어프레임 B3 검색 화면.
 *
 * 위쪽은 우리 지도에 있는 장소를 이름 · 업종으로 찾은 것, 아래쪽은 카카오 장소 검색 결과다.
 * 카카오 결과는 측정 기록이 없어 보여줄 점수가 없으니 누르면 그 가게에서 측정하러 간다 ([onMeasureClick]).
 * 측정을 저장하면 그 가게가 지도에 생긴다.
 * 검색어는 밖([query])에 둔다. 고른 가게가 아니라서 돌아왔을 때 치던 글자가 남아 있게.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
    onMeasureClick: (PlaceUi) -> Unit = {},
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    /** 장소별 측정 요청 수 (measurement_requests). 로그인 전이면 비어 있다 */
    requestCounts: Map<Long, Int> = emptyMap(),
    /** 내가 측정을 요청한 장소. 한 사람이 한 번만 요청할 수 있다 */
    requested: Set<Long> = emptySet(),
    /** '측정 요청' · '요청함'(누르면 취소). 로그인 확인 · 저장은 부르는 쪽에서 */
    onRequestClick: (PlaceUi) -> Unit = {},
) {
    var onlyCalm by remember { mutableStateOf(true) }
    var nearestFirst by remember { mutableStateOf(false) }
    var onlyOpen by remember { mutableStateOf(false) }

    val results = remember(query, nearestFirst, onlyCalm, onlyOpen, places) {
        places
            .filter { it.name.contains(query) || it.category.contains(query) }
            // '무던한 시간 있는 곳' 이 켜져 있어도 정보 부족 · 후보는 남긴다. 숨기면 측정 요청을 할 길이 없다
            .filter { !onlyCalm || it.level != SensoryLevel.HIGH }
            .filter { !onlyOpen || it.openNow == true }
            .sortedBy { if (nearestFirst) it.walkMinutes ?: Int.MAX_VALUE else it.level.ordinal }
    }
    val kakao = rememberKakaoSearch(query, places)
    val showKakao = query.isNotBlank() && KakaoPlaceRepository.enabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(query = query, onQueryChange = onQueryChange, onBack = onBack)
            // 지도 칩 줄과 같다. 글자 크기를 키운 폰에서는 옆으로 넘긴다
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(label = "무던한 시간 있는 곳", selected = onlyCalm, onClick = { onlyCalm = !onlyCalm })
                FilterChip(label = "가까운 순", selected = nearestFirst, onClick = { nearestFirst = !nearestFirst })
                FilterChip(label = "영업 중", selected = onlyOpen, onClick = { onlyOpen = !onlyOpen })
            }
        }
        // 검색 조건과 결과를 나누는 굵은 회색 띠
        Box(Modifier.fillMaxWidth().height(8.dp).background(BgSubtle))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            item {
                Text(
                    // 아래에 카카오 결과가 따로 붙으면 위쪽이 무엇인지 나눠 부른다
                    text = (if (showKakao) "지도에 있는 장소 " else "검색 결과 ") + results.size,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                )
            }

            items(results, key = { it.id }) { place ->
                PlaceRow(
                    name = place.name,
                    description = place.description,
                    level = place.level,
                    onClick = { onPlaceClick(place) },
                    // 정보가 없는 곳은 배지 대신 '측정 요청'. 요청이 많은 곳부터 측정하러 간다
                    trailing = if (place.level == SensoryLevel.NO_DATA) {
                        {
                            val done = place.id in requested
                            // 요청이 있으면 수를 붙인다. "측정 요청 3" · "요청함 3". 요청함을 다시 누르면 취소된다
                            val count = requestCounts[place.id]?.takeIf { it > 0 }?.let { " $it" }.orEmpty()
                            SmallOutlineButton(
                                text = (if (done) "요청함" else "측정 요청") + count,
                                onClick = { onRequestClick(place) },
                                leadingIcon = if (done) CalmIcons.Check else CalmIcons.Plus,
                            )
                        }
                    } else {
                        null
                    },
                )
            }

            if (showKakao) {
                item { KakaoSectionTitle(kakao, Modifier.padding(top = 20.dp, bottom = 6.dp)) }
                items(kakao.places, key = { "kakao-${it.id}" }) { place ->
                    KakaoPlaceRow(place, onMeasure = { onMeasureClick(place.toPlaceUi()) })
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .clip(Radius.card)
                        .background(BgSubtle)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CalmIcon(CalmIcons.Search, size = 16.dp, tint = TextSecondary)
                    Text(
                        text = "측정이 부족한 곳은 점수를 지어내지 않고 '정보 부족'으로 보여드려요.",
                        style = MaterialTheme.typography.caption,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

/**
 * 뒤로 가기가 안에 든 파란 테두리 입력칸 (최종안 08).
 * 지도 홈의 검색창은 누르면 넘어가는 버튼이지만, 여기서는 진짜 입력칸이다.
 * 화면에 들어오면 바로 글자판이 올라오게 해서 한 번 더 누르지 않게 한다.
 */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    /** null 이면 뒤로 가기 대신 돋보기를 둔다 (측정 장소 고르기처럼 위에 뒤로 가기가 따로 있을 때) */
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Height.search)
            .clip(Radius.pill)
            .background(BgSurface)
            .border(1.5.dp, BrandPrimary, Radius.pill)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pressHighlight(shape = Radius.pill, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                CalmIcon(CalmIcons.ArrowLeft, contentDescription = "뒤로")
            }
        } else {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CalmIcon(CalmIcons.Search, size = 20.dp, tint = TextTertiary)
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(text = "장소·주소 검색", style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(BrandPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            // 지우기: 회색 원 안의 ×
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(Radius.pill)
                    .background(BorderStrong)
                    .pressFade(role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                CalmIcon(CalmIcons.X, size = 14.dp, tint = BgSurface, contentDescription = "지우기")
            }
        }
    }
}
