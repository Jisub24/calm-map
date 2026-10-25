package com.calmmap.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * 피그마 최종안 Foundations 의 간격 · 모서리 · 높이 · 그림자.
 * 화면에서 숫자를 직접 쓰지 않고 여기 이름을 쓴다.
 */

/** 간격 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40. 화면 좌우 20, 카드 안 16, 카드 사이 12 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp

    /** 화면 좌우 여백 */
    val screen = l
    /** 카드 안쪽 여백 */
    val card = m
    /** 카드와 카드 사이 */
    val cardGap = s
}

/**
 * 모서리 4 그래프 막대 · 6 작은 태그(주의) · 10 말풍선 · 12 안쪽 타일 · 아이콘 박스 · 입력
 * · 16 알림 배너 · 20 카드 · 24 바텀시트 · 큰 일러스트 · 999 버튼 · 칩 · 배지 · 마커
 */
object Radius {
    val bar = RoundedCornerShape(4.dp)
    val tag = RoundedCornerShape(6.dp)
    val tooltip = RoundedCornerShape(10.dp)
    val tile = RoundedCornerShape(12.dp)
    val banner = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val illustration = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** 높이: 버튼 L 52 · 버튼 M 44 · 검색창 48 · 세그먼트 40 · 칩 36 · 탭 44 · 배지 23 · 하단 탭 64 · 상단 바 56 */
object Height {
    val buttonL = 52.dp
    val buttonM = 44.dp
    val search = 48.dp
    val segmented = 40.dp
    val chip = 36.dp
    val tab = 44.dp
    val badge = 23.dp
    val bottomNav = 64.dp
    val topBar = 56.dp

    /** 터치 영역 최소 44. 주요 버튼은 52 */
    val touchMin = 44.dp
}

/** 그림자 한 겹 */
data class ShadowSpec(val offset: DpOffset, val blur: Dp, val color: Color)

/**
 * 그림자. 카드는 없거나 아주 옅게, 지도 위에 뜨는 것(검색창 · 마커 · 버튼)만 또렷하게.
 * 값은 피그마 Effect 스타일 그대로다 (Shadow/Card, Shadow/Float …).
 */
object Shadows {
    val card = listOf(ShadowSpec(DpOffset(0.dp, 2.dp), 8.dp, Color(0x0F1B2133)))
    val float = listOf(ShadowSpec(DpOffset(0.dp, 4.dp), 14.dp, Color(0x241B2133)))
    val sheet = listOf(ShadowSpec(DpOffset(0.dp, (-4).dp), 20.dp, Color(0x1A1B2133)))
    val tooltip = listOf(ShadowSpec(DpOffset(0.dp, 4.dp), 12.dp, Color(0x2E1A1D24)))
    val marker = listOf(
        ShadowSpec(DpOffset(0.dp, 4.dp), 12.dp, Color(0x12000000)),
        ShadowSpec(DpOffset(0.dp, 1.dp), 3.dp, Color(0x24000000)),
    )
}
