package com.calmmap.data

import com.calmmap.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient

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
    }
}
