package com.calmmap.ui.theme

import androidx.compose.material3.Typography
import com.calmmap.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 피그마 최종안 타이포그래피 (Noto Sans KR).
 *
 * 기기 기본 글꼴은 숫자 · 영문 · 「·」 를 Roboto 로 그려서 시안과 폭이 달라진다.
 * 그래서 가변 글꼴 한 파일(res/font, 약 10MB)을 넣고 굵기만 바꿔 쓴다.
 *
 * 피그마 스타일 → Material 자리
 *   Display/28 Bold    displayLarge    온보딩 · 큰 제목
 *   Number/24 Bold     displayMedium   큰 숫자 (점수, 밝기)
 *   Title/22 Bold      headlineSmall   화면 안의 큰 제목
 *   Heading/18 Bold    titleLarge      상단바 제목 · 장소 이름
 *   Subhead/16 Medium  titleMedium     섹션 제목 · 권고 문구
 *   Body/15 Medium     titleSmall      버튼 L
 *   Body/15 Regular    bodyLarge       본문 · 검색창
 *   Label/14 Medium    labelLarge      버튼 M · 칩 · 탭 · 목록 이름
 *   Body/14 Regular    bodyMedium      본문 작게
 *   Body/13 Regular    bodySmall       알림 배너 · 조치 칩
 *   Caption/12 Medium  labelMedium     배지 · 강조 캡션
 *   Micro/11 Medium    labelSmall      하단 탭 · 말풍선
 *   Caption/12 Regular Typography.caption (아래 확장)
 *   Axis/10 Regular    Typography.axis    (아래 확장)
 */
/** 가변 글꼴 하나에서 쓰는 굵기 3가지 (400 · 500 · 700) */
@OptIn(ExperimentalTextApi::class)
val NotoSansKr = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.Bold).map {
        Font(R.font.noto_sans_kr, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
    },
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
)

val Typography = Typography(
    displayLarge = style(28, 38, FontWeight.Bold, (-0.5).sp),
    displayMedium = style(24, 32, FontWeight.Bold),
    headlineSmall = style(22, 30, FontWeight.Bold, (-0.3).sp),
    titleLarge = style(18, 26, FontWeight.Bold, (-0.2).sp),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(15, 23, FontWeight.Medium),
    bodyLarge = style(15, 23, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(13, 19, FontWeight.Normal),
    labelMedium = style(12, 17, FontWeight.Medium),
    labelSmall = style(11, 14, FontWeight.Medium),
)

private val Caption = style(12, 17, FontWeight.Normal)
private val Axis = style(10, 14, FontWeight.Normal)

/** Caption/12 Regular — 목록 보조 설명, 출처 */
val Typography.caption: TextStyle get() = Caption

/** Axis/10 Regular — 그래프 시간 축, '지금' */
val Typography.axis: TextStyle get() = Axis
