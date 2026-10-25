package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LevelCalmEdge
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.LevelNoData
import com.calmmap.ui.theme.LevelOnHigh
import com.calmmap.ui.theme.LevelOutline
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.SensoryLevel

/**
 * 단계 기호 ○ ◐ ● . 색을 못 보는 사람도 모양으로 단계를 읽게 한다.
 *
 * 두 가지 모양이 있다 (피그마 기준)
 *   Badge  배지 안 8dp — 배지 바탕이 이미 단계 색이라 기호는 바탕 위에 대비되게
 *          무던함 ○(진한 링) · 보통 ◐(오른쪽 반) · 부담 높음 ●(흰 점) · 후보 · 정보 부족 점선 링
 *   Dot    지도 마커 · 범례 12dp — 흰 바탕 위라 단계 색 그대로
 *          무던함 연한 점+테두리 · 보통 중간 점 · 부담 높음 진한 점 · 후보 점선 링 · 정보 부족 옅은 링
 */
enum class GlyphStyle { Badge, Dot }

@Composable
fun LevelGlyph(
    level: SensoryLevel,
    modifier: Modifier = Modifier,
    style: GlyphStyle = GlyphStyle.Badge,
    size: Dp = if (style == GlyphStyle.Badge) 8.dp else 12.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val ring = (if (style == GlyphStyle.Badge) 1.5.dp else 1.5.dp).toPx()
        val r = this.size.minDimension / 2f
        val dashed = Stroke(width = ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ring * 1.3f, ring * 1.2f)))
        when (style) {
            GlyphStyle.Badge -> when (level) {
                SensoryLevel.CALM -> drawCircle(LevelHigh, r - ring / 2, style = Stroke(ring))
                SensoryLevel.MID -> drawArc(TextPrimary, -90f, 180f, useCenter = true, topLeft = Offset.Zero, size = Size(r * 2, r * 2))
                SensoryLevel.HIGH -> drawCircle(LevelOnHigh, r)
                SensoryLevel.CANDIDATE -> drawCircle(LevelOutline, r - ring / 2, style = dashed)
                SensoryLevel.NO_DATA -> drawCircle(LevelNoData, r - ring / 2, style = dashed)
            }
            GlyphStyle.Dot -> when (level) {
                SensoryLevel.CALM -> {
                    drawCircle(LevelCalm, r)
                    drawCircle(LevelCalmEdge, r - ring / 2, style = Stroke(ring))
                }
                SensoryLevel.MID -> drawCircle(LevelMid, r)
                SensoryLevel.HIGH -> drawCircle(LevelHigh, r)
                SensoryLevel.CANDIDATE -> drawCircle(LevelOutline, r - ring / 2, style = dashed)
                SensoryLevel.NO_DATA -> drawCircle(LevelNoData, r - ring / 2, style = Stroke(ring))
            }
        }
    }
}
