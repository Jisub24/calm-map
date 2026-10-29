package com.calmmap.data

import android.util.Log
import com.calmmap.ui.model.CalmHours
import com.calmmap.ui.model.LightInfo
import com.calmmap.ui.model.PlaceDetailUi
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.ScoreBreakdown
import com.calmmap.ui.model.ScoreFactor
import com.calmmap.ui.model.SoundRow
import com.calmmap.ui.theme.SensoryLevel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 장소 상세(최종안 09)의 탭 네 개가 읽는 값. 로그인 없이 누구나 읽을 수 있는 표만 쓴다.
 *
 *   점수   hourly_forecast 의 지금 시각 칸 (하위 점수 4개 · 주 요인 · 실측 횟수)
 *          + congestion_areas(지역 이름) + commercial(상권 단계)
 *   소리   place_sound_summary (024 뷰) — 장소별 소리 종류 비율 (측정끼리 중앙값)
 *   빛     place_measure_summary (024 뷰) — 장소별 조도 중앙값
 *
 * 밝기 · 배경음악 · 안내방송 빈도는 장소 고유값이라 시간대로 나누지 않는다 (답변서2 질의1).
 * 시간대마다 달라지는 점수는 점수 탭이 hourly_forecast 의 칸에서 읽는다.
 *   사업장 business_calm_hours — 가게가 등록한 조용한 시간
 *
 * 024 뷰가 아직 없으면 소리 · 빛 탭은 "아직 측정이 없어요" 로 보이고 나머지는 정상이다.
 */
object PlaceDetailRepository {

    private const val TAG = "PlaceDetail"

    private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

    @Serializable
    private data class ForecastRow(
        val hour: Int,
        val status: String,
        val level: String? = null,
        val is_estimated: Boolean = false,
        val total_score: Float? = null,
        val main_factor: String? = null,
        val measurement_count: Int = 0,
        val area_crowd_level: Int? = null,
        val score_crowd: Float? = null,
        val score_loudness: Float? = null,
        val score_sound: Float? = null,
        val score_light: Float? = null,
    )

    @Serializable
    private data class PlaceRow(val id: Long, val congestion_area_cd: String? = null)

    @Serializable
    private data class AreaRow(val area_cd: String, val area_nm: String)

    @Serializable
    private data class CommercialRow(val id: Long, val level: Int, val level_text: String)

    @Serializable
    private data class CommercialCategoryRow(val mid_ctgr: String, val level: Int, val level_text: String)

    /** 가게가 등록한 조용한 시간 한 줄. 시각은 "10:00:00" */
    @Serializable
    private data class CalmHourRow(
        val weekday: Int,
        val start_time: String,
        val end_time: String,
        val actions: List<String>? = null,
        val custom_action: String? = null,
    )

    @Serializable
    private data class SoundSummaryRow(val sound_type: String, val ratio: Double, val measurement_count: Int)

    @Serializable
    private data class MeasureSummaryRow(
        val measurement_count: Int,
        val db_median: Double? = null,
        val lux_median: Double? = null,
        val lux_count: Int = 0,
        val sudden_per_hour: Double? = null,
        val announce_per_hour: Double? = null,
        val crowd_median: Int? = null,
    )

    /**
     * 지도에서 시간을 고르지 않았으면 오늘 줄(hourly_forecast)의 [nowHour] 시,
     * 골랐으면(PlaceUi.slot) 쌓인 평일/주말 줄(slot_forecast)의 그 시각 기준으로 읽는다
     */
    suspend fun load(place: PlaceUi, nowHour: Int): PlaceDetailUi = withFreshSession { read(place, nowHour) }

