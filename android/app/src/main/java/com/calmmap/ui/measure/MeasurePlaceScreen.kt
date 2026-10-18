package com.calmmap.ui.measure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.model.ENOUGH_MEASURES
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.sampleMeasureCount
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperSoft

// D1 측정할 장소 선택: 지금 위치 근처 장소 중 하나를 고른다
// TODO: 지금은 예시 장소를 가까운 순으로. 내 위치를 붙이면 실제 거리로
@Composable
fun MeasurePlaceScreen(
    onBack: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "어디서 측정할까요?", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
        ) {
            Text(text = "지금 위치 근처", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            places.sortedBy { it.walkMinutes }.forEach { place ->
                PlaceRow(place = place, onClick = { onPlaceClick(place) })
            }
            Text(
                text = "＋ 목록에 없는 장소 검색",
                style = MaterialTheme.typography.bodyLarge,
                color = InkSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressFade(role = Role.Button, onClick = onSearchClick)
                    .padding(vertical = 14.dp),
            )
        }

        Text(
            text = "1분 동안 폰을 들고 있으면 돼요. 원음은 저장하지 않아요.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PaperSoft)
                .border(1.dp, LineSoft, RoundedCornerShape(12.dp))
                .padding(14.dp),
        )
    }
}

// 장소 한 줄: 이름 · 거리와 측정 횟수. 횟수가 적으면 '데이터 부족'
@Composable
private fun PlaceRow(place: PlaceUi, onClick: () -> Unit) {
    val count = sampleMeasureCount(place.id)
    val distance = place.walkMinutes * 70
    val note = if (count < ENOUGH_MEASURES) " (데이터 부족)" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .bottomLine(LineFaint),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = place.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Ink)
        Text(
            text = "${distance}m · 측정 ${count}회$note",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MeasurePlaceScreenPreview() {
    CalmMapTheme {
        MeasurePlaceScreen(onBack = {}, onPlaceClick = {}, onSearchClick = {})
    }
}
