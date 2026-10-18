package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.model.VisitFeeling
import com.calmmap.ui.model.VisitUi
import com.calmmap.ui.model.sampleVisits
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox

// E2 방문 기록: 이번 달 요약과 다녀온 곳 목록. 하단 '기록' 탭 화면
@Composable
fun VisitHistoryScreen(
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    visits: List<VisitUi> = sampleVisits,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "방문 기록")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            // TODO: 지금은 목록 전체를 센다. 데이터가 붙으면 이번 달 것만 센다
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBox(
                    label = "이번 달 외출",
                    value = "${visits.size}회",
                    modifier = Modifier.weight(1f),
                )
                SummaryBox(
                    label = "괜찮았어요",
                    value = "${visits.count { it.feeling == VisitFeeling.OK }}회",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (visits.isEmpty()) {
                Text(
                    text = "아직 다녀온 곳이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            visits.forEach { VisitRow(it) }
        }

        BottomTabBar(selected = MainTab.HISTORY, onSelect = onTabSelect)
    }
}

// 숫자 하나를 크게 보여주는 상자
@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PaperBox)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

// 방문 한 줄: 왼쪽에 장소와 시각, 오른쪽에 아이 반응
@Composable
private fun VisitRow(visit: VisitUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomLine(LineFaint)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = visit.placeName, style = MaterialTheme.typography.bodyLarge, color = Ink)
            Text(text = visit.visitedAt, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        // 힘들었으면 원인도 같이: "😣 힘들었어요 · 안내방송"
        val feeling = listOfNotNull(visit.feeling.label, visit.trigger).joinToString(" · ")
        Text(text = feeling, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VisitHistoryScreenPreview() {
    CalmMapTheme {
        VisitHistoryScreen(onTabSelect = {})
    }
}
