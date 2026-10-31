package com.calmmap.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/**
 * 측정 요청(measurement_requests, 014). 정보가 없는 장소에 "여기도 재 주세요" 를 남긴다.
 * 요청이 많은 곳부터 측정하러 간다. 한 사람이 한 장소에 한 번만 요청할 수 있다 (014 unique).
 * 로그인한 사람만 읽고 쓸 수 있다 (RLS)
 */
object MeasurementRequestRepository {

    @Serializable
    private data class RequestRow(val place_id: Long, val user_id: String)

    /** 장소별 요청 수와, 내가 요청한 장소 */
    data class Requests(val counts: Map<Long, Int>, val mine: Set<Long>)

    /** 모든 장소의 요청을 읽는다. 줄이 장소 수 × 사람 수라 한 번에 읽어도 작다 */
    suspend fun load(): Requests {
        val me = AuthRepository.userId()
        val rows = withFreshSession {
            supabase.from("measurement_requests")
                .select(Columns.list("place_id", "user_id"))
                .decodeList<RequestRow>()
        }
        return Requests(
            counts = rows.groupingBy { it.place_id }.eachCount(),
            mine = rows.filter { it.user_id == me }.map { it.place_id }.toSet(),
        )
    }

    /** 그 장소에 측정을 요청한다 */
    suspend fun request(placeId: Long) {
        val userId = AuthRepository.userId() ?: error("로그인하지 않은 상태에서 요청하려고 했어요")
        try {
            withFreshSession { supabase.from("measurement_requests").insert(RequestRow(place_id = placeId, user_id = userId)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 다른 기기에서 이미 요청했으면 한 사람 한 번 규칙에 걸려 넣기가 실패한다. 요청돼 있으면 된 것으로 본다
            if (placeId !in load().mine) throw e
        }
    }

    /** 그 장소에 남긴 내 요청을 지운다. RLS 가 자기 요청만 지우게 한다 */
    suspend fun cancel(placeId: Long) {
        val userId = AuthRepository.userId() ?: error("로그인하지 않은 상태에서 취소하려고 했어요")
        withFreshSession {
            supabase.from("measurement_requests").delete {
                filter {
                    eq("place_id", placeId)
                    eq("user_id", userId)
                }
            }
        }
    }
}
