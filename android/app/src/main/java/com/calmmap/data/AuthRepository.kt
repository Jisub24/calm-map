package com.calmmap.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

// 로그인 · 로그아웃 · 지금 로그인 상태를 한곳에서 다룬다. 화면은 supabase.auth 를 직접 부르지 않는다
object AuthRepository {

    /** 로그인 상태. 브라우저에서 돌아와 로그인이 끝나면 Authenticated 로 바뀐다 */
    val status: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    /** 앱을 켤 때 기기에 저장된 로그인을 다 읽을 때까지 기다린 뒤 로그인돼 있는지 알려준다 */
    suspend fun awaitLoggedIn(): Boolean {
        val settled = status.first { it !is SessionStatus.Initializing }
        // RefreshFailure: 로그인은 돼 있는데 인터넷이 없어 토큰을 못 바꾼 경우. 로그인으로 본다
        return settled is SessionStatus.Authenticated || settled is SessionStatus.RefreshFailure
    }

    /** 브라우저로 카카오 로그인 창을 연다. 결과는 status 로 들어온다 */
    suspend fun signInWithKakao() = supabase.auth.signInWith(Kakao)

    suspend fun signOut() = supabase.auth.signOut()

    /** 지금 로그인한 사람의 번호. user_profiles.user_id 와 같은 값 */
    fun userId(): String? = supabase.auth.currentUserOrNull()?.id
}
