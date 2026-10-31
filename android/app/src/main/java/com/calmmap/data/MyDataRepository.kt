package com.calmmap.data

import io.github.jan.supabase.postgrest.postgrest

/**
 * 내 데이터 모두 삭제 · 회원 탈퇴. DB 함수(028)를 부른다.
 *
 * user_profiles · measurements 에는 앱이 지우거나 바꿀 수 있는 정책이 없어서 앱이 표를 직접 고치지 않는다.
 * 방문 기록 · 우리 아이 설정 · 측정 요청은 지우고, 직접 잰 측정은 계정 연결만 끊어 익명으로 남긴다
 * (장소 점수의 재료이고 원본 소리 · 이름이 없다).
 */
object MyDataRepository {

    /** 내 데이터만 지운다. 계정과 로그인은 그대로 */
    suspend fun deleteAll() {
        withFreshSession { supabase.postgrest.rpc("delete_my_data") }
    }

    /** 데이터를 지우고 계정도 지운다. 끝나면 부르는 쪽에서 로그아웃한다 */
    suspend fun deleteAccount() {
        withFreshSession { supabase.postgrest.rpc("delete_my_account") }
    }
}
