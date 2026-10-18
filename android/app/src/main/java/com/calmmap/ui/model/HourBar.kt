package com.calmmap.ui.model

import com.calmmap.ui.theme.SensoryLevel

/**
 * 시간대별 그래프의 막대 하나.
 *
 * @param hour      24시간제 시각
 * @param level     그 시간의 단계. null 이면 그 시간은 아무 정보도 없다
 * @param ratio     막대 높이 비율 0~1
 * @param estimated 보간으로 채운 값. 실측이 아니다
 *
 * DB 를 붙이면 hourly_forecast 한 줄이 여기 한 칸이 된다.
 *   total_score 가 비었으면   level = null
 *   is_estimated 가 true 면   estimated = true
 *   ratio 는 total_score / 100
 */
data class HourBar(
    val hour: Int,
    val level: SensoryLevel?,
    val ratio: Float,
    val estimated: Boolean = false,
)
