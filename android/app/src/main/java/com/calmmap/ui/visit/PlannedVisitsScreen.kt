package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.model.upcomingVisitDays
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

// 최종안 06-2 예정된 방문 · 06-3 없을 때. 지도 '방문 예정' 띠의 "더 보기" 로 들어온다
// 다녀온 기록(기록 탭)과 섞이지 않게 따로 둔다
@Composable
fun PlannedVisitsScreen(
    plans: List<VisitPlan>,
    onBack: () -> Unit,
    onPlanClick: (VisitPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "예정된 방문", onBack = onBack, background = BgCanvas)

        if (plans.isEmpty()) {
            EmptyPlans(Modifier.weight(1f))
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
                plans.forEachIndexed { i, plan ->
                    PlanRow(plan = plan, showDivider = i < plans.lastIndex, onClick = { onPlanClick(plan) })
                }
            }
            Text(
                text = "누르면 그 장소의 미리 보는 카드가 열려요. 다녀온 뒤 기록은 기록 탭에서 볼 수 있어요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
        }
    }
}

// 예정된 방문 한 줄: 달력 타일 · 장소(오늘이면 파란 태그) · 날짜 · 카드 보기
@Composable
private fun PlanRow(plan: VisitPlan, showDivider: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .let { if (showDivider) it.bottomLine(BorderDefault) else it }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(Radius.tile)
                .background(BrandPrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(CalmIcons.Calendar, size = 20.dp, tint = BrandPrimary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = plan.place.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                if (plan.day.label == "오늘") {
                    Text(
                        text = "오늘",
                        style = MaterialTheme.typography.labelSmall,
                        color = BgSurface,
                        modifier = Modifier
                            .clip(Radius.pill)
                            .background(BrandPrimary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(text = plan.recordLabel, style = MaterialTheme.typography.caption, color = TextTertiary)
        }
        Text(
            text = "카드 보기 ›",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )
    }
}

// 06-3 예정이 없을 때
@Composable
private fun EmptyPlans(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(Radius.pill)
                .background(BorderDefault),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(CalmIcons.Calendar, size = 28.dp, tint = TextTertiary)
        }
        Text(text = "예정된 방문이 없어요", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            text = "장소 상세에서 \"이 시간에 방문\"을 누르면 여기에 모여요.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PlannedVisitsScreenPreview() {
    val days = upcomingVisitDays()
    CalmMapTheme {
        PlannedVisitsScreen(
            plans = listOf(VisitPlan(samplePlaces[1], days[0], 15), VisitPlan(samplePlaces[0], days[2], 10)),
            onBack = {},
            onPlanClick = {},
        )
    }
}
