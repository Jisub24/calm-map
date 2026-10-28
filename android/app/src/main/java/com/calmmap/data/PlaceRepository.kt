package com.calmmap.data

import com.calmmap.ui.model.HourBar
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 지도에 찍을 장소를 읽는다. 로그인 없이 누구나 읽을 수 있다.
 *
 *   places           이름 · 좌표 · 업종
 *   place_hours      영업시간 → "영업 중" 표시
 *   hourly_forecast  batch 가 30분마다 채우는 오늘의 시간대별 점수 → 마커 단계 · 그래프 · 결론 문구
 *
 * 시각은 서울 기준으로 잡는다. hourly_forecast 의 date · hour 가 한국 시각으로 들어 있어서,
 * 폰 시간대가 다르면(에뮬레이터는 UTC) 엉뚱한 칸을 읽게 된다.
 */
object PlaceRepository {

    private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

    /** 그래프에 그리는 시각 범위. 최종안 09 의 8~22시 */
    private val CHART_HOURS = 8..22

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

    /**
     * hourly_forecast 한 줄 = 장소 하나의 한 시각.
     *   status   level(단계 확정) / estimate(후보 · 추정, 점선) / insufficient(정보 부족)
     *   level    calm / normal / high. status 가 level 일 때만 값이 있다
     */
    @Serializable
    private data class ForecastRow(
        val place_id: Long,
        val hour: Int,
        val status: String,
        val level: String? = null,
        val is_estimated: Boolean = false,
        val total_score: Float? = null,
        val main_factor: String? = null,
        val measurement_count: Int = 0,
        /** 주변 지역 혼잡 1(여유)~4(붐빔). 서울시 예측이 12시간 앞까지라 없는 시각도 있다 */
        val area_crowd_level: Int? = null,
    )

    /** 모든 장소를 지도용으로 읽는다 */
    suspend fun loadAll(now: ZonedDateTime = ZonedDateTime.now(SEOUL)): List<PlaceUi> = coroutineScope {
        val today = now.toLocalDate().toString()   // "2026-10-28"
        // 세 표를 동시에 읽는다
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
        val forecast = async {
            supabase.from("hourly_forecast")
                .select(
                    Columns.list(
                        "place_id", "hour", "status", "level", "is_estimated",
                        "total_score", "main_factor", "measurement_count", "area_crowd_level",
                    ),
                ) { filter { eq("date", today) } }
                .decodeList<ForecastRow>()
        }
        val hoursByPlace = hours.await().groupBy { it.place_id }
        val forecastByPlace = forecast.await().groupBy { it.place_id }

        places.await().map { row ->
            val rows = forecastByPlace[row.id].orEmpty()
            val byHour = rows.associateBy { it.hour }
            // 지금 시각에 단계 · 후보가 있으면 그것. 없으면(정보 부족 · 영업 전후) 가장 가까운 시각의 단계 · 후보로.
            // 측정한 시각이 하나라도 있는 장소를 지도에서 정보 부족으로 보이게 하지 않으려는 것
            val known = rows.filter { it.status != "insufficient" }
            val nowRow = byHour[now.hour]?.takeIf { it.status != "insufficient" }
                ?: known.minByOrNull { kotlin.math.abs(it.hour - now.hour) }
                ?: byHour[now.hour]
            val level = levelOf(nowRow)
            val measured = rows.sumOf { it.measurement_count }
            PlaceUi(
                id = row.id,
                name = row.name,
                category = row.category,
                position = LatLng(row.lat, row.lng),
                level = level,
                description = describe(level, measured),
                walkMinutes = null,
                openNow = hoursByPlace[row.id]?.let { isOpen(it, now.toLocalDateTime()) },
                recommendation = recommend(byHour, now.hour),
                recommendationBasis = basis(rows, measured),
                hours = if (rows.isEmpty()) emptyList() else CHART_HOURS.map { bar(it, byHour[it]) },
                areaCrowd = if (rows.isEmpty()) emptyList() else CHART_HOURS.map { byHour[it]?.area_crowd_level },
            )
        }
    }

