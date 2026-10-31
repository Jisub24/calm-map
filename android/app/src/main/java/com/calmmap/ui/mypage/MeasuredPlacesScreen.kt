package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/** 내가 측정한 장소 한 줄. [place] 는 지도 장소라 단계 점과 상세 열기에 쓴다 */
data class MeasuredPlaceUi(val place: PlaceUi, val count: Int, val lastLabel: String)

// 내 정보 '내가 측정한 장소'. 누르면 그 장소 상세로
@Composable
fun MeasuredPlacesScreen(
    /** null 이면 아직 읽는 중 */
    places: List<MeasuredPlaceUi>?,
    failed: Boolean,
    onBack: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "내가 측정한 장소", onBack = onBack, background = BgCanvas)

        val empty = when {
            places == null && failed -> "목록을 불러오지 못했어요"
            places == null -> "불러오는 중…"
            places.isEmpty() -> "아직 직접 측정한 장소가 없어요.\n지도의 '측정' 버튼으로 시작할 수 있어요."
            else -> null
        }
        if (empty != null || places == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = empty.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                places.forEachIndexed { i, row ->
                    PlaceRow(
                        name = row.place.name,
                        description = "${row.count}번 측정 · 마지막 ${row.lastLabel}",
                        level = row.place.level,
                        onClick = { onPlaceClick(row.place) },
                        showDivider = i < places.lastIndex,
                    )
                }
            }
            Text(
                text = "누르면 그 장소 상세가 열려요. 측정은 장소 점수에 쓰여요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
        }
    }
}
