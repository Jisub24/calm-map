package com.calmmap.ui.model

import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.onboarding.Sensitivity
import com.calmmap.ui.onboarding.SensoryFactor

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// '오늘' 은 서울 기준. 폰 시간대가 다르면(에뮬레이터는 UTC) 아침에 어제 날짜가 오늘로 잡힌다
private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

/** 방문 시간 선택에서 고르는 날짜 칩 하나. "오늘" · "내일" · "토" · "11/7" */
data class VisitDay(val label: String, val date: LocalDate)

/**
 * 오늘 · 내일 · 내일 다음의 가장 가까운 토 · 일. 날짜순으로 둔다.
 * 이름은 visitDayOf 와 같아서, 일주일 넘게 남은 날은 "11/7" 처럼 날짜로 나온다.
 * 금요일이면 '내일(토)' 과 다음 주 토요일이 둘 다 "토" 로 보이던 것을 막는다
 */
fun upcomingVisitDays(today: LocalDate = LocalDate.now(SEOUL)): List<VisitDay> {
    val tomorrow = today.plusDays(1)
    fun nextAfterTomorrow(day: DayOfWeek): LocalDate {
        var date = tomorrow.plusDays(1)
        while (date.dayOfWeek != day) date = date.plusDays(1)
        return date
    }
    return listOf(today, tomorrow, nextAfterTomorrow(DayOfWeek.SATURDAY), nextAfterTomorrow(DayOfWeek.SUNDAY))
        .sorted()
        .map { visitDayOf(it, today) }
}

/**
 * DB 에서 읽은 날짜를 날짜 칩과 같은 이름으로. 오늘 · 내일 · 어제, 일주일 안이면 "토", 그 밖은 "11/8"
 */
fun visitDayOf(date: LocalDate, today: LocalDate = LocalDate.now(SEOUL)): VisitDay {
    val label = when {
        date == today -> "오늘"
        date == today.plusDays(1) -> "내일"
        date == today.minusDays(1) -> "어제"
        date.isAfter(today) && date.isBefore(today.plusDays(7)) ->
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        else -> "${date.monthValue}/${date.dayOfMonth}"
    }
    return VisitDay(label, date)
}

/** 정해 둔 방문. 지도 위 '방문 예정' 띠와 미리 보는 카드 · 현장 모드 · 방문 후 기록이 같이 쓴다 */
data class VisitPlan(
    val place: PlaceUi,
    val day: VisitDay,
    val hour: Int,
    /** visits 줄 번호. 취소 · 기록할 때 이것으로 찾는다. 미리보기용은 null */
    val id: Long? = null,
) {
    /** "오늘 15:00" */
    val whenLabel: String get() = "${day.label} ${"%02d".format(hour)}:00"

    /** 방문 기록에 들어가는 형식. "10/18 (토) 15:00" */
    val recordLabel: String
        get() {
            val week = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
            return "${day.date.monthValue}/${day.date.dayOfMonth} ($week) ${"%02d".format(hour)}:00"
        }
}

/**
 * 미리 보는 카드의 한 단계.
 *
 * @param text      카드 목록에 쓰는 한 줄. "문을 밀고 들어가요"
 * @param childText 아이와 보기에서 크게 보여줄 글. 줄을 나눠 짧게 끊는다
 * @param icon      단계 그림 (아이와 보기에서는 크게)
 */
data class CardStep(val text: String, val childText: String, val icon: CalmIcons)

// TODO: AI 카드 생성을 붙이면 장소마다 만들어 받는다. 예측이라 '~할 수 있어요' 로 쓴다
val sampleCardSteps = listOf(
    CardStep("문을 밀고 들어가요", "문을 밀고\n들어가요", CalmIcons.Store),
    CardStep("안내방송이 나올 수 있어요", "안내방송이\n나올 수 있어요", CalmIcons.Volume),
    CardStep("소리가 크면 헤드폰을 써요", "소리가 크면\n헤드폰을 써요", CalmIcons.Headphones),
    CardStep("힘들면 1층 쉼터에서 쉬어요", "힘들면\n1층 쉼터에서 쉬어요", CalmIcons.Pin),
)

/** 현장 모드에 보여줄 가까운 조용한 곳 */
data class QuietSpot(val name: String, val distance: String)

// TODO: 현장 측정과 장소 정보가 붙으면 지운다
val sampleQuietSpots = listOf(
    QuietSpot("2층 수유실", "약 30m · 엘리베이터 옆"),
    QuietSpot("비상계단 앞 복도", "약 15m"),
)

/**
 * 방문 후 기록에서 고르는 '무엇이 힘들었나요'. dbValue 는 visits.hard_factors 에 들어가는 값 (011 규칙과 같게).
 * '잘 모르겠어요' 는 DB 에 넣지 않고 비워 둔다.
 * [factor] 는 우리 아이 설정의 자극. 같은 자극이 힘들었던 방문이 쌓이면 설정을 올릴지 묻는다
 */
enum class HardFactor(val label: String, val dbValue: String?, val factor: SensoryFactor?) {
    ANNOUNCEMENT("안내방송", "announcement", SensoryFactor.SuddenSound),
    SPEECH("말소리", "speech", SensoryFactor.SustainedLoud),
    MUSIC("음악", "music", SensoryFactor.SustainedLoud),
    MACHINE("기계음", "machine", SensoryFactor.Machine),
    SUDDEN("갑작스러운 소리", "sudden", SensoryFactor.SuddenSound),
    CROWD("사람 많음", "crowd", SensoryFactor.Crowd),
    UNKNOWN("잘 모르겠어요", null, null),
}

/** 방문 기록으로 우리 아이 설정을 한 단계 올리자는 제안. [count] 는 그 자극이 힘들었던 방문 수 */
data class RaiseSuggestion(val factor: SensoryFactor, val to: Sensitivity, val count: Int)

/**
 * 이번 기록에서 고른 자극([picked]) 가운데, 힘들었던 방문이 충분히 쌓인 것 하나를 한 단계 올리자고 한다.
 *   보통 → 민감   2번
 *   민감 → 매우   4번
 * 한 번 힘들었다고 바로 올리지 않는다 (아이 컨디션 · 그날 상황일 수 있다). 보호자가 '올리기' 를 눌러야 바뀐다
 */
fun raiseSuggestion(
    picked: List<HardFactor>,
    counts: Map<SensoryFactor, Int>,
    current: Map<SensoryFactor, Sensitivity>,
): RaiseSuggestion? = picked.mapNotNull { it.factor }.distinct().firstNotNullOfOrNull { f ->
    val (to, need) = when (current[f] ?: Sensitivity.Normal) {
        Sensitivity.Normal -> Sensitivity.Sensitive to 2
        Sensitivity.Sensitive -> Sensitivity.Very to 4
        Sensitivity.Very -> return@firstNotNullOfOrNull null
    }
    val count = counts[f] ?: 0
    if (count >= need) RaiseSuggestion(f, to, count) else null
}
