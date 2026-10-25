package com.calmmap.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import com.calmmap.ui.theme.ShadowSpec

/**
 * 피그마 그림자(Shadows.card · float · sheet · tooltip · marker)를 그대로 그린다.
 * Compose 기본 shadow(elevation)는 색과 번짐을 정할 수 없어서 피그마 값과 달라진다.
 *
 * background 보다 앞에 붙인다. 예) .calmShadow(Shadows.float, Radius.pill).background(…)
 */
fun Modifier.calmShadow(shadows: List<ShadowSpec>, shape: Shape): Modifier =
    shadows.fold(this) { acc, s ->
        acc.dropShadow(shape, Shadow(radius = s.blur, color = s.color, offset = s.offset))
    }
