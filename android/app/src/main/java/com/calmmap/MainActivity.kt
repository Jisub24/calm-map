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
import com.calmmap.ui.onboarding.LocationPermissionScreen
import com.calmmap.ui.onboarding.LoginScreen
import com.calmmap.ui.onboarding.OnboardingScreen
import com.calmmap.ui.onboarding.SplashScreen
import com.calmmap.ui.theme.CalmMapTheme
import kotlinx.coroutines.delay

// 처음 진입 때 거치는 단계. 위에서 아래 순서로 진행한다
private enum class StartStep { Splash, Onboarding, Login, LocationPermission }

// 앱의 입구. 앱을 켜면 여기서 첫 화면을 띄운다
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmMapTheme {
                // TODO: 화면이 더 늘어나면 화면 이동(Navigation)으로 바꾼다
                // 지금 보여줄 단계. 바뀌면 아래 화면이 다시 그려진다
                var step by remember { mutableStateOf(StartStep.Splash) }

                // 처음 한 번만: 1.5초 뒤 스플래시에서 온보딩으로
                LaunchedEffect(Unit) {
                    delay(1500)
                    step = StartStep.Onboarding
                }

                // step 이 바뀔 때 화면을 왼쪽으로 밀어내며 바꾼다
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        // 새 화면은 오른쪽에서 들어오고, 이전 화면은 왼쪽으로 나간다
                        slideInHorizontally { fullWidth -> fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -fullWidth }
                    },
                    label = "startStep",
                ) { currentStep ->
                    when (currentStep) {
                        StartStep.Splash -> SplashScreen()
                        StartStep.Onboarding -> OnboardingScreen(onFinish = { step = StartStep.Login })
                        StartStep.Login -> LoginScreen(
                            // TODO: 카카오 로그인을 붙이면 로그인 성공 후에 넘어가게 바꾼다
                            onKakaoClick = { step = StartStep.LocationPermission },
                            // TODO: 게스트 로그인(signInAnonymously) 붙이기 전까지는 아무 동작 없음
                            onGuestClick = {},
                        )
                        StartStep.LocationPermission -> LocationPermissionScreen(
                            onDone = { /* TODO: 아이 프로필 화면으로 */ },
                        )
                    }
                }
            }
        }
    }
}
