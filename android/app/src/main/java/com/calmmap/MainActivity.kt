package com.calmmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmmap.data.supabase
import com.calmmap.ui.map.MapScreen
import com.calmmap.ui.onboarding.ChildProfileScreen
import com.calmmap.ui.onboarding.LocationPermissionScreen
import com.calmmap.ui.onboarding.LoginScreen
import com.calmmap.ui.onboarding.OnboardingScreen
import com.calmmap.ui.onboarding.SplashScreen
import com.calmmap.ui.theme.CalmMapTheme
import com.naver.maps.map.NaverMapSdk
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 처음 진입 때 거치는 단계. 위에서 아래 순서로 진행하고, 끝나면 지도 홈으로 간다
private enum class StartStep { Splash, Onboarding, Login, LocationPermission, ChildProfile, Map }

// 앱의 입구. 앱을 켜면 여기서 첫 화면을 띄운다
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 지도 인증이 실패해도 네이버 SDK 는 아무 말 없이 빈 화면만 보여준다.
        // 원인(키가 틀렸는지, 패키지 이름이 안 맞는지)을 알 수 있게 이유를 남긴다.
        // 지도가 만들어지기 전에 걸어둬야 해서 setContent 앞에 둔다.
        NaverMapSdk.getInstance(this).onAuthFailedListener =
            NaverMapSdk.OnAuthFailedListener { exception ->
                Log.e("CalmMap", "네이버 지도 인증 실패: ${exception.message}", exception)
            }

        // 앱이 꺼진 상태에서 로그인 주소(calmmap://login)로 열렸을 때
        supabase.handleDeeplinks(intent)

        enableEdgeToEdge()
        setContent {
            CalmMapTheme {
                // TODO: 화면이 더 늘어나면 화면 이동(Navigation)으로 바꾼다
                // 지금 보여줄 단계. 바뀌면 아래 화면이 다시 그려진다
                var step by remember { mutableStateOf(StartStep.Splash) }
                val scope = rememberCoroutineScope()
                // 로그인 상태. 브라우저에서 돌아와 로그인이 끝나면 Authenticated 로 바뀐다
                val sessionStatus by supabase.auth.sessionStatus.collectAsStateWithLifecycle()

                // 로그인 화면에 있는데 로그인이 끝났으면 다음 단계로
                LaunchedEffect(sessionStatus) {
                    if (sessionStatus is SessionStatus.Authenticated && step == StartStep.Login) {
                        step = StartStep.LocationPermission
                    }
                }

                // 처음 한 번만: 1.5초 뒤 스플래시에서 온보딩으로
                LaunchedEffect(Unit) {
                    delay(1500)
                    step = StartStep.Onboarding
                }

                // step 이 바뀔 때 화면을 옆으로 밀어내며 바꾼다
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        // 앞 단계로 가면 왼쪽으로, 뒤로가기면 오른쪽으로 민다
                        val forward = targetState.ordinal > initialState.ordinal
                        val direction = if (forward) 1 else -1
                        slideInHorizontally { fullWidth -> fullWidth * direction } togetherWith
                            slideOutHorizontally { fullWidth -> -fullWidth * direction }
                    },
                    label = "startStep",
                ) { currentStep ->
                    when (currentStep) {
                        StartStep.Splash -> SplashScreen()
                        StartStep.Onboarding -> OnboardingScreen(onFinish = { step = StartStep.Login })
                        StartStep.Login -> LoginScreen(
                            // 브라우저로 카카오 로그인 창을 연다. 다음 단계 이동은 위 LaunchedEffect 가 한다
                            onKakaoClick = {
                                scope.launch {
                                    try {
                                        supabase.auth.signInWith(Kakao)
                                    } catch (e: Exception) {
                                        Log.e("CalmMap", "카카오 로그인 실패", e)
                                        Toast.makeText(this@MainActivity, "로그인하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            // TODO: 게스트 로그인(signInAnonymously) 붙이기 전까지는 아무 동작 없음
                            onGuestClick = {},
                        )
                        StartStep.LocationPermission -> LocationPermissionScreen(
                            onDone = { step = StartStep.ChildProfile },
                        )
                        StartStep.ChildProfile -> ChildProfileScreen(
                            // TODO: Supabase profiles 에 저장한 뒤 넘어가게 바꾼다
                            onStart = { _ -> step = StartStep.Map },
                            onSkip = { step = StartStep.Map },
                            onBack = { step = StartStep.LocationPermission },
                        )
                        StartStep.Map -> MapScreen()
                    }
                }
            }
        }
    }

    // 앱이 켜져 있는 상태에서 브라우저가 로그인 주소로 돌려보냈을 때 (singleTask)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}
