package com.calmmap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 밝은 화면 하나로 고정한다.
 *
 * 어두운 화면(다크 모드)을 넣지 않는 이유:
 *   단계를 진하기로 구분하는데, 어두운 화면에서는 진하기가 뒤집힌다.
 *   '무던함'(연함)이 어두운 바탕에서 가장 눈에 띄게 되어 반대로 읽힌다.
 *
 * 기기 배경화면 색을 따라가는 기능(dynamic color)도 끈다.
 *   켜두면 사용자 배경화면에 따라 단계 색이 제멋대로 바뀐다.
 *   보라색 폰에서는 '보통'이 보라색이 되어 설계가 통째로 무너진다.
 */
private val CalmMapColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimarySoft,
    onPrimaryContainer = BrandPrimary,
    secondary = TextSecondary,
    onSecondary = BgSurface,
    background = BgSurface,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderStrong,
    outlineVariant = BorderDefault,
)

@Composable
fun CalmMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalmMapColors,
        typography = Typography,
        content = content,
    )
}
