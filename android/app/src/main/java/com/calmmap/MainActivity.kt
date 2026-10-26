package com.calmmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calmmap.ui.CalmMapApp
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
import com.calmmap.data.AuthRepository
import com.calmmap.data.ProfileRepository
import com.calmmap.data.supabase
import com.calmmap.ui.onboarding.ChildProfileScreen
import com.calmmap.ui.onboarding.LocationPermissionScreen
import com.calmmap.ui.onboarding.LoginScreen
import com.calmmap.ui.onboarding.OnboardingScreen
import com.calmmap.ui.onboarding.SplashScreen
import com.calmmap.ui.theme.CalmMapTheme
import com.naver.maps.map.NaverMapSdk
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.async
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
                // 로그인 없이 둘러보기로 들어왔는지. 그러면 우리 아이 설정을 건너뛰고 지도로 간다
                var browsing by remember { mutableStateOf(false) }
                // 로그인한 계정에 우리 아이 설정이 있는지. 위치 권한 다음에 설정 화면을 보일지 정한다
                var hasProfile by remember { mutableStateOf(false) }
                // 우리 아이 설정에서 뒤로 가면 돌아갈 곳 (위치 권한 또는 둘러보던 지도)
                var profileBack by remember { mutableStateOf(StartStep.LocationPermission) }
                val scope = rememberCoroutineScope()
                // 로그인 상태. 브라우저에서 돌아와 로그인이 끝나면 Authenticated 로 바뀐다
                val sessionStatus by AuthRepository.status.collectAsStateWithLifecycle()

                // 설정이 저장돼 있는지 DB 에서 확인한다.
                // 못 읽으면(인터넷 없음 등) 있다고 본다. 저장된 설정을 처음 값으로 덮어쓰지 않게
                suspend fun checkProfile(): Boolean {
                    hasProfile = try {
                        ProfileRepository.load() != null
                    } catch (e: Exception) {
                        Log.e("CalmMap", "우리 아이 설정 확인 실패", e)
                        true
                    }
                    return hasProfile
                }

                // 로그인이 끝났을 때 (로그인 화면 또는 둘러보던 지도의 로그인 안내 시트)
                LaunchedEffect(sessionStatus) {
                    if (sessionStatus !is SessionStatus.Authenticated) return@LaunchedEffect
                    when {
                        step == StartStep.Login -> {
                            browsing = false
                            checkProfile()
                            step = StartStep.LocationPermission
                        }
                        // 둘러보다 로그인했으면 설정이 없을 때만 설정 화면으로
                        browsing -> {
                            browsing = false
                            if (!checkProfile()) {
                                profileBack = StartStep.Map
                                step = StartStep.ChildProfile
                            }
                        }
                    }
                }

                // 처음 한 번만: 스플래시를 1.5초 보여주는 동안 저장된 로그인과 설정을 확인한다
                // 로그인 + 설정 있음 → 지도 / 로그인만 → 우리 아이 설정 / 로그인 안 됨 → 온보딩
                LaunchedEffect(Unit) {
                    val next = async {
                        when {
                            !AuthRepository.awaitLoggedIn() -> StartStep.Onboarding
                            checkProfile() -> StartStep.Map
                            else -> StartStep.ChildProfile
                        }
                    }
                    delay(1500)
                    step = next.await()
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
                                        AuthRepository.signInWithKakao()
                                    } catch (e: Exception) {
                                        Log.e("CalmMap", "카카오 로그인 실패", e)
                                        Toast.makeText(this@MainActivity, "로그인하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            // 최종안 흐름: 로그인 없이 둘러보기 → 위치 권한 → 지도 (보기 전용)
                            // TODO: signInAnonymously 와 보기 전용 제한(로그인 안내 시트)은 따로 붙인다
                            onGuestClick = {
                                browsing = true
                                step = StartStep.LocationPermission
                            },
                        )
                        StartStep.LocationPermission -> LocationPermissionScreen(
                            onDone = {
                                profileBack = StartStep.LocationPermission
                                step = if (browsing || hasProfile) StartStep.Map else StartStep.ChildProfile
                            },
                        )
                        StartStep.ChildProfile -> ChildProfileScreen(
                            // TODO: Supabase profiles 에 저장한 뒤 넘어가게 바꾼다
                            onStart = { _ -> step = StartStep.Map },
                            onBack = { step = profileBack },
                        )
                        StartStep.Map -> CalmMapApp(
                            isGuest = browsing,
                            // 로그인 안내 시트의 카카오 버튼. 로그인이 끝나면 sessionStatus 가 바뀐다
                            onLoginClick = {
                                scope.launch {
                                    try {
                                        AuthRepository.signInWithKakao()
                                    } catch (e: Exception) {
                                        Log.e("CalmMap", "카카오 로그인 실패", e)
                                    }
                                }
                            },
                            // 계정 관리 > 로그아웃. 기록은 계정에 남는다
                            onLogout = {
                                scope.launch {
                                    try {
                                        AuthRepository.signOut()
                                    } catch (e: Exception) {
                                        Log.e("CalmMap", "로그아웃 실패", e)
                                    }
                                    browsing = false
                                    hasProfile = false
                                    step = StartStep.Login
                                }
                            },
                        )
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
