package com.calmmap.data

import android.util.Log
import com.calmmap.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

// 로그인 후 브라우저가 앱으로 돌려보내는 주소. AndroidManifest 와 Supabase 대시보드 Redirect URL 에도 같은 값을 쓴다
const val LOGIN_REDIRECT_SCHEME = "calmmap"
const val LOGIN_REDIRECT_HOST = "login"

// 앱 전체가 같이 쓰는 Supabase 연결. 처음 쓰는 순간 한 번만 만들어진다
val supabase by lazy {
    require(BuildConfig.SUPABASE_URL.isNotBlank()) {
        "local.properties 에 SUPABASE_URL, SUPABASE_ANON_KEY 를 넣어야 해요"
    }
    createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            // PKCE: 브라우저에서 받은 코드를 앱이 토큰으로 바꾸는 방식. 앱 로그인에 권장된다
            flowType = FlowType.PKCE
            scheme = LOGIN_REDIRECT_SCHEME
            host = LOGIN_REDIRECT_HOST
            // 로그인 정보는 기기에 저장돼서 앱을 다시 켜도 유지된다 (기본값)
        }
        // 표 읽기 · 쓰기. 로그인한 상태면 그 사람 권한(RLS)으로 요청된다
        install(Postgrest)
    }
}

// ── 로그인 토큰 ────────────────────────────────────────────

/** 만료까지 이보다 적게 남았으면 요청 전에 새로 받는다. 요청이 가는 동안 만료되지 않게 */
private val REFRESH_MARGIN = 60.seconds

/** 여러 화면이 동시에 새로 받으려 해도 한 번만 받게 줄 세운다 */
private val refreshLock = Mutex()

/**
 * DB 요청을 감싼다. 로그인 토큰(1시간짜리)이 만료됐거나 곧 만료되면 먼저 새로 받고,
 * 그래도 401(토큰 거절)이 오면 한 번 새로 받아 다시 보낸다. 로그인하지 않았으면 그냥 보낸다.
 *
 * supabase 가 토큰을 알아서 바꾸긴 하지만 그 타이머는 앱이 화면에 떠 있는 동안만 돈다.
 * PC 가 쉬거나 폰 화면이 꺼졌다 켜진 직후에는 만료된 토큰이 그대로 나가서 읽기가 전부 거절됐다 (이슈 #56).
 *
 * 401 은 서버가 요청을 받기 전에 거절한 것이라 쓰기 요청을 다시 보내도 두 번 들어가지 않는다.
 */
suspend fun <T> withFreshSession(block: suspend () -> T): T {
    refreshIfExpiring()
    val usedToken = supabase.auth.currentSessionOrNull()?.accessToken
    return try {
        block()
    } catch (e: RestException) {
        if (e.statusCode != 401 || usedToken == null) throw e
        refresh(usedToken)
        block()
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun refreshIfExpiring() {
    val session = supabase.auth.currentSessionOrNull() ?: return
    if (session.expiresAt - Clock.System.now() > REFRESH_MARGIN) return
    refresh(session.accessToken)
}

/** [staleToken] 이 아직 지금 토큰일 때만 새로 받는다. 기다리는 동안 다른 쪽이 이미 바꿨으면 건너뛴다 */
private suspend fun refresh(staleToken: String) = refreshLock.withLock {
    if (supabase.auth.currentSessionOrNull()?.accessToken == staleToken) {
        supabase.auth.refreshCurrentSession()
        Log.i("CalmMap", "로그인 토큰을 새로 받았어요")
    }
}

/**
 * 로그에 남길 짧은 오류 설명. "PostgrestRestException 401 JWT expired (PGRST303)".
 *
 * supabase 오류는 메시지와 스택에 요청 헤더가 통째로 들어 있어서 그대로 남기면 사용자 로그인 토큰이 찍힌다.
 * 그래서 오류를 Log 에 넘기지 않고 이 한 줄만 남긴다.
 */
fun Throwable.brief(): String {
    val name = this::class.simpleName ?: "Exception"
    return when (this) {
        is PostgrestRestException -> "$name $statusCode $error" + (code?.let { " ($it)" } ?: "")
        is RestException -> "$name $statusCode $error"
        else -> "$name: " + message?.lineSequence()?.firstOrNull()?.trim()?.take(150).orEmpty()
    }
}