    // ── hourly_forecast → 화면 값 ──────────────────────────

    /** 마커 · 배지에 쓰는 단계. 줄이 없거나 정보 부족이면 NO_DATA, 추정이면 후보 */
    private fun levelOf(r: ForecastRow?): SensoryLevel = when {
        r == null || r.status == "insufficient" -> SensoryLevel.NO_DATA
        r.status == "estimate" -> SensoryLevel.CANDIDATE
        else -> levelWord(r.level)
    }

    private fun levelWord(level: String?): SensoryLevel = when (level) {
        "calm" -> SensoryLevel.CALM
        "normal" -> SensoryLevel.MID
        "high" -> SensoryLevel.HIGH
        else -> SensoryLevel.NO_DATA
    }

    /**
     * 그래프 막대 하나. 추정(점선) 막대도 높이와 진하기는 점수대로 그려야 해서
     * 단계 이름이 없으면 점수로 나눈다 (40 미만 무던함 · 70 미만 보통 · 그 위 부담 높음).
     * TODO: 기준선 40 · 70 은 settings.level_thresholds 와 같은 값. 설정을 읽게 되면 거기서 가져온다
     */
    private fun bar(hour: Int, r: ForecastRow?): HourBar {
        val score = r?.total_score
        if (r == null || r.status == "insufficient" || score == null) return HourBar(hour, null, 0f)
        val level = if (r.status == "level") levelWord(r.level) else when {
            score < 40f -> SensoryLevel.CALM
            score < 70f -> SensoryLevel.MID
            else -> SensoryLevel.HIGH
        }
        return HourBar(hour, level, (score / 100f).coerceIn(0f, 1f), estimated = r.is_estimated)
    }

    /** 목록 한 줄 설명. "지금 보통 · 직접 측정 3건" */
    private fun describe(level: SensoryLevel, measured: Int): String = when (level) {
        SensoryLevel.NO_DATA -> "소리 · 빛 정보가 아직 없어요"
        SensoryLevel.CANDIDATE -> "지금은 추정이에요 · 직접 측정 ${measured}건"
        else -> "지금 ${level.label} · 직접 측정 ${measured}건"
    }

    /**
     * 카드 맨 위 결론 한 줄. 점수를 줄 세우지 않고 **무던한 시간대** 를 앞세운다 (원칙 4).
     * 무던함은 실측으로 확정된 시각만 센다 (추정 시각은 '후보' 라 추천하지 않는다 — 답변서 질의 2).
     */
    private fun recommend(byHour: Map<Int, ForecastRow>, nowHour: Int): String {
        if (byHour.isEmpty()) return "아직 측정이 부족해서 안내하기 어려워요"
        val calmHours = CHART_HOURS.filter { h ->
            byHour[h]?.let { it.status == "level" && it.level == "calm" } == true
        }
        val upcoming = calmHours.filter { it >= nowHour }
        return when {
            upcoming.isNotEmpty() -> "오늘은 ${upcoming.first()}시 이후가 무던해요"
            calmHours.isNotEmpty() -> "오늘 무던한 시간은 지났어요 · ${calmHours.first()}시쯤이 무던했어요"
            byHour.values.any { it.status == "level" } -> "오늘은 무던한 시간대가 확인되지 않았어요"
            else -> "아직 추정만 있어요 · 측정이 쌓이면 안내해 드려요"
        }
    }

    /** 결론 아래 작은 줄. 무엇을 근거로 했는지 */
    private fun basis(rows: List<ForecastRow>, measured: Int): String = when {
        rows.isEmpty() -> "이 장소의 소리 · 빛 정보가 아직 없어요"
        measured == 0 -> "주변 지역 혼잡으로 추정했어요"
        else -> "직접 측정한 기록 ${measured}건을 바탕으로 예측했어요"
    }

    // ── 영업시간 ──────────────────────────────────────────

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
