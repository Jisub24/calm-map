package com.calmmap.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime

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

    /** 화면에 부를 이름. 카카오 프로필 이름을 쓰고 앱에서는 고치지 않는다 (DB 에 이름 칸이 없다) */
    fun displayName(): String? {
        val meta = supabase.auth.currentUserOrNull()?.userMetadata ?: return null
        // 카카오가 이름을 담아 주는 칸이 계정마다 달라서 차례로 본다
        return listOf("name", "full_name", "preferred_username", "user_name", "nickname")
            .firstNotNullOfOrNull { key -> (meta[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } }
    }

    /** 가입일 "2026.10.24". 카카오로 처음 로그인한 날 */
    // createdAt 이 코틀린 표준 시간(kotlin.time.Instant)이라 아직 실험 기능 표시가 필요하다
    @OptIn(ExperimentalTime::class)
    fun joinedAt(): String? {
        val created = supabase.auth.currentUserOrNull()?.createdAt ?: return null
        return Instant.parse(created.toString())
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }
}
