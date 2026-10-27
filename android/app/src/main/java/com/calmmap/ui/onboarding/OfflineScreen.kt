package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

// 최종안 01-1 인터넷 연결 없음: 앱을 켤 때 인터넷이 없으면 지도 · 장소를 볼 수 없어서 여기서 멈춘다
@Composable
fun OfflineScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    // 다시 확인하는 동안 버튼을 막는다
    checking: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        OfflineIllustration()
        Spacer(Modifier.height(40.dp))
        Text(
            text = "인터넷에 연결되어 있지 않아요",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "지도와 장소 정보는 인터넷이 있어야 볼 수 있어요.\n와이파이나 데이터를 켠 뒤 다시 시도해 주세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "다시 시도", enabled = !checking, onClick = onRetry)
    }
}

// 회색 카드 안 흰 원에 와이파이 꺼짐 아이콘
@Composable
private fun OfflineIllustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(Radius.illustration)
            .background(BgSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(Radius.pill)
                .background(BgSurface),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(CalmIcons.WifiOff, size = 48.dp, tint = TextSecondary)
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OfflineScreenPreview() {
    CalmMapTheme { OfflineScreen(onRetry = {}) }
}
