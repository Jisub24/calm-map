package com.calmmap.data

import com.calmmap.ui.model.HardFactor
import com.calmmap.ui.model.VisitFeeling
import com.calmmap.ui.model.VisitUi
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
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

    @Serializable
    private data class PlannedRow(val id: Long, val place_id: Long, val planned_at: String)

    /** 읽어 온 방문 예정 하나. 날짜와 시는 서울 기준 */
    data class Planned(val id: Long, val placeId: Long, val date: LocalDate, val hour: Int)

    /**
     * 아직 기록하지 않은(result 가 빈) 방문 예정을 읽는다. 지난 예정도 '다녀왔어요' 로 기록할 수 있게 같이 읽는다.
     * 같은 장소가 여러 줄이면 가장 최근에 만든 것 하나만 돌려준다 (앱을 다시 켜기 전에 생긴 중복)
     */
    suspend fun loadPlanned(): List<Planned> {
        val rows = withFreshSession {
            supabase.from("visits")
                .select(Columns.list("id", "place_id", "planned_at")) {
                    filter { exact("result", null) }
                }
                .decodeList<PlannedRow>()
        }
        return rows
            .groupBy { it.place_id }
            .map { (_, same) -> same.maxBy { it.id } }
            .map { row ->
                // DB 는 "2026-11-01T06:00:00+00:00" 처럼 세계 표준시로 준다
                val at = OffsetDateTime.parse(row.planned_at).atZoneSameInstant(SEOUL)
                Planned(id = row.id, placeId = row.place_id, date = at.toLocalDate(), hour = at.hour)
            }
    }

    /**
     * 방문 예정을 정하고 visits id 를 돌려준다. 취소 · 기록할 때 이 id 로 찾는다.
     * 그 장소에 기록 전 예정이 이미 있으면 새 줄 대신 그 줄의 시각을 바꾼다.
     * 앱이 기억하는 줄 번호가 아니라 DB 를 먼저 보고 정해서, 앱을 막 켰거나 DB 가 바뀌었어도 줄이 겹치지 않는다
     */
    suspend fun plan(placeId: Long, date: LocalDate, hour: Int): Long {
        val userId = AuthRepository.userId() ?: error("로그인하지 않은 상태에서 저장하려고 했어요")
        val at = plannedAt(date, hour)
        val existing = withFreshSession {
            supabase.from("visits")
                .select(Columns.list("id")) {
                    filter {
                        eq("place_id", placeId)
                        exact("result", null)
                    }
                }
                .decodeList<IdRow>()
        }.maxOfOrNull { it.id }

        if (existing != null) {
            withFreshSession {
                supabase.from("visits").update(PlannedAtRow(at)) { filter { eq("id", existing) } }
            }
            return existing
        }
        return withFreshSession {
            supabase.from("visits")
                .insert(NewVisitRow(user_id = userId, place_id = placeId, planned_at = at)) { select(Columns.list("id")) }
                .decodeSingle<IdRow>()
                .id
        }
    }

    /**
     * 그 장소의 방문 예정을 지운다. 기록 전 줄만 지워서 다녀온 뒤 기록은 남는다.
     * 줄 번호 하나가 아니라 장소로 지워서, 예전에 겹쳐 생긴 줄이 있어도 같이 사라진다
     */
    suspend fun cancel(placeId: Long) {
        withFreshSession {
            supabase.from("visits").delete {
                filter {
                    eq("place_id", placeId)
                    exact("result", null)
                }
            }
        }
    }

    /** 다녀온 뒤 기록. hard_factors 는 '힘들었어요' 일 때만 채운다 (011 규칙) */
    @Serializable
    private data class RecordRow(
        val result: String,
        val hard_factors: List<String>?,
        val recorded_at: String,
    )

    /**
     * 방문 예정에 다녀온 결과를 채운다. 아직 기록하지 않은 줄만 고친다.
     * 고친 줄이 없으면(다른 기기에서 지웠거나 이미 기록함) 예외. 저장된 줄 없이 화면만 넘어가지 않게
     */
    suspend fun record(visitId: Long, feeling: VisitFeeling, factors: List<HardFactor>) {
        val hard = feeling == VisitFeeling.HARD
        val row = RecordRow(
            result = if (hard) "hard" else "ok",
            // '잘 모르겠어요' 는 값이 없어서 빠진다. 하나도 안 남으면 비운다
            hard_factors = if (hard) factors.mapNotNull { it.dbValue }.ifEmpty { null } else null,
            recorded_at = Instant.now().toString(),
        )
        val updated = withFreshSession {
            supabase.from("visits").update(row) {
                select(Columns.list("id"))
                filter {
                    eq("id", visitId)
                    exact("result", null)
                }
            }.decodeList<IdRow>()
        }
        check(updated.isNotEmpty()) { "기록할 방문 예정을 찾지 못했어요" }
    }

    @Serializable
    private data class PlaceName(val name: String)

    /** 기록된 방문 한 줄. places(name) 은 place_id 로 이어진 장소 이름을 같이 받아 온 것 */
    @Serializable
    private data class HistoryRow(
        val planned_at: String,
        val result: String,
        val hard_factors: List<String>? = null,
        val places: PlaceName? = null,
    )

    /** 다녀와서 기록한 방문들. 방문 예정 시각이 최근인 것부터 */
    // TODO: 지금은 전부 읽는다. 기록이 쌓이면 최근 몇 건만 받아 오고 화면은 '더 보기' 로 나눈다
    suspend fun loadHistory(): List<VisitUi> {
        val rows = withFreshSession {
            supabase.from("visits")
                .select(Columns.raw("planned_at, result, hard_factors, places(name)")) {
                    // result 가 비어 있지 않은 줄 = 다녀와서 기록한 방문
                    filter { filterNot("result", FilterOperator.IS, null) }
                    order("planned_at", Order.DESCENDING)
                }
                .decodeList<HistoryRow>()
        }
        return rows.map { row ->
            val at = OffsetDateTime.parse(row.planned_at).atZoneSameInstant(SEOUL)
            val week = at.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            VisitUi(
                placeName = row.places?.name ?: "알 수 없는 장소",
                visitedAt = "${at.monthValue}/${at.dayOfMonth} ($week) ${"%02d".format(at.hour)}:00",
                feeling = if (row.result == "hard") VisitFeeling.HARD else VisitFeeling.OK,
                trigger = row.hard_factors
                    ?.mapNotNull { v -> HardFactor.entries.find { it.dbValue == v }?.label }
                    ?.joinToString(", ")
                    ?.ifEmpty { null },
                visitedOn = at.toLocalDate(),
            )
        }
    }

    /** "2026-10-31T15:00+09:00". 폰 시간대와 상관없이 서울 시각으로 */
    private fun plannedAt(date: LocalDate, hour: Int): String =
        date.atTime(hour, 0).atZone(SEOUL).toOffsetDateTime().toString()

    private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
}
