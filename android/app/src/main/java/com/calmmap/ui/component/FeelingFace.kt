package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.Radius

/**
 * 괜찮았어요 / 힘들었어요 얼굴 (흰 원 안에 눈 두 개와 입).
 * 이모지는 기기마다 모양 · 색이 달라서 직접 그린다. 방문 후 기록 · 온보딩 3장이 같이 쓴다
 */
@Composable
fun FeelingFace(good: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    // 선 굵기는 48 기준 값을 크기에 맞춰 줄인다
    val scale = size / 48.dp
    Box(
        modifier = modifier.size(size).clip(Radius.pill).background(BgSurface).border(1.dp, BorderDefault, Radius.pill),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size / 2)) {
            val eye = 1.6.dp.toPx() * scale
            drawCircle(color, eye, Offset(this.size.width * 0.32f, this.size.height * 0.38f))
            drawCircle(color, eye, Offset(this.size.width * 0.68f, this.size.height * 0.38f))
            val w = this.size.width * 0.44f
            val top = if (good) this.size.height * 0.45f else this.size.height * 0.62f
            drawArc(
                color = color,
                startAngle = if (good) 20f else 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset((this.size.width - w) / 2, top),
                size = Size(w, w * 0.6f),
                style = Stroke(1.8.dp.toPx() * scale, cap = StrokeCap.Round),
            )
        }
    }
}
