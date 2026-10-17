package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.components.SecondaryButton
import com.calmmap.ui.theme.CalmMapTheme

// A3 로그인: 카카오 로그인 또는 게스트로 시작. 어느 쪽이든 끝나면 위치 권한 화면으로 간다
// TODO: 실제 로그인은 Supabase Auth 로 붙인다 (카카오: signInWithOAuth, 게스트: signInAnonymously)
@Composable
fun LoginScreen(
    onKakaoClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 위아래 Spacer 로 로고·앱 이름을 남는 공간 가운데에 둔다
        Spacer(modifier = Modifier.weight(1f))

        // 로고 자리. 로고 이미지가 나오면 Image 로 바꾼다
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "로고", fontSize = 12.sp, color = Color(0xFF808080))
        }
        Text(
            text = "무던한 지도",
            fontSize = 26.sp,
            lineHeight = 39.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
        )
        Text(
            text = "오늘, 무던한 시간에 나가요",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Color(0xFF737373),
        )

        Spacer(modifier = Modifier.weight(1f))

        // TODO: 실제 디자인 때 카카오 공식 버튼 가이드(노란색, 로고)로 바꾼다
        PrimaryButton(text = "카카오로 시작하기", onClick = onKakaoClick)
        SecondaryButton(text = "게스트로 둘러보기", onClick = onGuestClick)

        GuestNotice()

        Text(
            text = "시작하면 이용약관과 개인정보 처리방침에 동의하게 돼요",
            fontSize = 11.sp,
            lineHeight = 16.5.sp,
            color = Color(0xFF808080),
            textAlign = TextAlign.Center,
        )
    }
}

// 게스트로 시작하면 기록이 기기에만 남는다는 안내
@Composable
private fun GuestNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "게스트는 앱을 지우면 아이 설정과 방문 기록이 사라져요.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color(0xFF595959),
        )
        Text(
            text = "나중에 카카오로 연결하면 기록이 그대로 옮겨져요.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color(0xFF595959),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    CalmMapTheme {
        LoginScreen(onKakaoClick = {}, onGuestClick = {})
    }
}
