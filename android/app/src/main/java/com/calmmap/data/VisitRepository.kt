package com.calmmap.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.Serializable

// 방문 예정과 다녀온 뒤 기록(visits)을 다룬다. 자기 것만 읽고 쓰고 지울 수 있다 (RLS)
object VisitRepository {

    /** visits 에 넣는 한 줄. 결과(result · hard_factors)는 다녀온 뒤에 채운다 */
    @Serializable
    private data class NewVisitRow(
        val user_id: String,
        val place_id: Long,
        /** 방문 예정 시각. 서울 시간대를 붙여 보낸다 */
        val planned_at: String,
    )

    @Serializable
    private data class PlannedAtRow(val planned_at: String)

    @Serializable
    private data class IdRow(val id: Long)

    /** 방문 예정을 새로 넣고 visits id 를 돌려준다. 취소 · 기록할 때 이 id 로 찾는다 */
    suspend fun add(placeId: Long, date: LocalDate, hour: Int): Long {
        val userId = AuthRepository.userId() ?: error("로그인하지 않은 상태에서 저장하려고 했어요")
        val row = NewVisitRow(user_id = userId, place_id = placeId, planned_at = plannedAt(date, hour))
        return withFreshSession {
            supabase.from("visits")
                .insert(row) { select(Columns.list("id")) }
                .decodeSingle<IdRow>()
                .id
        }
    }

    /** 이미 있는 방문 예정의 시각만 바꾼다. 같은 장소를 다시 정했을 때 */
    suspend fun reschedule(visitId: Long, date: LocalDate, hour: Int) {
        withFreshSession {
            supabase.from("visits").update(PlannedAtRow(plannedAt(date, hour))) {
                filter { eq("id", visitId) }
            }
        }
    }

    /** "2026-10-31T15:00+09:00". 폰 시간대와 상관없이 서울 시각으로 */
    private fun plannedAt(date: LocalDate, hour: Int): String =
        date.atTime(hour, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString()
}
