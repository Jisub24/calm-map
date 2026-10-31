package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

/** 최종안 18-3 확인 창: 제목 · 설명 · 취소(Secondary) / 확인(Danger) 나란히 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "취소",
    /** 확인 버튼 모양. 지우는 일은 Danger, 설정을 올리는 것처럼 되돌릴 수 있는 일은 Primary */
    confirmVariant: ButtonVariant = ButtonVariant.Danger,
) {
    // 기본 너비 대신 좌우 24 여백 (최종안 312/360)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(Radius.card)
                .background(BgSurface)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalmButton(dismissText, onDismiss, Modifier.weight(1f), ButtonVariant.Secondary, ButtonSize.M)
                CalmButton(confirmText, onConfirm, Modifier.weight(1f), confirmVariant, ButtonSize.M)
            }
        }
    }
}
