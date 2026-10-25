package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.ButtonVariant
import com.calmmap.ui.component.CalmButton
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.CalmLogo
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

// 최종안 03 로그인: 카카오로 시작하기 또는 로그인 없이 둘러보기
// 카카오 로그인 처리는 MainActivity 에 있다
// TODO: 로그인 없이 둘러보기(onGuestClick)는 Supabase Auth signInAnonymously 로 붙인다
@Composable
fun LoginScreen(
    onKakaoClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        CalmLogo(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(text = "무던한 지도", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "감각이 예민한 아이와 함께\n덜 힘든 시간에 외출해요",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))

        CalmButton(text = "카카오로 시작하기", onClick = onKakaoClick, variant = ButtonVariant.Kakao)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "로그인 없이 둘러보기", onClick = onGuestClick)
        Spacer(Modifier.height(16.dp))
        BrowseInfo()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "시작하면 이용약관과 개인정보 처리방침에 동의하게 돼요",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// 로그인 없이 할 수 있는 것과 없는 것
@Composable
private fun BrowseInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSubtle)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalmIcon(CalmIcons.Info, size = 18.dp, tint = TextTertiary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "로그인 없이도 지도와 장소별 혼잡도·감각 부담 그래프를 볼 수 있어요.",
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
            )
            Text(
                text = "측정, 방문 계획, 우리 아이 맞춤은 로그인 후 쓸 수 있어요.",
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    CalmMapTheme { LoginScreen(onKakaoClick = {}, onGuestClick = {}) }
}
