package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.VisitFeeling
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.VisitUi
import com.calmmap.ui.model.sampleVisits
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandOnPrimary
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.StatusCautionSoft
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import java.time.YearMonth
import java.time.ZoneId

/** 최종안 17 방문 기록: 이번 달 요약 · 예정된 방문(가장 가까운 1개) · 지난 방문 */
@Composable
fun VisitHistoryScreen(
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    visits: List<VisitUi> = sampleVisits,
    plans: List<VisitPlan> = emptyList(),
    onPlanClick: (VisitPlan) -> Unit = {},
    onAllPlansClick: () -> Unit = {},
    /** 지난 방문을 아직 못 읽었는지 · 읽다가 실패했는지 */
    loading: Boolean = false,
    failed: Boolean = false,
) {
    // 이번 달 것만 센다. 날짜가 없는 예시 기록은 모두 센다
    val thisMonth = remember(visits) {
        val now = YearMonth.now(ZoneId.of("Asia/Seoul"))
        visits.filter { v -> v.visitedOn?.let { YearMonth.from(it) == now } ?: true }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .statusBarsPadding(),
    ) {
        TopBar(title = "방문 기록", background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("이번 달 외출", "${thisMonth.size}회", Modifier.weight(1f))
                StatTile("괜찮았어요", "${thisMonth.count { it.feeling == VisitFeeling.OK }}회", Modifier.weight(1f))
            }

            plans.firstOrNull()?.let { next ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("예정된 방문", Modifier.weight(1f))
                    Text(
                        text = "모두 보기 ${plans.size} ›",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandPrimary,
                        modifier = Modifier.pressFade(role = Role.Button, onClick = onAllPlansClick),
                    )
                }
                UpcomingCard(next, onClick = { onPlanClick(next) })
            }

            SectionLabel("지난 방문")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp),
            ) {
                if (visits.isEmpty()) {
                    Text(
                        text = when {
                            loading -> "방문 기록을 불러오는 중이에요"
                            failed -> "기록을 불러오지 못했어요. 인터넷 연결을 확인해 주세요"
                            else -> "아직 다녀온 곳이 없어요"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
                visits.forEachIndexed { i, visit -> VisitRow(visit, divider = i < visits.lastIndex) }
            }
        }

        BottomTabBar(selected = MainTab.HISTORY, onSelect = onTabSelect)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextTertiary, modifier = modifier)
}

// 숫자 하나를 크게 보여주는 흰 칸
@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Radius.banner)
            .background(BgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.caption, color = TextTertiary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}

// 가장 가까운 예정 방문. 누르면 미리 보는 카드
@Composable
private fun UpcomingCard(plan: VisitPlan, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .pressFade(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(Radius.tile)
                .background(BrandPrimaryPressed)
                .padding(10.dp),
        ) {
            CalmIcon(CalmIcons.Calendar, size = 20.dp, tint = BrandOnPrimary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = plan.place.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text = "${plan.whenLabel} · 미리 보는 카드 준비됨", style = MaterialTheme.typography.caption, color = TextTertiary)
        }
        CalmIcon(CalmIcons.ChevronRight, size = 20.dp, tint = TextTertiary)
    }
}

// 방문 한 줄: 장소 · 시각, 오른쪽에 결과 태그. 힘들었으면 원인도 같이 주의 색으로
@Composable
private fun VisitRow(visit: VisitUi, divider: Boolean) {
    val hard = visit.feeling == VisitFeeling.HARD
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.bottomLine(BorderDefault) else Modifier)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = visit.placeName, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(text = visit.visitedAt, style = MaterialTheme.typography.caption, color = TextTertiary)
        }
        Text(
            text = listOfNotNull(visit.feeling.label, visit.trigger).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = if (hard) StatusCaution else TextSecondary,
            modifier = Modifier
                .clip(Radius.pill)
                .background(if (hard) StatusCautionSoft else BgSubtle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VisitHistoryScreenPreview() {
    CalmMapTheme {
        VisitHistoryScreen(onTabSelect = {})
    }
}
