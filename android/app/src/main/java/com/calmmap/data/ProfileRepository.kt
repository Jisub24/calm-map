package com.calmmap.data

import com.calmmap.ui.onboarding.Sensitivity
import com.calmmap.ui.onboarding.SensoryFactor
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

// 우리 아이 설정(user_profiles)을 읽고 쓴다. 계정 하나당 한 줄이고 자기 줄만 보인다 (RLS)
object ProfileRepository {

    /** user_profiles 한 줄. 칸 이름은 DB 와 같게 둔다 */
    @Serializable
    private data class ProfileRow(
        val user_id: String,
        val sensitivities: Map<String, String> = emptyMap(),
    )

    /** 저장된 설정을 읽는다. 아직 설정하지 않은 계정이면 null */
    suspend fun load(): Map<SensoryFactor, Sensitivity>? {
        // RLS 가 자기 줄만 돌려주므로 조건 없이 읽어도 0줄 또는 1줄이다
        val row = supabase.from("user_profiles")
            .select()
            .decodeSingleOrNull<ProfileRow>()
            ?: return null
        return SensoryFactor.entries.associateWith { factor ->
            // 키가 없거나 모르는 값이면 보통 (020 규칙)
            row.sensitivities[factor.dbKey()]?.let(::sensitivityOf) ?: Sensitivity.Normal
        }
    }

    // 앱 요인 ↔ DB 키 (020_user_profiles_levels.sql)
    private fun SensoryFactor.dbKey() = when (this) {
        SensoryFactor.SuddenSound -> "sudden"
        SensoryFactor.Alarm -> "alarm"
        SensoryFactor.LoudVoice -> "loud_voice"
        SensoryFactor.Machine -> "machine"
        SensoryFactor.SustainedLoud -> "sustained"
        SensoryFactor.Crowd -> "crowd"
        SensoryFactor.BrightLight -> "light"
    }

    private fun Sensitivity.dbValue() = when (this) {
        Sensitivity.Normal -> "normal"
        Sensitivity.Sensitive -> "sensitive"
        Sensitivity.Very -> "very"
    }

    private fun sensitivityOf(value: String) = Sensitivity.entries.firstOrNull { it.dbValue() == value }
}
