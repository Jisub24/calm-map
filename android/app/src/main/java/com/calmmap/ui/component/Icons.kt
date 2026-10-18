package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * 화면에 쓰는 아이콘을 직접 그린다.
 *
 * Material 아이콘 묶음(material-icons-extended)을 쓰지 않는 이유는 용량이다.
 * 아이콘 수천 개가 들어 있는데 우리가 쓰는 건 여섯 개뿐이고,
 * 제출물이 APK 라서 괜히 키울 이유가 없다.
 *
 * 와이어프레임 아이콘도 선 몇 개짜리라 직접 그리는 편이 모양도 더 맞는다.
 * 좌표는 와이어프레임의 24 칸 기준을 0~1 비율로 옮긴 것이다.
 */

private const val STROKE = 2f / 24f
private const val STROKE_THIN = 1.8f / 24f

private fun DrawScope.outline(width: Float) = Stroke(
    width = size.minDimension * width,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** 돋보기. 검색창과 검색 화면 */
@Composable
fun SearchIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(
            color = color,
            radius = s * 0.29f,
            center = Offset(s * 0.46f, s * 0.46f),
            style = outline(STROKE),
        )
        drawLine(
            color = color,
            start = Offset(s * 0.67f, s * 0.67f),
            end = Offset(s * 0.88f, s * 0.88f),
            strokeWidth = s * STROKE,
            cap = StrokeCap.Round,
        )
    }
}

/** 마이크. 지도 위 측정 버튼 */
@Composable
fun MicIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.375f, s * 0.125f),
            size = Size(s * 0.25f, s * 0.46f),
            cornerRadius = CornerRadius(s * 0.125f),
            style = outline(STROKE),
        )
        val cradle = Path().apply {
            moveTo(s * 0.21f, s * 0.46f)
            cubicTo(s * 0.21f, s * 0.79f, s * 0.79f, s * 0.79f, s * 0.79f, s * 0.46f)
        }
        drawPath(cradle, color, style = outline(STROKE))
        drawLine(
            color = color,
            start = Offset(s * 0.5f, s * 0.75f),
            end = Offset(s * 0.5f, s * 0.88f),
            strokeWidth = s * STROKE,
            cap = StrokeCap.Round,
        )
    }
}

/** 뒤로 가기 */
@Composable
fun BackIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawLine(
            color = color,
            start = Offset(s * 0.83f, s * 0.5f),
            end = Offset(s * 0.17f, s * 0.5f),
            strokeWidth = s * STROKE,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(s * 0.42f, s * 0.25f)
            lineTo(s * 0.17f, s * 0.5f)
            lineTo(s * 0.42f, s * 0.75f)
        }
        drawPath(head, color, style = outline(STROKE))
    }
}

/** 하단 탭 — 지도 */
@Composable
fun MapTabIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(s / 2f, s / 2f)
        drawCircle(color, radius = s * 0.33f, center = center, style = outline(STROKE_THIN))
        drawCircle(color, radius = s * 0.125f, center = center, style = outline(STROKE_THIN))
    }
}

/** 하단 탭 — 기록 */
@Composable
fun ListTabIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        listOf(0.29f, 0.5f, 0.71f).forEach { y ->
            drawLine(
                color = color,
                start = Offset(s * 0.25f, s * y),
                end = Offset(s * 0.75f, s * y),
                strokeWidth = s * STROKE_THIN,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** 하단 탭 — 내 정보 */
@Composable
fun PersonTabIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(
            color = color,
            radius = s * 0.33f,
            center = Offset(s / 2f, s / 2f),
            style = outline(STROKE_THIN),
        )
    }
}
