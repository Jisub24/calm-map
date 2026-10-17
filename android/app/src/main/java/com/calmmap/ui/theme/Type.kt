package com.calmmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 와이어프레임에서 쓴 글자 크기를 Material3 자리에 맞춰 넣은 것이다.
 *
 * 폰트 파일을 따로 넣지 않고 기기 기본 글꼴을 쓴다.
 * 안드로이드의 한글 기본 글꼴이 본고딕 계열이라 와이어프레임(Noto Sans KR)과
 * 사실상 같은 모양이고, APK 에 폰트를 넣으면 용량만 수 MB 늘어난다.
 *
 * 쓰는 자리
 *   titleLarge   18  장소 이름 (하단바)
 *   titleMedium  17  화면 제목, 권고 문구
 *   titleSmall   15  섹션 제목, 버튼
 *   bodyLarge    14  목록의 장소 이름
 *   bodyMedium   13  칩, 검색창, 항목 이름
 *   bodySmall    12  설명
 *   labelMedium  11  배지, 목록의 보조 설명
 *   labelSmall   10  그래프 시간 축, 범례
 */
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)
