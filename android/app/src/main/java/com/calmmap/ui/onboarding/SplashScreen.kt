package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.theme.CalmMapTheme

// A1 스플래시: 앱 이름과 한 줄 소개를 화면 가운데에 보여준다
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp),
        // 두 줄 사이 8, 묶음은 화면 정가운데
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "무던한 지도",
            fontSize = 28.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
        )
        Text(
            text = "오늘, 무던한 시간에 나가요",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Color(0xFF737373),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SplashScreenPreview() {
    CalmMapTheme {
        SplashScreen()
    }
}
