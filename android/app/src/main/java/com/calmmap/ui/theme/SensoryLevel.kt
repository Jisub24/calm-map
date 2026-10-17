package com.calmmap.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 장소에 붙는 감각부하 표시. 지도 마커 · 목록 배지 · 그래프 막대가 모두 이것을 쓴다.
 *
 * 앞의 세 개(무던함 · 보통 · 부담 높음)는 채워서 그리고,
 * 뒤의 두 개(후보 · 정보 부족)는 채우지 않는다.
 * 확인된 값이 아니라는 것을 '비어 있음'으로 보여주기 위해서다.
 *
 * 나중에 DB 와 이을 때는 hourly_forecast 의 level · is_estimated · total_score 를
 * 여기로 옮겨 담는다.
 *   total_score 가 비었으면          NO_DATA
 *   is_estimated 가 true 면          CANDIDATE
 *   아니면 level 그대로              CALM / MID / HIGH
 */
enum class SensoryLevel(
    val label: String,
    /** 채울 색. null 이면 채우지 않고 테두리로만 그린다 */
    val fill: Color?,
    /** 채운 위에 올리는 글자색 */
    val onFill: Color,
    /** 테두리를 점선으로 그릴지 */
    val dashed: Boolean,
) {
    CALM("무던함", LevelCalm, Ink, false),
    MID("보통", LevelMid, Ink, false),
    HIGH("부담 높음", LevelHigh, Paper, false),
    CANDIDATE("후보", null, Ink, false),
    NO_DATA("정보 부족", null, InkSoft, true),
}
