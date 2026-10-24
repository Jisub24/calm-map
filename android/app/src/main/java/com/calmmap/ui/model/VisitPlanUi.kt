package com.calmmap.ui.model

import com.calmmap.ui.component.CalmIcons

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 방문 시간 선택에서 고르는 날짜 칩 하나. "오늘" · "내일" · "토" · "일" */
data class VisitDay(val label: String, val date: LocalDate)

/** 오늘 · 내일 · 다가오는 토 · 일. 토 · 일이 오늘이나 내일과 겹치면 그다음 주로 */
fun upcomingVisitDays(today: LocalDate = LocalDate.now()): List<VisitDay> {
    val tomorrow = today.plusDays(1)
    fun nextAfterTomorrow(day: DayOfWeek): LocalDate {
        var date = tomorrow.plusDays(1)
        while (date.dayOfWeek != day) date = date.plusDays(1)
        return date
    }
    return listOf(
        VisitDay("오늘", today),
        VisitDay("내일", tomorrow),
        VisitDay("토", nextAfterTomorrow(DayOfWeek.SATURDAY)),
        VisitDay("일", nextAfterTomorrow(DayOfWeek.SUNDAY)),
    )
}

/** 정해 둔 방문. 지도 위 '방문 예정' 띠와 미리 보는 카드 · 현장 모드 · 방문 후 기록이 같이 쓴다 */
data class VisitPlan(
    val place: PlaceUi,
    val day: VisitDay,
    val hour: Int,
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

/** 방문 후 기록에서 고르는 '무엇이 힘들었나요' */
val visitTriggers = listOf("갑작스러운 소리", "사람 많음", "밝은 조명", "기다림", "잘 모르겠어요")
