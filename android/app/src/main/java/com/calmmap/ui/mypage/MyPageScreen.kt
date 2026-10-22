package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.onboarding.SensoryFactor
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox

// TODO: Supabase user_profiles 에서 읽어오면 지운다
private val sampleSensitivities = linkedMapOf(
    SensoryFactor.Announcement to 4,
    SensoryFactor.BabyCrying to 3,
    SensoryFactor.Crowd to 2,
)

// E1 내 정보: 아이 민감 요인 요약과 메뉴 목록
@Composable
fun MyPageScreen(
    onTabSelect: (MainTab) -> Unit,
    onVisitHistoryClick: () -> Unit,
    onDataInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    sensitivities: Map<SensoryFactor, Int> = sampleSensitivities,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "내 정보")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        ) {
            // TODO: 수정하기는 아이 프로필 화면을 다시 여는 것으로 붙인다
            ProfileSummary(sensitivities = sensitivities, onEditClick = {})
            Spacer(modifier = Modifier.height(12.dp))

            MenuRow("방문 기록", onClick = onVisitHistoryClick)
            // TODO: 측정 기능(D)이 생기면 연결
            MenuRow("내가 측정한 장소", onClick = {})
            // TODO: 알림 기능이 생기면 연결
            MenuRow("알림 설정", onClick = {})
            MenuRow("데이터·개인정보 안내", onClick = onDataInfoClick)
            // TODO: 사업장 웹 주소가 정해지면 브라우저로 연다
            MenuRow("사업장이세요? 무던한 시간 등록", onClick = {})
        }

        BottomTabBar(selected = MainTab.ME, onSelect = onTabSelect)
    }
}

// 우리 아이 상자: 고른 요인을 "안내방송 ●●●●" 칩으로 보여준다
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSummary(
    sensitivities: Map<SensoryFactor, Int>,
    onEditClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperBox)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "우리 아이", style = MaterialTheme.typography.titleSmall, color = Ink)

        if (sensitivities.isEmpty()) {
            Text(
                text = "아직 설정한 민감 요인이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sensitivities.forEach { (factor, level) ->
                    SummaryChip(text = "${factor.label} ${"●".repeat(level)}")
                }
            }
        }

        Text(
            text = "수정하기 ›",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = InkSoft,
            modifier = Modifier.pressFade(role = Role.Button, onClick = onEditClick),
        )
    }
}

// 누를 수 없는 요약 칩. 선택된 칩과 같은 모양
@Composable
private fun SummaryChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Paper,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LineStrong)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// 메뉴 한 줄. 손가락으로 누르기 쉽게 높이를 48dp 로 잡는다
@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .bottomLine(LineFaint),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal, color = Ink)
        Text(text = "›", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MyPageScreenPreview() {
    CalmMapTheme {
        MyPageScreen(onTabSelect = {}, onVisitHistoryClick = {}, onDataInfoClick = {})
    }
}
