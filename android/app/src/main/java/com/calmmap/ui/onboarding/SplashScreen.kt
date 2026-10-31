package com.calmmap.ui.onboarding

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.calmmap.ui.component.CalmLogo
import com.calmmap.ui.theme.BrandOnPrimary
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.CalmMapTheme

// 최종안 01 스플래시: 파란 바탕 가운데에 로고 · 앱 이름
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    // 파란 바탕이라 상태바 글자(시계 · 배터리)를 흰색으로. 화면을 떠나면 원래대로
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CalmLogo(size = 80.dp, tile = BrandOnPrimary)
            Text(text = "무던한 지도", style = MaterialTheme.typography.displayLarge, color = BrandOnPrimary)
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SplashScreenPreview() {
    CalmMapTheme { SplashScreen() }
}
