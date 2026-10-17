package com.calmmap.ui.search

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
import androidx.compose.runtime.mutableStateListOf
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
import com.calmmap.ui.component.SearchIcon
import com.calmmap.ui.component.BackIcon
import com.calmmap.ui.component.SmallOutlineButton
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.pressHighlight
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.SensoryLevel

/**
 * 와이어프레임 B3 검색 화면.
 *
 * 우리가 가진 장소 안에서 이름 · 업종으로 찾는다.
 * 전국 아무 가게나 찾는 것이 아니다. 측정하지 않은 곳은 보여줘도
 * '정보 부족' 밖에 쓸 말이 없기 때문이다.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
) {
    var query by remember { mutableStateOf("") }
    var nearestFirst by remember { mutableStateOf(true) }
    var onlyCalm by remember { mutableStateOf(false) }
    // 측정을 요청한 장소. 한 사람이 한 번만 누를 수 있다
    val requested = remember { mutableStateListOf<Long>() }

    val results = remember(query, nearestFirst, onlyCalm, places) {
        places
            .filter { it.name.contains(query) || it.category.contains(query) }
            .filter { !onlyCalm || it.level == SensoryLevel.CALM || it.level == SensoryLevel.CANDIDATE }
            .sortedBy { if (nearestFirst) it.walkMinutes else it.level.ordinal }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onBack = onBack,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    label = "가까운 순",
                    selected = nearestFirst,
                    onClick = { nearestFirst = true },
                )
                FilterChip(
                    label = "무던한 시간 있는 곳만",
                    selected = onlyCalm,
                    onClick = { onlyCalm = !onlyCalm },
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = "검색 결과 ${results.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(results, key = { it.id }) { place ->
                if (place.level == SensoryLevel.NO_DATA) {
                    NoDataResult(
                        place = place,
                        requested = place.id in requested,
                        onClick = { onPlaceClick(place) },
                        onRequest = { requested += place.id },
                    )
                } else {
                    PlaceRow(
                        name = place.name,
                        description = place.description,
                        level = place.level,
                        onClick = { onPlaceClick(place) },
                        verticalPadding = 12.dp,
                    )
                }
            }

            item {
                Text(
                    text = "측정이 부족한 곳은 점수를 지어내지 않고 '정보 부족'으로 보여줘요.",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )
            }
        }

        BottomTabBar(selected = MainTab.MAP, onSelect = {})
    }
}

/**
 * 뒤로 가기 + 글자를 직접 받는 입력칸.
 *
 * 지도 홈의 검색창은 누르면 넘어가는 버튼이지만, 여기서는 진짜 입력칸이다.
 * 화면에 들어오면 바로 글자판이 올라오게 해서 한 번 더 누르지 않게 한다.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressHighlight(
                    shape = RoundedCornerShape(percent = 50),
                    role = Role.Button,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BackIcon(size = 20.dp, color = Ink)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(shape)
                .outlineBorder(LineStrong, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchIcon(size = 15.dp, color = Ink)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "장소 검색",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
        }
    }
}

/**
 * 소리 · 빛 정보가 없는 장소의 결과 줄.
 *
 * 다른 줄과 달리 아래에 '측정 요청' 버튼이 붙는다.
 * 정보가 없는 곳에서 보호자가 할 수 있는 일을 하나 주기 위한 것이고,
 * 요청이 많은 곳부터 측정하러 가는 기준이 된다.
 */
@Composable
private fun NoDataResult(
    place: PlaceUi,
    requested: Boolean,
    onClick: () -> Unit,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bottomLine(LineFaint)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressFade(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
            }
            LevelBadge(
                level = place.level,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        SmallOutlineButton(
            text = if (requested) "요청함" else "측정 요청 · 4명 요청함",
            onClick = onRequest,
            enabled = !requested,
        )
    }
}
