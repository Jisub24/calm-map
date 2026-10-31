package com.calmmap.ui.map

import androidx.compose.ui.text.style.TextOverflow
import com.calmmap.ui.component.ButtonSize
import com.calmmap.ui.detail.CautionBanner
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.ChartLegend
import com.calmmap.ui.component.SelectedHourNote
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.caption
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BgSubtle

/**
 * 와이어프레임 B2. 지도에서 장소를 누르면 아래에서 올라오는 카드.
 *
 * 순서가 중요하다. **권고 시간대를 점수보다 먼저, 가장 크게** 둔다.
 * 보호자가 알고 싶은 것은 "몇 점인가" 가 아니라 "언제 가면 되나" 이고,
 * 점수를 앞세우면 장소를 줄 세우게 된다.
 */
@Composable
fun PlaceSheet(
    place: PlaceUi,
    nowHour: Int,
    onDetailClick: () -> Unit,
    onVisitClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 카드를 아래로 끌어 내렸을 때. 지도 화면이 고른 장소를 비운다 */
    onDismiss: () -> Unit = {},
) {
    // 그래프에서 누른 시각. 말풍선이 떠 있는 동안만 값이 있다
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .calmShadow(Shadows.sheet, Radius.sheet)
            .clip(Radius.sheet)
            .background(BgSurface)
            // 카드 위에서 끌면 지도가 아니라 카드가 반응한다. 아래로 끌면 닫힌다
            .sheetDrag(open = true, onOpen = {}, onClose = onDismiss)
            // 카드의 빈 곳을 누르면 말풍선을 닫는다.
            // 말풍선을 닫는 방법이 같은 막대를 또 누르는 것뿐이면,
            // 어느 막대를 눌렀는지 잊었을 때 닫을 길이 없다
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { selectedHour = null }
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            LevelBadge(level = place.level, prefix = if (place.personal) "우리 아이 기준 ·" else null)
        }

        Text(
            text = place.infoLine,
            style = MaterialTheme.typography.caption,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )

        // 우리 아이 맞춤에서 '매우' 로 고른 자극이 관측된 곳. 장소 상세와 같은 상자
        place.caution?.let { CautionBanner(text = it, modifier = Modifier.padding(top = 12.dp)) }

        RecommendationBox(
            recommendation = place.recommendation,
            basis = place.recommendationBasis,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (place.hours.isNotEmpty()) {
            HourlyBarChart(
                bars = place.hours,
                // 지도에서 시간을 골랐으면 축에 그 시각을 '선택' 으로 표시한다
                nowHour = place.slot?.hour ?: nowHour,
                nowLabel = if (place.slot == null) "지금" else "선택",
                compact = true,
                selectedHour = selectedHour,
                // 같은 막대를 다시 누르면 말풍선을 닫는다
                onBarClick = { selectedHour = if (selectedHour == it.hour) null else it.hour },
                modifier = Modifier.padding(top = 14.dp),
            )
            // 막대를 누르면 범례 자리에 그 시간 설명. 같은 한 줄이라 카드 높이가 바뀌지 않는다
            val picked = place.hours.firstOrNull { it.hour == selectedHour }
            if (picked != null) {
                SelectedHourNote(bar = picked, modifier = Modifier.padding(top = 8.dp))
            } else {
                ChartLegend(modifier = Modifier.padding(top = 8.dp), bars = place.hours)
            }
        }

        place.businessNote?.let { note ->
            BusinessNote(note = note, modifier = Modifier.padding(top = 10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(text = "상세 보기", onClick = onDetailClick, size = ButtonSize.M, modifier = Modifier.weight(1f))
            PrimaryButton(text = "방문 계획", onClick = onVisitClick, size = ButtonSize.M, modifier = Modifier.weight(1f))
        }
    }
}

/** 카드에서 가장 크게 보이는 자리. 점수가 아니라 시간대를 말한다 (연한 파랑 상자) */
@Composable
internal fun RecommendationBox(
    recommendation: String,
    basis: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BrandPrimarySoft)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = recommendation, style = MaterialTheme.typography.titleMedium, color = BrandPrimaryPressed)
        Text(text = basis, style = MaterialTheme.typography.caption, color = TextSecondary)
    }
}

/** 사업장이 직접 등록한 시간. AI 예측과 구분해서 가게 아이콘과 '사업장 등록' 을 앞에 붙인다 */
@Composable
internal fun BusinessNote(note: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(Radius.pill)
            .background(BgSubtle)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(CalmIcons.Store, size = 18.dp, tint = TextSecondary)
        Text(
            text = "사업장 등록 · $note",
            style = MaterialTheme.typography.caption,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
