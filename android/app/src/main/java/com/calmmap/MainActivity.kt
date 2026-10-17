package com.calmmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calmmap.ui.onboarding.OnboardingScreen
import com.calmmap.ui.onboarding.SplashScreen
import com.calmmap.ui.theme.CalmMapTheme
import kotlinx.coroutines.delay

// 앱의 입구. 앱을 켜면 여기서 첫 화면을 띄운다
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmMapTheme {
                // TODO: 로그인 화면이 생기면 화면 이동(Navigation)으로 바꾼다
                // 스플래시를 보여줄지. 바뀌면 아래 if 가 다시 실행된다
                var showSplash by remember { mutableStateOf(true) }

                // 처음 한 번만: 1.5초 뒤 스플래시를 끈다
                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }

                // showSplash 가 바뀔 때 화면을 왼쪽으로 밀어내며 바꾼다
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        // 새 화면은 오른쪽에서 들어오고, 이전 화면은 왼쪽으로 나간다
                        slideInHorizontally { fullWidth -> fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -fullWidth }
                    },
                    label = "splash",
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else {
                        OnboardingScreen(onFinish = { /* TODO: 로그인 화면으로 */ })
                    }
                }
            }
        }
    }
}
