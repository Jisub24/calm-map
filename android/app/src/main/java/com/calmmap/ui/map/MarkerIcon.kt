package com.calmmap.ui.map

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LevelCalmEdge
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.LevelNoData
import com.calmmap.ui.theme.LevelOutline
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안 MapMarker. 흰 알약 + 단계 점 + 장소 이름.
 *
 *   기본       흰 바탕 · 점 12 · 이름 12 Bold
 *   후보       이름 뒤에 " · 후보", 점은 점선 링
 *   정보 부족   흰 바탕 + 회색 테두리 · 이름 회색 · " · 정보 부족"
 *   선택       진한 바탕 · 이름 13 흰색 · 조금 더 크게
 *
 * 네이버 지도 마커는 Compose 로 그릴 수 없고 비트맵을 받는다. 그래서 여기서 직접 그린다.
 * 점 모양은 component/LevelGlyph(Dot) 와 같은 규칙이다.
 * 장소마다 이름이 달라서 장소별로 한 장씩 만든다 (MapScreen 에서 remember).
 */
fun markerBitmap(
    level: SensoryLevel,
    name: String,
    density: Density,
    selected: Boolean = false,
    // 앱 글꼴(Noto Sans KR). 못 읽으면 기본 굵은 글꼴
    typeface: Typeface = Typeface.DEFAULT_BOLD,
): Bitmap = with(density) {
    val label = when (level) {
        SensoryLevel.CANDIDATE -> "$name · 후보"
        SensoryLevel.NO_DATA -> "$name · 정보 부족"
        else -> name
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        // 가변 글꼴이라 굵기를 따로 정한다
        fontVariationSettings = "'wght' 700"
        textSize = (if (selected) 13 else 12).dp.toPx()
        color = when {
            selected -> BgSurface.toArgb()
            level == SensoryLevel.NO_DATA -> TextTertiary.toArgb()
            else -> TextPrimary.toArgb()
        }
    }
    val dot = 12.dp.toPx()
    val gap = 6.dp.toPx()
    val padL = 10.dp.toPx()
    val padR = 12.dp.toPx()
    val padV = (if (selected) 8 else 6).dp.toPx()
    val textH = textPaint.fontMetrics.let { it.descent - it.ascent }
    val pillW = padL + dot + gap + textPaint.measureText(label) + padR
    val pillH = padV * 2 + maxOf(dot, textH)

    // 그림자가 잘리지 않게 사방에 여유를 둔다
    val margin = 8.dp.toPx()
    val bitmap = Bitmap.createBitmap((pillW + margin * 2).toInt(), (pillH + margin * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pill = RectF(margin, margin, margin + pillW, margin + pillH)
    val r = pillH / 2

    // 그림자 (피그마 Shadows.marker 두 겹)
    Shadows.marker.forEach { s ->
        canvas.drawRoundRect(
            RectF(pill).apply { offset(s.offset.x.toPx(), s.offset.y.toPx()) }, r, r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = s.color.toArgb()
                maskFilter = BlurMaskFilter(s.blur.toPx() / 2, BlurMaskFilter.Blur.NORMAL)
            },
        )
    }
    // 바탕
    canvas.drawRoundRect(pill, r, r, fill(if (selected) TextPrimary else BgSurface))
    if (level == SensoryLevel.NO_DATA && !selected) {
        canvas.drawRoundRect(pill, r, r, stroke(BorderStrong, 1.dp.toPx()))
    }

    // 단계 점
    val cx = pill.left + padL + dot / 2
    val cy = pill.centerY()
    val ring = 1.5.dp.toPx()
    when (level) {
        SensoryLevel.CALM -> {
            canvas.drawCircle(cx, cy, dot / 2, fill(LevelCalm))
            canvas.drawCircle(cx, cy, dot / 2 - ring / 2, stroke(LevelCalmEdge, ring))
        }
        SensoryLevel.MID -> canvas.drawCircle(cx, cy, dot / 2, fill(LevelMid))
        SensoryLevel.HIGH -> canvas.drawCircle(cx, cy, dot / 2, fill(LevelHigh))
        SensoryLevel.CANDIDATE -> canvas.drawCircle(
            cx, cy, dot / 2 - ring / 2,
            stroke(LevelOutline, ring).apply { pathEffect = DashPathEffect(floatArrayOf(ring * 1.3f, ring * 1.2f), 0f) },
        )
        SensoryLevel.NO_DATA -> canvas.drawCircle(cx, cy, dot / 2 - ring / 2, stroke(LevelNoData, ring))
    }

    // 이름 (세로 가운데)
    val baseline = cy - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2
    canvas.drawText(label, cx + dot / 2 + gap, baseline, textPaint)

    bitmap
}

private fun fill(c: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.toArgb(); style = Paint.Style.FILL }

private fun stroke(c: Color, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = c.toArgb(); style = Paint.Style.STROKE; strokeWidth = w
}
