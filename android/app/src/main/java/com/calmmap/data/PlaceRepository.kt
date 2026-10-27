package com.calmmap.data

import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

// 지도에 찍을 장소(places)와 영업시간(place_hours)을 읽는다. 로그인 없이 누구나 읽을 수 있다
object PlaceRepository {

    @Serializable
    private data class PlaceRow(
        val id: Long,
        val name: String,
        val category: String,
        val lat: Double,
        val lng: Double,
    )

    /** 요일 하나의 영업시간. 시각은 "09:00:00", 닫는 시각은 "24:00:00" 도 온다 */
    @Serializable
    private data class HoursRow(
        val place_id: Long,
        /** 1=월 … 7=일 */
        val weekday: Int,
        val is_closed: Boolean,
        val open_time: String? = null,
        val close_time: String? = null,
        /** 자정을 넘겨 다음 날 닫는지 */
        val closes_next_day: Boolean = false,
    )

    /** 모든 장소를 지도용으로 읽는다. 점수(hourly_forecast)는 아직 붙이지 않아 모두 '정보 부족' */
    suspend fun loadAll(now: LocalDateTime = LocalDateTime.now()): List<PlaceUi> = coroutineScope {
        // 두 표를 동시에 읽는다
        val places = async {
            supabase.from("places")
                .select(Columns.list("id", "name", "category", "lat", "lng"))
                .decodeList<PlaceRow>()
        }
        val hours = async {
            supabase.from("place_hours")
                .select(Columns.list("place_id", "weekday", "is_closed", "open_time", "close_time", "closes_next_day"))
                .decodeList<HoursRow>()
        }
        val hoursByPlace = hours.await().groupBy { it.place_id }
        places.await().map { row ->
            PlaceUi(
                id = row.id,
                name = row.name,
                category = row.category,
                position = LatLng(row.lat, row.lng),
                level = SensoryLevel.NO_DATA,
                description = "소리 · 빛 정보가 아직 없어요",
                walkMinutes = null,
                openNow = hoursByPlace[row.id]?.let { isOpen(it, now) },
                recommendation = "아직 측정이 부족해서 안내하기 어려워요",
                recommendationBasis = "이 장소의 소리 · 빛 정보가 아직 없어요",
            )
        }
    }

    /** 지금 영업 중인지. 오늘 영업시간과, 어제 자정을 넘겨 아직 안 닫은 경우를 같이 본다 */
    private fun isOpen(rows: List<HoursRow>, now: LocalDateTime): Boolean {
        val today = now.dayOfWeek.value // 월=1 … 일=7, DB 와 같다
        val yesterday = if (today == 1) 7 else today - 1
        val minute = now.hour * 60 + now.minute

        val todayRow = rows.find { it.weekday == today }
        if (todayRow != null && !todayRow.is_closed) {
            val open = minutesOf(todayRow.open_time) ?: return false
            val close = minutesOf(todayRow.close_time) ?: return false
            if (todayRow.closes_next_day) {
                if (minute >= open) return true
            } else if (minute in open until close) {
                return true
            }
        }
        // 어제 열어서 오늘 새벽에 닫는 가게 (예: 09:00 ~ 다음날 01:00)
        val yesterdayRow = rows.find { it.weekday == yesterday }
        if (yesterdayRow != null && !yesterdayRow.is_closed && yesterdayRow.closes_next_day) {
            val close = minutesOf(yesterdayRow.close_time) ?: return false
            if (minute < close) return true
        }
        return false
    }

    /** "09:30:00" → 570. "24:00:00" 은 1440 */
    private fun minutesOf(time: String?): Int? {
        val parts = time?.split(":") ?: return null
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return h * 60 + m
    }
}
