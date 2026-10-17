package com.calmmap.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 테두리를 그린다. 점선도 그릴 수 있다.
 *
 * Compose 기본 `Modifier.border` 로는 점선을 못 그린다.
 * 우리 화면에서 점선은 "확인되지 않은 값"을 뜻하는 중요한 표시라
 * (정보 부족 배지 · 추정 시간대 막대 · 지도의 정보 부족 마커)
 * 직접 그리는 함수를 하나 두고 돌려쓴다.
 */
/** 위쪽에만 선을 긋는다. 하단 탭 */
fun Modifier.topLine(color: Color, width: Dp = 1.dp): Modifier = this.drawBehind {
    val w = width.toPx()
    drawLine(
        color = color,
        start = Offset(0f, w / 2f),
        end = Offset(size.width, w / 2f),
        strokeWidth = w,
    )
}

/** 아래쪽에만 선을 긋는다. 목록 구분선 */
fun Modifier.bottomLine(color: Color, width: Dp = 1.dp): Modifier = this.drawBehind {
    val w = width.toPx()
    drawLine(
        color = color,
        start = Offset(0f, size.height - w / 2f),
        end = Offset(size.width, size.height - w / 2f),
        strokeWidth = w,
    )
}

fun Modifier.outlineBorder(
    color: Color,
    shape: Shape,
    width: Dp = 1.dp,
    dashed: Boolean = false,
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val stroke = Stroke(
        width = strokeWidth,
        pathEffect = if (dashed) {
            PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        } else {
            null
        },
    )

    // 선은 경로 한가운데를 기준으로 그려진다. 그대로 두면 바깥 절반이
    // clip 에 잘려 더 얇아 보이므로 선 굵기의 절반만큼 안쪽으로 당긴다.
    inset(strokeWidth / 2f) {
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> drawRect(color = color, style = stroke)
            is Outline.Rounded -> drawPath(
                path = Path().apply { addRoundRect(outline.roundRect) },
                color = color,
                style = stroke,
            )
            is Outline.Generic -> drawPath(outline.path, color = color, style = stroke)
        }
    }
}
