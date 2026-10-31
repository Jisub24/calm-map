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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.ConfirmDialog
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BgSubtle

// E3 데이터·개인정보 안내: 무엇을 모으고 무엇을 안 모으는지
@Composable
fun DataInfoScreen(
    onBack: () -> Unit,
    /** 확인 창에서 '삭제하기' 를 눌렀을 때 */
    onDeleteAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 로그인했을 때만 지울 데이터가 있다. 둘러보기 중이면 삭제 버튼을 뺀다 */
    canDelete: Boolean = true,
    /** 지우는 중. 버튼을 막아 두 번 누르지 않게 */
    deleting: Boolean = false,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = "내 데이터를 모두 지울까요?",
            body = "방문 기록 · 우리 아이 설정 · 측정 요청이 지워지고, 되돌릴 수 없어요. 계정은 그대로예요.",
            confirmText = "삭제하기",
            onConfirm = {
                confirmDelete = false
                onDeleteAllClick()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
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
                color = TextTertiary,
            )
            Text(
                text = "직접 잰 측정은 데이터를 지우거나 탈퇴해도 누구 것인지 알 수 없게 남아 장소 점수에 쓰여요. " +
                    "측정에는 원본 소리 · 이름 · 연락처가 없어요.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // 스크롤 영역 밖에 둬서 항상 아래에 보인다
        if (canDelete) {
            SecondaryButton(
                text = if (deleting) "지우는 중…" else "내 데이터 모두 삭제",
                onClick = { confirmDelete = true },
                enabled = !deleting,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            )
        }
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
                if (filled) Modifier.background(BgSubtle)
                else Modifier.border(1.dp, BorderDefault, shape),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        items.forEach {
            Text(text = "• $it", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
