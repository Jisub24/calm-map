package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.model.upcomingVisitDays
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.Paper

// 예정된 방문 목록. 지도 '방문 예정' 띠의 "더 보기" 로 들어온다
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
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "예정된 방문", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            if (plans.isEmpty()) {
                Text(
                    text = "예정된 방문이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            plans.forEach { PlanRow(plan = it, onClick = { onPlanClick(it) }) }
        }
    }
}

// 예정된 방문 한 줄. 누르면 그 장소의 미리 보는 카드
@Composable
private fun PlanRow(plan: VisitPlan, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .bottomLine(LineFaint)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = plan.place.name, style = MaterialTheme.typography.bodyLarge, color = Ink)
            Text(text = plan.recordLabel, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        Text(text = "카드 보기 ›", style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PlannedVisitsScreenPreview() {
    val days = upcomingVisitDays()
    CalmMapTheme {
        PlannedVisitsScreen(
            plans = listOf(
                VisitPlan(samplePlaces[0], days[0], 17),
                VisitPlan(samplePlaces[1], days[1], 15),
            ),
            onBack = {},
            onPlanClick = {},
        )
    }
}
