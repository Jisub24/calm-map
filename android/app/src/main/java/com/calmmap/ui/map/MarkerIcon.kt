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
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LevelCalmEdge
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.LevelNoData
import com.calmmap.ui.theme.LevelOutline
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안 MapMarker. 흰 알약 + 단계 점 + 장소 이름.
 *
 *   기본       흰 바탕 · 점 12 · 이름 12 Bold
 *   후보       이름 뒤에 " · 후보", 점은 점선 링
 *   정보 부족   흰 바탕 + 회색 테두리 · 이름 회색 · " · 정보 부족"
 *   선택       진한 바탕 · 이름 13 흰색 · 조금 더 크게
 *   주의       이름 뒤에 '주의' 꼬리표 (우리 아이 맞춤에서 '매우' 로 고른 자극이 관측됐을 때)
 *   무던한 시간 이름 뒤에 '무던한 시간' 꼬리표 (지금이 사업장이 등록한 무던한 시간일 때)
 *
 * 네이버 지도 마커는 Compose 로 그릴 수 없고 비트맵을 받는다. 그래서 여기서 직접 그린다.
 * 점 모양은 component/LevelGlyph(Dot) 와 같은 규칙이다.
 * 장소마다 이름이 달라서 장소별로 한 장씩 만든다 (MapScreen 에서 remember).
 */
/** 마커 그림과, 그림 안에서 단계 점이 있는 위치(0~1). 이 점이 장소 좌표에 오게 anchor 로 쓴다 */
class MarkerImage(val bitmap: Bitmap, val anchorX: Float, val anchorY: Float)

fun markerBitmap(
    level: SensoryLevel,
    name: String,
    density: Density,
    selected: Boolean = false,
    // 앱 글꼴(Noto Sans KR). 못 읽으면 기본 굵은 글꼴
    typeface: Typeface = Typeface.DEFAULT_BOLD,
    // 우리 아이 맞춤에서 '매우' 로 고른 자극이 관측됐으면 이름 뒤에 '주의' 를 붙인다. 단계 점은 그대로
    caution: Boolean = false,
    // 지금이 사업장이 등록한 무던한 시간이면 이름 뒤에 '무던한 시간'. 점수와는 따로다
    calmNow: Boolean = false,
): MarkerImage = with(density) {
    // 상호 앞부분만 쓴다. "스타벅스 홍대역8번출구점" 처럼 길면 알약이 옆 마커를 덮는다.
    // 단계는 글자가 아니라 점 모양으로 읽는다 (하단 범례). 전체 이름은 카드 · 상세에 나온다
    val label = shortName(name)
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
    val ring = 1.5.dp.toPx()
    val textH = textPaint.fontMetrics.let { it.descent - it.ascent }
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        fontVariationSettings = "'wght' 700"
        textSize = 10.dp.toPx()
        color = BgSurface.toArgb()
    }
    val tagPadH = 5.dp.toPx()
    // 이름 뒤 꼬리표. 사업장 무던한 시간(파랑) → 우리 아이 맞춤 주의(갈색) 순서
    val tags = listOfNotNull(
        if (calmNow) CALM_NOW to BrandPrimary else null,
        if (caution) CAUTION to StatusCaution else null,
    )
    val tagWidths = tags.map { (text, _) -> tagPadH * 2 + tagPaint.measureText(text) }
    val tagW = tagWidths.sumOf { (gap + it).toDouble() }.toFloat()
    val pillW = padL + dot + gap + textPaint.measureText(label) + tagW + padR
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

    // 단계 점. 모든 마커가 똑같이 알약 왼쪽 끝
    val cx = pill.left + padL + dot / 2
    val cy = pill.centerY()
    drawLevelDot(canvas, level, cx, cy, dot, ring)

    // 이름 (세로 가운데), 점 오른쪽
    val baseline = cy - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2
    canvas.drawText(label, cx + dot / 2 + gap, baseline, textPaint)

    // 꼬리표. 이름 오른쪽에 차례로, 색 바탕에 흰 글자
    val tagH = 16.dp.toPx()
    val tagBase = cy - (tagPaint.fontMetrics.ascent + tagPaint.fontMetrics.descent) / 2
    var left = cx + dot / 2 + gap + textPaint.measureText(label)
    tags.forEachIndexed { i, (text, color) ->
        left += gap
        val tag = RectF(left, cy - tagH / 2, left + tagWidths[i], cy + tagH / 2)
        canvas.drawRoundRect(tag, 4.dp.toPx(), 4.dp.toPx(), fill(color))
        canvas.drawText(text, tag.left + tagPadH, tagBase, tagPaint)
        left += tagWidths[i]
    }

    MarkerImage(bitmap, cx / bitmap.width, cy / bitmap.height)
}

private const val CAUTION = "주의"
private const val CALM_NOW = "무던한 시간"

private fun drawLevelDot(canvas: Canvas, level: SensoryLevel, cx: Float, cy: Float, dot: Float, ring: Float) {
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
}

private fun fill(c: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.toArgb(); style = Paint.Style.FILL }

private fun stroke(c: Color, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = c.toArgb(); style = Paint.Style.STROKE; strokeWidth = w
}

/** 마커용 짧은 이름. 첫 띄어쓰기 앞까지, 그래도 7자를 넘으면 6자 + "…" */
fun shortName(name: String): String {
    val head = name.trim().substringBefore(' ')
    return if (head.length <= 7) head else head.take(6) + "…"
}