    private suspend fun read(place: PlaceUi, nowHour: Int): PlaceDetailUi = coroutineScope {
        val slot = place.slot
        val hour = slot?.hour ?: nowHour
        val today = LocalDate.now(SEOUL)
        val dayType = slot?.groupLabel ?: if (today.dayOfWeek.value >= 6) "주말" else "평일"

        val forecast = async {
            val columns = Columns.list(
                "hour", "status", "level", "is_estimated", "total_score", "main_factor", "measurement_count",
                "area_crowd_level", "score_crowd", "score_loudness", "score_sound", "score_light",
            )
            if (slot == null) {
                supabase.from("hourly_forecast")
                    .select(columns) { filter { eq("place_id", place.id); eq("date", today.toString()) } }
                    .decodeList<ForecastRow>()
            } else {
                supabase.from("slot_forecast")
                    .select(columns) { filter { eq("place_id", place.id); eq("day_group", slot.dayGroup) } }
                    .decodeList<ForecastRow>()
            }
        }
        val placeRow = async {
            supabase.from("places")
                .select(Columns.list("id", "congestion_area_cd")) { filter { eq("id", place.id) } }
                .decodeSingle<PlaceRow>()
        }
        val calmHours = async {
            supabase.from("business_calm_hours")
                .select(Columns.list("weekday", "start_time", "end_time", "actions", "custom_action")) {
                    filter { eq("place_id", place.id) }
                    order("weekday", Order.ASCENDING)
                }
                .decodeList<CalmHourRow>()
        }
        // 요약 뷰(024)가 아직 없을 수 있다. 그때는 빈 값으로 두고 나머지 탭은 그린다
        val sounds = async {
            runCatching {
                supabase.from("place_sound_summary")
                    .select(Columns.list("sound_type", "ratio", "measurement_count")) { filter { eq("place_id", place.id) } }
                    .decodeList<SoundSummaryRow>()
            }.onFailure { Log.w(TAG, "장소 상세 일부 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }
        }
        val measure = async {
            runCatching {
                supabase.from("place_measure_summary")
                    .select(
                        Columns.list("measurement_count", "db_median", "lux_median", "lux_count", "sudden_per_hour", "announce_per_hour", "crowd_median"),
                    ) { filter { eq("place_id", place.id) } }
                    .decodeSingleOrNull<MeasureSummaryRow>()
            }.onFailure { Log.w(TAG, "장소 상세 일부 읽기 실패: ${it.brief()}") }.getOrNull()
        }

        // 지역 이름 · 상권은 장소의 지역 코드를 알아야 읽을 수 있다. 1km 안에 지역이 없는 장소(027)는 비어 있다
        val areaCd = placeRow.await().congestion_area_cd
        val area = async {
            if (areaCd == null) return@async null
            runCatching {
                supabase.from("congestion_areas")
                    .select(Columns.list("area_cd", "area_nm")) { filter { eq("area_cd", areaCd) } }
                    .decodeSingleOrNull<AreaRow>()
            }.onFailure { Log.w(TAG, "장소 상세 일부 읽기 실패: ${it.brief()}") }.getOrNull()
        }
        val commercial = async {
            if (areaCd == null) return@async null
            runCatching {
                supabase.from("commercial")
                    .select(Columns.list("id", "level", "level_text")) {
                        filter { eq("area_cd", areaCd) }
                        order("base_at", Order.DESCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<CommercialRow>()
            }.onFailure { Log.w(TAG, "장소 상세 일부 읽기 실패: ${it.brief()}") }.getOrNull()
        }
        val commercialRow = commercial.await()
        val categories = if (commercialRow == null) emptyList() else runCatching {
            supabase.from("commercial_categories")
                .select(Columns.list("mid_ctgr", "level", "level_text")) { filter { eq("commercial_id", commercialRow.id) } }
                .decodeList<CommercialCategoryRow>()
        }.onFailure { Log.w(TAG, "장소 상세 일부 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }

        val rows = forecast.await()
        val m = measure.await()
        val soundRows = sounds.await()
        val areaName = area.await()?.area_nm?.substringBefore("(")?.trim() ?: "주변 지역"

        PlaceDetailUi(
            score = scoreOf(rows, hour, dayType, m, place, areaName, commercialRow, categories),
            sounds = soundRowsOf(soundRows, m),
            fixedSounds = fixedSoundRowsOf(soundRows, m),
            measureCount = m?.measurement_count ?: soundRows.maxOfOrNull { it.measurement_count },
            light = lightOf(rows, m),
            calmHours = calmHoursOf(calmHours.await()),
        )
    }

    // ── 점수 탭 ────────────────────────────────────────────

    /** 지금 시각 칸. 정보 부족이면 가장 가까운 점수 있는 시각 (지도 마커와 같은 규칙) */
    private fun nowRow(rows: List<ForecastRow>, nowHour: Int): ForecastRow? {
        val scored = rows.filter { it.status != "insufficient" && it.total_score != null }
        return scored.firstOrNull { it.hour == nowHour } ?: scored.minByOrNull { abs(it.hour - nowHour) }
    }

    private fun scoreOf(
        rows: List<ForecastRow>,
        nowHour: Int,
        /** "주말" · "평일" */
        dayType: String,
        m: MeasureSummaryRow?,
        place: PlaceUi,
        areaName: String,
        commercial: CommercialRow?,
        categories: List<CommercialCategoryRow>,
    ): ScoreBreakdown? {
        val r = nowRow(rows, nowHour) ?: return null
        val main = r.main_factor
        val crowdNote = m?.crowd_median?.let { "측정한 분들 답 · ${crowdWord(it)}" }

        // 업종 상권이 있으면 그것, 없으면(병원 · 백화점) 지역 전체 상권
        val mids = COMMERCIAL_CATEGORY_MAP[place.category].orEmpty()
        val categoryRow = categories.firstOrNull { it.mid_ctgr in mids }
        val commercialName = if (categoryRow != null) "$areaName ${place.category} 상권" else "$areaName 상권"
        val commercialCrowd = categoryRow?.level_text ?: commercial?.level_text ?: "정보 없음"

        return ScoreBreakdown(
            hour = r.hour,
            total = r.total_score!!.roundToInt(),
            factors = listOf(
                ScoreFactor("소리 종류 · 돌발", r.score_sound?.roundToInt(), isMain = main == "sound"),
                ScoreFactor("소리 크기", r.score_loudness?.roundToInt(), isMain = main == "loudness"),
                ScoreFactor("장소 혼잡", r.score_crowd?.roundToInt(), isMain = main == "crowd", note = crowdNote),
                ScoreFactor("밝기", r.score_light?.roundToInt(), isMain = main == "light"),
            ),
            mainFactor = mainFactorNote(main, m),
            basis = "$dayType ${r.hour}시 칸 · 실측 ${r.measurement_count}회" + if (r.is_estimated) " · 추정" else "",
            areaCrowd = r.area_crowd_level?.let { AREA_WORDS.getOrNull(it - 1) } ?: "정보 없음",
            commercialCrowd = commercialCrowd,
            areaName = "$areaName 일대",
            commercialName = commercialName,
        )
    }

    /** '주 요인' 상자 한 줄. 왜 그 항목이 가장 높았는지를 실제 값으로 */
    // 값을 모르면 빈 문자열 → 화면에서 상자를 그리지 않는다. 무던한 곳에 "조명이 밝아요" 처럼 틀린 말을 붙이지 않으려는 것
    private fun mainFactorNote(main: String?, m: MeasureSummaryRow?): String = when (main) {
        "sound" -> m?.sudden_per_hour?.takeIf { it > 0 }?.let { "돌발 소리가 1시간에 ${fmt(it)}회" }
        "loudness" -> m?.db_median?.let { "평균 소리 크기 ${it.roundToInt()} dB" }
        "light" -> m?.lux_median?.let { "밝기 ${it.roundToInt()} lux" }
        "crowd" -> m?.crowd_median?.let { "측정한 분들 답 · ${crowdWord(it)}" }
        else -> null
    } ?: ""

    // ── 소리 탭 ────────────────────────────────────────────

    private fun soundRowsOf(rows: List<SoundSummaryRow>, m: MeasureSummaryRow?): List<SoundRow> {
        if (rows.isEmpty() && m == null) return emptyList()
        val ratio = rows.associate { it.sound_type to it.ratio }
        val sudden = m?.sudden_per_hour ?: 0.0
        val announce = m?.announce_per_hour ?: 0.0
        return buildList {
            add(SoundRow("사람 말소리", freqWord(ratio["speech"])))
            if (sudden > 0) {
                add(
                    SoundRow(
                        label = "돌발 소리",
                        value = "${fmt(sudden)}회 / 1시간",
                        emphasized = true,
                        subLabel = "알림음 · 부딪힘 · 경보처럼 갑자기 커지는 소리예요",
                        subValue = if (announce > 0) "안내방송 추정 ${fmt(announce)}회" else null,
                    ),
                )
            }
            add(SoundRow("배경 음악", freqWord(ratio["music"])))
            add(SoundRow("기계음", freqWord(ratio["machine"])))
            add(SoundRow("경보 · 큰 소리", freqWord(maxOf(ratio["alarm"] ?: 0.0, ratio["loud_voice"] ?: 0.0))))
        }
    }

    /**
     * 정보 부족 화면 소리 탭. 답변서2 질의1 이 장소 고유값으로 둔 것만 — 안내방송 빈도 · 배경 음악.
     * 말소리 · 돌발 소리는 붐비는 정도를 따라 시간마다 달라져서 그 시간대 기록(장소 상세)에서만 보여준다
     */
    private fun fixedSoundRowsOf(rows: List<SoundSummaryRow>, m: MeasureSummaryRow?): List<SoundRow> {
        if (rows.isEmpty() && m == null) return emptyList()
        val announce = m?.announce_per_hour ?: 0.0
        return listOf(
            if (announce > 0) {
                SoundRow("안내방송", "${fmt(announce)}회 / 1시간", emphasized = true, subLabel = "안내 앞 알림음과 말소리로 추정했어요")
            } else {
                SoundRow("안내방송", "없음")
            },
            SoundRow("배경 음악", freqWord(rows.firstOrNull { it.sound_type == "music" }?.ratio)),
        )
    }

    /** 비율 → 말. 소리 탭의 빈도 막대(FrequencyMeter)가 이 말을 다시 0~4 로 읽는다 */
    private fun freqWord(ratio: Double?): String = when {
        ratio == null || ratio <= 0.0 -> "없음"
        ratio >= 0.6 -> "거의 항상"
        ratio >= 0.3 -> "자주"
        ratio >= 0.1 -> "가끔"
        else -> "드묾"
    }

    // ── 빛 탭 ──────────────────────────────────────────────

    private fun lightOf(rows: List<ForecastRow>, m: MeasureSummaryRow?): LightInfo? {
        val lux = m?.lux_median ?: return null
        // 밝기는 장소 고유값이라 batch 가 모든 칸(평일 · 주말 · 빈 칸까지)에 같은 값을 넣는다
        val score = rows.firstNotNullOfOrNull { it.score_light }
        return LightInfo(
            lux = lux.roundToInt(),
            luxLevel = when {
                score == null -> SensoryLevel.NO_DATA
                score < 40f -> SensoryLevel.CALM
                score < 70f -> SensoryLevel.MID
                else -> SensoryLevel.HIGH
            },
            // 깜빡임은 폰으로 잴 수 없어서 늘 미측정이다
            flicker = "미측정",
            flickerMeasured = false,
            note = "미측정은 '깜빡임 없음'이 아니에요. 깜빡임에 민감한 아이라면 확인 전까지 '무던함'으로 추천하지 않아요.",
        )
    }

    // ── 사업장 탭 ──────────────────────────────────────────

    /** 같은 시간 · 같은 조치는 요일을 묶는다. "화 · 목 10~12시" */
    private fun calmHoursOf(rows: List<CalmHourRow>): CalmHours? {
        if (rows.isEmpty()) return null
        val groups = rows.groupBy { Triple(it.start_time, it.end_time, it.actions.orEmpty() + listOfNotNull(it.custom_action)) }
        val hours = groups.entries.joinToString("\n") { (key, list) ->
            val days = list.map { it.weekday }.distinct().sorted().joinToString(" · ") { WEEKDAYS[it - 1] }
            "$days ${clock(key.first)}~${clock(key.second)}시"
        }
        val actions = rows.flatMap { it.actions.orEmpty().map { a -> ACTION_WORDS[a] ?: a } + listOfNotNull(it.custom_action) }
            .distinct()
        return CalmHours(
            businessHours = hours,
            businessActions = actions.joinToString(" · ").ifBlank { null },
        )
    }

    /** "10:00:00" → "10", "10:30:00" → "10:30" */
    private fun clock(time: String): String {
        val (h, min) = time.split(":")
        return if (min == "00") h.trimStart('0').ifEmpty { "0" } else "${h.trimStart('0').ifEmpty { "0" }}:$min"
    }

    // ── 말 바꾸기 ──────────────────────────────────────────

    private fun crowdWord(level: Int): String = CROWD_WORDS.getOrElse(level) { "잘 모름" }

    private fun fmt(x: Double): String = if (x == x.roundToInt().toDouble()) "${x.roundToInt()}" else "%.1f".format(x)

    private val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")
    private val AREA_WORDS = listOf("여유", "보통", "약간 붐빔", "붐빔")
    private val CROWD_WORDS = listOf("한가함", "보통", "붐빔", "매우 붐빔")
    private val ACTION_WORDS = mapOf("music_off" to "음악 끔", "light_down" to "조명 낮춤", "announcement_min" to "방송 최소")

    /** places.category → 서울시 상권 중분류. settings.commercial_category_map(015) 과 같은 표. settings 는 로그인해야 읽혀서 여기 둔다 */
    private val COMMERCIAL_CATEGORY_MAP = mapOf(
        "마트" to listOf("할인점/슈퍼마켓"),
        "카페" to listOf("제과/커피/패스트푸드"),
        "식당" to listOf("한식", "일식/중식/양식", "기타요식"),
        "미용실" to listOf("미용서비스"),
    )
}
