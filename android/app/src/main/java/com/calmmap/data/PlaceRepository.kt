package com.calmmap.data

import android.util.Log
import com.calmmap.ui.model.ChildFit
import com.calmmap.ui.model.HourBar
import com.calmmap.ui.model.OtherRecord
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.TimeSlot
import com.calmmap.ui.model.clockShort
import com.calmmap.ui.model.josa
import com.calmmap.ui.model.weekdaysLabel
import com.calmmap.ui.onboarding.Sensitivity
import com.calmmap.ui.onboarding.SensoryFactor
import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 지도에 찍을 장소를 읽는다. 로그인 없이 누구나 읽을 수 있다.
 *
 *   places           이름 · 좌표 · 업종
 *   place_hours      영업시간 → "영업 중" 표시
 *   hourly_forecast  batch 가 20분마다 채우는 오늘의 시간대별 점수 → '지금' 마커 단계 · 그래프 · 결론 문구
 *   slot_forecast    날짜 없이 평일/주말 × 시각으로 낸 점수 · 주변 혼잡 → 지도 '시간 선택'
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
        val kakao_id: String? = null,
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
     * hourly_forecast · slot_forecast 한 줄 = 장소 하나의 한 시각. 두 표가 같은 칸을 쓴다.
     *   status   level(단계 확정) / estimate(후보 · 추정, 점선) / insufficient(정보 부족)
     *   level    calm / normal / high. status 가 level 일 때만 값이 있다
     */
    @Serializable
    private data class ForecastRow(
        val place_id: Long,
        /** slot_forecast 만. weekday / weekend */
        val day_group: String? = null,
        val hour: Int,
        val status: String,
        val level: String? = null,
        val is_estimated: Boolean = false,
        val total_score: Float? = null,
        val main_factor: String? = null,
        val measurement_count: Int = 0,
        /** 주변 지역 혼잡 1(여유)~4(붐빔). 서울시 예측이 12시간 앞까지라 없는 시각도 있다 */
        val area_crowd_level: Int? = null,
        /** slot_forecast 만. area_crowd_level 을 낸 날 수 */
        val area_days: Int = 0,
        /** 하위 점수 0~100. 우리 아이 맞춤이 고른 자극별로 본다. 밝기는 장소 값이라 모든 칸이 같다 */
        val score_crowd: Float? = null,
        val score_loudness: Float? = null,
        val score_light: Float? = null,
    )

    /** 사업장 웹에서 등록한 무던한 시간 (business_calm_hours). 시각은 "10:00:00" */
    @Serializable
    private data class CalmRow(
        val place_id: Long,
        val weekday: Int,
        val start_time: String,
        val end_time: String,
        val actions: List<String>? = null,
        val custom_action: String? = null,
    )

    /** place_sound_summary 한 줄. heard_count 는 028 부터 있다 */
    @Serializable
    private data class SoundRow(val place_id: Long, val sound_type: String, val ratio: Double = 0.0, val heard_count: Int? = null)

    /** place_measure_summary 한 줄. *_heard_count 는 028 부터 있다 */
    @Serializable
    private data class MeasureRow(
        val place_id: Long,
        val sudden_per_hour: Double? = null,
        val announce_per_hour: Double? = null,
        val sudden_heard_count: Int? = null,
        val announce_heard_count: Int? = null,
    )

    /**
     * 지도가 쓰는 장소 목록. 한 번 읽어 두고, 시간을 고르거나 우리 아이 설정이 바뀔 때마다 DB 를 다시 읽지 않고 여기서 만든다.
     * 같은 때 · 같은 설정이면 만들어 둔 목록을 그대로 준다. 목록이 같아야 지도가 마커를 다시 그리지 않는다
     */
    class MapPlaces internal constructor(private val make: (TimeSlot?, Map<SensoryFactor, Sensitivity>) -> List<PlaceUi>) {
        private val made = mutableMapOf<Pair<TimeSlot?, Map<SensoryFactor, Sensitivity>>, List<PlaceUi>>()

        /** 고른 때 기준. null 이면 지금. [child] 는 우리 아이 설정. 주면 장소마다 맞춤 값(PlaceUi.fit)을 같이 만든다 */
        fun at(slot: TimeSlot?, child: Map<SensoryFactor, Sensitivity> = emptyMap()): List<PlaceUi> =
            made.getOrPut(slot to child) { make(slot, child) }
    }

    /** 모든 장소를 지도용으로 읽는다. 로그인 토큰이 만료됐으면 새로 받고 읽는다 */
    suspend fun loadAll(now: ZonedDateTime = ZonedDateTime.now(SEOUL)): MapPlaces = withFreshSession { read(now) }

    private suspend fun read(now: ZonedDateTime): MapPlaces = coroutineScope {
        val today = now.toLocalDate().toString()
        // 네 표를 동시에 읽는다
        val places = async {
            supabase.from("places")
                .select(Columns.list("id", "name", "category", "lat", "lng", "kakao_id"))
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
                        "place_id", "date", "hour", "status", "level", "is_estimated",
                        "total_score", "main_factor", "measurement_count", "area_crowd_level",
                        "score_crowd", "score_loudness", "score_light",
                    ),
                ) { filter { eq("date", today) } }
                .decodeList<ForecastRow>()
        }
        // 시간 선택용. 못 읽어도(026 실행 전 등) '지금' 지도는 뜨게 빈 목록으로 둔다
        val slots = async {
            runCatching {
                supabase.from("slot_forecast")
                    .select(
                        Columns.list(
                            "place_id", "day_group", "hour", "status", "level", "is_estimated",
                            "total_score", "main_factor", "measurement_count", "area_crowd_level", "area_days",
                            "score_crowd", "score_loudness", "score_light",
                        ),
                    )
                    .decodeList<ForecastRow>()
            }.onFailure { Log.w("CalmMap", "시간 선택 값 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }
        }
        // 우리 아이 맞춤에 쓰는 장소 고유값. 못 읽어도 지도는 뜨고, 맞춤은 혼잡 · 소리 크기 · 밝기로만 본다
        val sounds = async {
            runCatching {
                supabase.from("place_sound_summary")
                    .select(Columns.list("place_id", "sound_type", "ratio", "heard_count"))
                    .decodeList<SoundRow>()
            }.recoverCatching {
                // 028 전이면 heard_count 가 없다. 중앙값 비율로 대신 본다
                supabase.from("place_sound_summary")
                    .select(Columns.list("place_id", "sound_type", "ratio"))
                    .decodeList<SoundRow>()
            }.onFailure { Log.w("CalmMap", "소리 요약 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }
        }
        val measures = async {
            runCatching {
                supabase.from("place_measure_summary")
                    .select(Columns.list("place_id", "sudden_per_hour", "announce_per_hour", "sudden_heard_count", "announce_heard_count"))
                    .decodeList<MeasureRow>()
            }.recoverCatching {
                // 028 전이면 *_heard_count 가 없다. 중앙값으로 대신 본다
                supabase.from("place_measure_summary")
                    .select(Columns.list("place_id", "sudden_per_hour", "announce_per_hour"))
                    .decodeList<MeasureRow>()
            }.onFailure { Log.w("CalmMap", "측정 요약 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }
        }
        // 사업장이 등록한 무던한 시간. 못 읽어도 지도는 뜨고 표시만 빠진다
        val calm = async {
            runCatching {
                supabase.from("business_calm_hours")
                    .select(Columns.list("place_id", "weekday", "start_time", "end_time", "actions", "custom_action"))
                    .decodeList<CalmRow>()
            }.onFailure { Log.w("CalmMap", "사업장 무던한 시간 읽기 실패: ${it.brief()}") }.getOrElse { emptyList() }
        }
        val placeRows = places.await()
        val hoursByPlace = hours.await().groupBy { it.place_id }
        val calmByPlace = calm.await().groupBy { it.place_id }
        val todayByPlace = forecast.await().groupBy { it.place_id }
        // 평일/주말 → 장소 → 그 묶음 줄들
        val slotsByGroup = slots.await().groupBy { it.day_group }.mapValues { (_, rows) -> rows.groupBy { it.place_id } }
        val factsByPlace = factsOf(sounds.await(), measures.await())

        MapPlaces { slot, child ->
            val byPlace = if (slot == null) todayByPlace else slotsByGroup[slot.dayGroup].orEmpty()
            // 지금 보는 묶음의 반대쪽. 이쪽에 측정이 없으면 "주말 기록 보기" 로 안내한다
            val otherWeekend = !(slot?.weekend ?: isWeekend(now.toLocalDate()))
            val otherByPlace = slotsByGroup[if (otherWeekend) "weekend" else "weekday"].orEmpty()
            placeRows.map { row ->
                placeUi(
                    row, byPlace[row.id].orEmpty(), hoursByPlace[row.id], now, slot, otherByPlace[row.id].orEmpty(), otherWeekend,
                    child, factsByPlace[row.id], calmByPlace[row.id].orEmpty(),
                )
            }
        }
    }

    fun isWeekend(day: LocalDate): Boolean = day.dayOfWeek.value >= 6

    /**
     * 장소 하나를 화면 값으로 바꾼다.
     *   지금([slot] 이 null)   오늘 줄. 지금 시각 칸만. 비어 있으면 정보 부족
     *   고른 때               slot_forecast 줄. 그 시각 칸만. 비어 있으면 정보 부족 (다른 시각 값을 그 시각처럼 보이지 않게)
     */
    private fun placeUi(
        row: PlaceRow,
        /** 그때(오늘 · 고른 묶음)의 점수 줄. 영업하지 않는 시각 줄은 아래에서 뺀다 */
        allRows: List<ForecastRow>,
        hours: List<HoursRow>?,
        now: ZonedDateTime,
        slot: TimeSlot?,
        /** 반대쪽 묶음(평일 ↔ 주말)의 slot_forecast 줄 */
        other: List<ForecastRow>,
        otherWeekend: Boolean,
        /** 우리 아이 설정. 민감 · 매우로 고른 자극이 있으면 맞춤 값(fit)을 같이 만든다 */
        child: Map<SensoryFactor, Sensitivity>,
        facts: PlaceFacts?,
        calm: List<CalmRow>,
    ): PlaceUi {
        // 사업장이 영업시간을 넣으면 batch 가 다시 돌기 전에도 그 시각은 그래프에서 '영업 안 함' 으로 뺀다
        val closed = closedHours(hours, now, slot)
        val rows = allRows.filter { it.hour !in closed }
        val byHour = rows.associateBy { it.hour }
        val known = rows.any { it.status != "insufficient" }
        val otherRecord = otherRecordOf(known, other, otherWeekend, slot?.hour ?: now.hour)
        // 그 시각 칸만 본다. 비어 있으면 정보 부족. 다른 시각 값을 끌어오면 마커에 몇 시 값인지 안 보여서
        // 지금 잰 것처럼 보이고, 문 닫은 시각에도 앞 시각 단계가 뜬다
        val pickedRow = byHour[slot?.hour ?: now.hour]
        val level = levelOf(pickedRow)
        val measured = rows.sumOf { it.measurement_count }
        // 영업 중인지도 그때 기준으로. 고른 때는 그 묶음의 가장 가까운 날 그 시각 30분으로 본다 (화면에 날짜는 안 나온다)
        val openAt = if (slot == null) now.toLocalDateTime() else nextDayOf(now.toLocalDate(), slot.weekend).atTime(slot.hour, 30)
        val describeAs = { lv: SensoryLevel ->
            describe(
                level = lv,
                measured = measured,
                estimated = pickedRow?.is_estimated == true,
                at = slot?.let { "${it.hour}시" } ?: "지금",
                known = known,
                other = otherRecord,
            )
        }
        val dayWord = when {
            slot == null -> "오늘은"
            slot.weekend -> "주말에는"
            else -> "평일에는"
        }
        // 영업시간을 아는데 그때 하루 종일 닫혀 있으면 정보 부족이 아니라 쉬는 날
        val restDay = closed.containsAll(CHART_HOURS.toList())
        // 무던함에서 뺄 시각을 받아 결론 문구를 만든다. 맞춤이 아니면 뺄 시각이 없다
        val recommendWithout = { blocked: Set<Int> ->
            when {
                restDay -> "$dayWord 쉬는 날이에요"
                // 이쪽 묶음엔 측정이 없고 반대쪽에만 있으면 그걸 먼저 알린다 (지도 카드 · 정보 부족 화면)
                otherRecord != null -> "${otherRecord.emptyGroupLabel}에 잰 기록은 아직 없어요"
                else -> recommend(
                    byHour = byHour,
                    nowHour = if (slot == null) now.hour else null,
                    dayWord = dayWord,
                    blocked = blocked,
                )
            }
        }
        val bars = if (rows.isEmpty()) emptyList() else CHART_HOURS.map { if (it in closed) HourBar(it, null, 0f, closed = true) else bar(it, byHour[it]) }
        val calmNow = slot == null && calm.any { c ->
            val minute = now.hour * 60 + now.minute
            c.weekday == now.dayOfWeek.value && minute >= (minutesOf(c.start_time) ?: 0) && minute < (minutesOf(c.end_time) ?: 0)
        }
        return PlaceUi(
            id = row.id,
            name = row.name,
            category = row.category,
            position = LatLng(row.lat, row.lng),
            level = level,
            description = describeAs(level),
            walkMinutes = null,
            openNow = hours?.let { isOpen(it, openAt) },
            recommendation = recommendWithout(emptySet()),
            recommendationBasis = when {
                restDay -> "등록된 영업시간 기준이에요"
                otherRecord != null ->
                    "${otherRecord.slot.groupLabel}에는 ${otherRecord.count}번 쟀어요 · 상세 보기에서 ${otherRecord.slot.groupLabel} 기록을 볼 수 있어요"
                else -> basis(rows, measured, known)
            },
            hours = bars,
            businessNote = businessNoteOf(calm, calmNow),
            calmNow = calmNow,
            areaCrowd = if (rows.isEmpty()) emptyList() else CHART_HOURS.map { byHour[it]?.area_crowd_level },
            measureCount = measured,
            slot = slot,
            areaDays = rows.maxOfOrNull { it.area_days } ?: 0,
            kakaoId = row.kakao_id,
            otherRecord = otherRecord,
            fit = fitOf(child, byHour, rows, facts, pickedRow, level, bars, describeAs, recommendWithout),
        )
    }

    /**
     * 이쪽 묶음에 단계 · 후보가 한 칸도 없는데 반대쪽에는 잰 기록이 있으면 그 기록.
     * 홍대 5곳은 주말에만 재서, 이게 없으면 평일 내내 "정보가 없어요" 로 보였다
     */
    private fun otherRecordOf(known: Boolean, other: List<ForecastRow>, weekend: Boolean, hour: Int): OtherRecord? {
        val count = other.sumOf { it.measurement_count }
        if (known || count == 0) return null
        val measured = other.filter { it.measurement_count > 0 }
        return OtherRecord(
            slot = TimeSlot(weekend, measured.minBy { kotlin.math.abs(it.hour - hour) }.hour),
            count = count,
            summary = if (other.any { it.status == "level" }) {
                recommend(other.associateBy { it.hour }, nowHour = null, dayWord = if (weekend) "주말에는" else "평일에는")
            } else {
                null
            },
        )
    }

    /** [from] 부터 세어 평일(주말)인 첫날 */
    private fun nextDayOf(from: LocalDate, weekend: Boolean): LocalDate =
        generateSequence(from) { it.plusDays(1) }.first { isWeekend(it) == weekend }

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

    /**
     * 목록 한 줄 설명. "지금 보통 · 직접 측정 3건" · "15시는 후보예요 · 직접 측정 1건".
     * 부담 높음은 측정이 모자라 추정이어도 알린다 (답변서2). 그때는 추정이라고 같이 적는다.
     * [at] 은 "지금" 또는 고른 시각 "15시", [known] 은 그 날 점수가 한 칸이라도 있는지.
     * [other] 가 있으면(이쪽 묶음엔 측정이 없고 반대쪽엔 있으면) 정보가 없다고 하지 않고 어느 쪽에 있는지 말한다
     */
    private fun describe(level: SensoryLevel, measured: Int, estimated: Boolean, at: String, known: Boolean, other: OtherRecord?): String {
        val topic = if (at == "지금") "지금은" else "${at}는"
        return when {
            other != null -> "${other.emptyGroupLabel} 측정이 아직 없어요 · ${other.slot.groupLabel} 측정 ${other.count}건"
            level == SensoryLevel.NO_DATA && !known -> "소리 · 빛 정보가 아직 없어요"
            level == SensoryLevel.NO_DATA -> "$topic 정보가 부족해요 · 직접 측정 ${measured}건"
            level == SensoryLevel.CANDIDATE -> "$topic 후보예요 · 직접 측정 ${measured}건"
            estimated -> "$at ${level.label} · 추정 · 직접 측정 ${measured}건"
            else -> "$at ${level.label} · 직접 측정 ${measured}건"
        }
    }

    /**
     * 카드 맨 위 결론 한 줄. 점수를 줄 세우지 않고 **무던한 시간대** 를 앞세운다 (원칙 4).
     * 무던함은 실측으로 확정된 시각만 센다 (추정 시각은 '후보' 라 추천하지 않는다 — 답변서 질의 2).
     * [dayWord] 는 "오늘은" · "주말에는" · "평일에는". 오늘이 아닌 날은 [nowHour] 가 null 이라 지난 시각이 없다
     */
    private fun recommend(byHour: Map<Int, ForecastRow>, nowHour: Int?, dayWord: String, blocked: Set<Int> = emptySet()): String {
        // 칸이 있어도 전부 정보 부족이면 안내할 게 없다. 아래로 내려가면 "아직 추정만 있어요" 로 잘못 나온다
        if (byHour.values.none { it.status != "insufficient" }) return "아직 측정이 부족해서 안내하기 어려워요"
        // [blocked] 는 우리 아이 맞춤에서 무던함으로 안내하지 않는 시각
        val calm = calmHours(byHour) - blocked
        // 문 닫기 전 마지막 시각까지 무던하면 "N시 이후", 아니면 실제로 무던한 구간만 말한다
        val lastHour = byHour.keys.filter { it in CHART_HOURS }.maxOrNull()
        val next = calm.filter { it >= (nowHour ?: 0) }.minOrNull()
        val past = nowHour?.let { h -> calm.filter { it < h }.maxOrNull() }
        return when {
            next != null -> {
                val end = runEnd(calm, next)
                if (end == lastHour && end != next) "$dayWord ${next}시 이후가 무던해요" else "$dayWord ${span(next, end)}가 무던해요"
            }
            past != null -> "오늘 무던한 시간은 ${span(runStart(calm, past), past)}였어요"
            byHour.values.any { it.status == "level" } -> "$dayWord 무던한 시간이 확인되지 않았어요"
            else -> "아직 추정만 있어요"
        }
    }

    /** 무던한 시각이 이어지는 구간의 끝 · 처음 */
    private fun runEnd(calm: Set<Int>, from: Int): Int { var h = from; while (h + 1 in calm) h++; return h }
    private fun runStart(calm: Set<Int>, from: Int): Int { var h = from; while (h - 1 in calm) h--; return h }

    /** 15, 15 → "15시" · 15, 17 → "15~17시" */
    private fun span(start: Int, end: Int): String = if (start == end) "${start}시" else "$start~${end}시"

    /** 결론 아래 작은 줄. 무엇을 근거로 했는지 */
    private fun basis(rows: List<ForecastRow>, measured: Int, known: Boolean): String = when {
        rows.isEmpty() || !known -> "이 장소의 소리 · 빛 정보가 아직 없어요"
        measured == 0 -> "주변 지역 혼잡으로 추정했어요"
        else -> "직접 측정한 기록 ${measured}건을 바탕으로 예측했어요"
    }

    /** 실측으로 확정된 무던한 시각. 추정은 '후보' 라 넣지 않는다 (답변서 질의 2) */
    private fun calmHours(byHour: Map<Int, ForecastRow>): Set<Int> = CHART_HOURS.filter { h ->
        byHour[h]?.let { it.status == "level" && it.level == "calm" && !it.is_estimated } == true
    }.toSet()

    /** {14, 15, 17} → "14~15시, 17시" */
    private fun spans(hours: Set<Int>): String {
        val sorted = hours.sorted()
        val parts = mutableListOf<String>()
        var start = sorted.first()
        var prev = start
        for (h in sorted.drop(1)) {
            if (h != prev + 1) {
                parts += span(start, prev)
                start = h
            }
            prev = h
        }
        parts += span(start, prev)
        return parts.joinToString(", ")
    }

    // ── 우리 아이 맞춤 ─────────────────────────────────────

    /** 맞춤에서 장소 단위로 보는 값. 소리 종류 · 돌발 · 안내방송은 장소 고유값이다 (답변서2 질의1) */
    private class PlaceFacts(
        /** 한 번이라도 들린 소리 종류 (alarm · loud_voice · machine …) */
        val heard: Set<String>,
        /** 돌발 · 안내방송이 한 번이라도 있었는지 */
        val sudden: Boolean,
    )

    /** 보통 · 부담 높음 기준선. bar() 의 40 · 70 과 같다 */
    private const val NOTICE = 40f
    private const val HIGH = 70f

    /** 시각마다 달라지는 자극. 나머지는 장소 고유값이라 모든 시각이 같다 (답변서2 질의1) */
    private val HOURLY = setOf(SensoryFactor.Crowd, SensoryFactor.SustainedLoud)

    /**
     * '매우' 는 한 번만 관측돼도 알린다. 요약 뷰의 대표값은 측정끼리 중앙값이라 측정 절반 이상에서 나와야 0 보다 커져서,
     * 한 번이라도 나온 측정 수(*_heard_count, 028)로 본다. 028 전이면 중앙값으로 대신 본다
     */
    private fun factsOf(sounds: List<SoundRow>, measures: List<MeasureRow>): Map<Long, PlaceFacts> {
        val heard = sounds.filter { (it.heard_count ?: if (it.ratio > 0) 1 else 0) > 0 }.groupBy({ it.place_id }, { it.sound_type })
        val measureById = measures.associateBy { it.place_id }
        return (sounds.map { it.place_id } + measureById.keys).toSet().associateWith { id ->
            val m = measureById[id]
            val sudden = (m?.sudden_heard_count ?: if ((m?.sudden_per_hour ?: 0.0) > 0) 1 else 0) > 0
            val announce = (m?.announce_heard_count ?: if ((m?.announce_per_hour ?: 0.0) > 0) 1 else 0) > 0
            PlaceFacts(heard = heard[id].orEmpty().toSet(), sudden = sudden || announce)
        }
    }

    /**
     * 고른 자극이 그 칸에서 얼마나 나왔나 (0~100). 모르면 null 이고, 모르는 것은 바꾸지 않는다.
     *   사람 많음 · 계속되는 큰 소리   그 시각 혼잡 · 소리 크기 점수
     *   밝은 조명                     장소 밝기 점수
     *   소리 종류                     측정에서 한 번이라도 관측됐으면 100, 아니면 0. 측정이 없는 장소는 null
     */
    private fun signal(f: SensoryFactor, r: ForecastRow?, light: Float?, facts: PlaceFacts?): Float? = when (f) {
        SensoryFactor.Crowd -> r?.score_crowd
        SensoryFactor.SustainedLoud -> r?.score_loudness
        SensoryFactor.BrightLight -> light
        SensoryFactor.SuddenSound -> facts?.let { if (it.sudden) 100f else 0f }
        SensoryFactor.Alarm -> facts?.let { if ("alarm" in it.heard) 100f else 0f }
        SensoryFactor.LoudVoice -> facts?.let { if ("loud_voice" in it.heard) 100f else 0f }
        SensoryFactor.Machine -> facts?.let { if ("machine" in it.heard) 100f else 0f }
    }

    /**
     * 우리 아이 맞춤 값 (ChildFit 설명 참고). 민감 · 매우로 고른 자극이 없으면 null.
     *   민감 · 매우   그 자극이 보통(40) 이상인 시각은 무던함에서 뺀다
     *   매우         부담 높음(70) 이상이거나 소리 종류가 관측되면 '주의'
     */
    private fun fitOf(
        child: Map<SensoryFactor, Sensitivity>,
        byHour: Map<Int, ForecastRow>,
        rows: List<ForecastRow>,
        facts: PlaceFacts?,
        /** 마커 · 목록이 보여주는 칸 */
        pickedRow: ForecastRow?,
        level: SensoryLevel,
        bars: List<HourBar>,
        describeAs: (SensoryLevel) -> String,
        recommendWithout: (Set<Int>) -> String,
    ): ChildFit? {
        val chosen = child.filterValues { it != Sensitivity.Normal }
        if (chosen.isEmpty()) return null
        val light = rows.firstNotNullOfOrNull { it.score_light }
        fun at(f: SensoryFactor, r: ForecastRow?) = signal(f, r, light, facts) ?: 0f

        val very = chosen.filterValues { it == Sensitivity.Very }.keys
        // 자극별로 보통 이상인 시각. 이 시각들은 무던함으로 안내하지 않는다
        val notice = chosen.keys.associateWith { f -> CHART_HOURS.filter { at(f, byHour[it]) >= NOTICE }.toSet() }
        val blocked = notice.values.flatten().toSet()

        val blockedNow = chosen.keys.any { at(it, pickedRow) >= NOTICE }
        val cautionNow = very.any { at(it, pickedRow) >= HIGH }
        // 후보(추정)도 무던함 후보라서 같이 보통으로 올린다. 부담을 낮게 안내하지 않으려는 것
        val fitLevel = if (blockedNow && (level == SensoryLevel.CALM || level == SensoryLevel.CANDIDATE)) SensoryLevel.MID else level
        val fitBars = bars.map { if (it.level == SensoryLevel.CALM && it.hour in blocked) it.copy(level = SensoryLevel.MID) else it }

        val removed = bars.filter { it.level == SensoryLevel.CALM && it.hour in blocked }.map { it.hour }.toSet()
        val removedBy = chosen.keys.filter { f -> notice.getValue(f).any { it in removed } }
        val warnings = very.mapNotNull { f ->
            val name = "'매우'로 고른 ${f.label}${josa(f.label, "이", "가")}"
            val highHours = CHART_HOURS.filter { at(f, byHour[it]) >= HIGH }.toSet()
            when {
                f !in HOURLY && at(f, null) >= HIGH ->
                    if (f == SensoryFactor.BrightLight) "$name 이 장소에서 높게 나왔어요" else "$name 이 장소에서 관측됐어요"
                highHours.isNotEmpty() -> "$name ${spans(highHours)}에 높아요"
                else -> null
            }
        }
        val reason = when {
            removed.isNotEmpty() -> "우리 아이 설정의 ${removedBy.joinToString(" · ") { it.label }} 때문에 ${spans(removed)}를 무던함에서 뺐어요"
            warnings.isNotEmpty() -> "단계는 그대로 두고, 고른 자극이 관측된 것만 따로 알려요"
            rows.none { it.status != "insufficient" } && facts == null -> "아직 측정이 없어 우리 아이 기준으로 볼 수 없어요"
            else -> "고른 자극이 이 장소에서 높게 나오지 않아 바뀐 시간이 없어요"
        }
        return ChildFit(
            level = fitLevel,
            description = (if (cautionNow) "주의 · " else "") + describeAs(fitLevel),
            recommendation = recommendWithout(blocked),
            hours = fitBars,
            reason = reason,
            warning = warnings.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            cautionNow = cautionNow,
        )
    }

    // ── 영업시간 ──────────────────────────────────────────

    /** place_hours 한 줄 → 여는 시각들. batch forecast.open_hours 와 같은 규칙 (09:00~22:30 이면 9~22시) */
    private fun openHours(r: HoursRow?): Set<Int> {
        if (r == null || r.is_closed) return emptySet()
        val open = minutesOf(r.open_time) ?: return emptySet()
        val close = r.close_time ?: return emptySet()
        // 자정을 넘기거나 24시까지 하면 그날 23시까지
        val last = if (r.closes_next_day || close.startsWith("24:")) 23 else (minutesOf(close) ?: return emptySet()).let { if (it % 60 > 0) it / 60 else it / 60 - 1 }
        return (open / 60..last).toSet()
    }

    /**
     * 그래프에서 '영업 안 함' 으로 그릴 시각. 영업시간을 모르는 장소는 비운다 (batch 가 8~22시로 계산한다).
     * 지금은 오늘 요일, 고른 때는 그 묶음 요일 중 하루라도 여는 시각을 연 것으로 본다 (batch group_hours 와 같다)
     */
    private fun closedHours(hours: List<HoursRow>?, now: ZonedDateTime, slot: TimeSlot?): Set<Int> {
        if (hours.isNullOrEmpty()) return emptySet()
        val days = if (slot == null) listOf(now.dayOfWeek.value) else if (slot.weekend) listOf(6, 7) else (1..5).toList()
        val open = days.flatMap { d -> openHours(hours.find { it.weekday == d }) }.toSet()
        return CHART_HOURS.filterNot { it in open }.toSet()
    }

    /**
     * 장소 카드의 사업장 한 줄. 사업장 웹 미리보기와 같은 글자.
     *   지금이 그 시간이면  "지금 무던한 시간 운영 중"
     *   아니면              "화 · 목 10~12시 무던한 시간 운영" (여러 묶음이면 "외 N")
     */
    private fun businessNoteOf(calm: List<CalmRow>, calmNow: Boolean): String? {
        if (calm.isEmpty()) return null
        if (calmNow) return "지금 무던한 시간 운영 중"
        val groups = calm.groupBy { Triple(it.start_time, it.end_time, it.actions.orEmpty().sorted() + listOfNotNull(it.custom_action)) }
            .values
            .sortedWith(compareBy({ g -> g.minOf { it.weekday } }, { g -> minutesOf(g.first().start_time) ?: 0 }))
        val first = groups.first()
        val label = "${weekdaysLabel(first.map { it.weekday })} ${clockShort(first[0].start_time)}~${clockShort(first[0].end_time)}시 무던한 시간 운영"
        return if (groups.size > 1) "$label 외 ${groups.size - 1}" else label
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
