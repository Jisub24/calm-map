package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.components.SecondaryButton
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox

// E3 데이터·개인정보 안내: 무엇을 모으고 무엇을 안 모으는지
@Composable
fun DataInfoScreen(
    onBack: () -> Unit,
    onDeleteAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "데이터 안내", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBox(
                title = "수집하는 것",
                items = listOf(
                    "소리 종류 라벨과 점수",
                    "소리 크기(상대값)와 측정 시각",
                    "아이 민감 요인 태그",
                    "방문 후 괜찮음/힘듦",
                ),
                filled = true,
            )
            InfoBox(
                title = "수집하지 않는 것",
                items = listOf(
                    "원본 소리(녹음 파일)",
                    "대화 내용·음성 특징",
                    "진단명·행동 기록",
                    "이름·연락처",
                ),
                filled = false,
            )
            Text(
                text = "소리 분류는 폰 안에서 끝나요 (온디바이스 AI).",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // 스크롤 영역 밖에 둬서 항상 아래에 보인다
        SecondaryButton(
            text = "내 데이터 모두 삭제",
            onClick = onDeleteAllClick,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )
    }
}

// 수집하는 것은 회색 바탕, 수집하지 않는 것은 테두리만
@Composable
private fun InfoBox(title: String, items: List<String>, filled: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (filled) Modifier.background(PaperBox)
                else Modifier.border(1.dp, LineSoft, shape),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Ink)
        items.forEach {
            Text(text = "• $it", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DataInfoScreenPreview() {
    CalmMapTheme {
        DataInfoScreen(onBack = {}, onDeleteAllClick = {})
    }
}
