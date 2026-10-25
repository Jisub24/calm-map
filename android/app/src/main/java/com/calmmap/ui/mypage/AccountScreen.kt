package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.ConfirmDialog
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/** 최종안 18-2 계정 관리 + 18-3 탈퇴 확인 창 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: 가입일 · 개수는 Supabase 에서 읽어온다
    joinedAt: String = "2026.10.24",
    visitCount: Int = 6,
    measuredCount: Int = 3,
    childCount: Int = 1,
) {
    var confirmWithdraw by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "계정 관리", onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard {
                CardTitle("로그인 정보")
                InfoRow("로그인 방식", "카카오")
                Divider()
                InfoRow("가입일", joinedAt)
            }
            WhiteCard {
                CardTitle("저장된 내 데이터")
                InfoRow("방문 기록", "${visitCount}건")
                Divider()
                InfoRow("직접 측정한 장소", "${measuredCount}곳")
                Divider()
                InfoRow("우리 아이 설정", "${childCount}명")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                ActionRow("로그아웃", TextPrimary, onLogout)
                Divider()
                ActionRow("회원 탈퇴", TextTertiary) { confirmWithdraw = true }
            }
            Text(
                text = "로그아웃해도 기록은 계정에 남아요. 다시 로그인하면 그대로 볼 수 있어요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
        }
    }

    if (confirmWithdraw) {
        ConfirmDialog(
            title = "정말 탈퇴할까요?",
            body = "방문 기록 ${visitCount}건과 우리 아이 설정이 모두 지워지고, 되돌릴 수 없어요.",
            confirmText = "탈퇴하기",
            onConfirm = {
                confirmWithdraw = false
                onWithdraw()
            },
            onDismiss = { confirmWithdraw = false },
        )
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderDefault),
    )
}

// 이름 · 값 한 줄
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
    }
}

// 누르는 한 줄: 이름 · ›
@Composable
private fun ActionRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
        CalmIcon(CalmIcons.ChevronRight, tint = TextPrimary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AccountScreenPreview() {
    CalmMapTheme {
        AccountScreen(onBack = {}, onLogout = {}, onWithdraw = {})
    }
}
