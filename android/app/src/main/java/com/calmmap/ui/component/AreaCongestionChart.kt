package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import kotlin.math.roundToInt
import com.calmmap.ui.model.TimeSlot

/**
 * 주변 지역 혼잡 꺾은선과 그 아래 한 줄. 장소 상세(시간대별 탭)와 정보 부족 화면이 같이 쓴다.
 *   지금       오늘 실제 혼잡 + 서울시 예측(12시간 앞까지)
 *   시간 선택   지난 같은 묶음(평일 · 주말) 날들의 실제 혼잡 중앙값. 아래에 "지난 주말 N일 기록이에요"
 * 값이 하나도 없으면 빈 그래프 대신 글로 알린다.
 */
@Composable
fun AreaCongestionSection(levels: List<Int?>, markHour: Int, slot: TimeSlot?, days: Int) {
    if (levels.none { it != null }) {
        Text(
            text = if (slot == null) "주변 지역 혼잡 정보가 아직 없어요" else "아직 쌓인 ${slot.groupLabel} 혼잡 기록이 없어요",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        return
    }
    AreaCongestionChart(levels = levels, nowHour = markHour)
    if (slot != null && days > 0) {
        Text(text = "지난 ${slot.groupLabel} ${days}일 기록이에요", style = MaterialTheme.typography.caption, color = TextTertiary)
    }
}

/** 주변 지역 혼잡 ⓘ 설명. 지금은 서울시 추정, 시간 선택은 지난 실측을 모은 값 */
fun areaCongestionInfo(slot: TimeSlot?): String =
    if (slot == null) "서울시가 추정한 혼잡도예요. 매장 안 혼잡도와 다를 수 있어요."
    else "서울시가 잰 지난 ${slot.groupLabel} 혼잡을 시각마다 모은 가운데 값이에요. 매장 안 혼잡도와 다를 수 있어요."

private val congestionWords = listOf("여유", "보통", "약간 붐빔", "붐빔")

/**
 * 주변 지역 혼잡 꺾은선 (8~22시). 최종안 09.
 *
 *   - 점과 점을 직선으로 잇고 아래를 연하게 채운다
 *   - 시간마다 흰 속 · 파란 테두리 점. 지금 시각만 속을 채우고 연한 후광을 두른다
 *   - 보이는 점(지금, 또는 누른 시각)에서 축까지 점선을 내리고 위에 흰 말풍선 "12시 보통"
 *   - 누르거나 끌면 가장 가까운 시각으로 말풍선이 옮겨 간다 (막대 그래프와 같은 방식)
 */
@Composable
fun AreaCongestionChart(levels: List<Int?>, nowHour: Int) {
    val startHour = 8
    val nowIndex = (nowHour - startHour).coerceIn(0, levels.lastIndex)
    // 누른 칸. 없으면 지금 칸에 말풍선을 둔다
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = selected ?: nowIndex

    val chartHeight = 92.dp
    val tipRoom = 36.dp     // 말풍선이 제목을 가리지 않게 위에 늘 비워 두는 자리
    val padTop = 10.dp
    val padBottom = 14.dp   // 가장 낮은 점(여유)과 바닥선 사이
    val step = (chartHeight - padTop - padBottom) / 3
    fun yOf(level: Int) = padTop + step * (4 - level)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = tipRoom), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(levels) {
                        detectTapGestures { pos ->
                            selected = (pos.x / (size.width / levels.size)).toInt().coerceIn(0, levels.lastIndex)
                        }
                    }
                    .pointerInput(levels) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            selected = (change.position.x / (size.width / levels.size)).toInt().coerceIn(0, levels.lastIndex)
                        }
                    },
            ) {
                val slot = size.width / levels.size
                val baseY = size.height - 8.dp.toPx()
                fun pt(i: Int) = Offset(slot * (i + 0.5f), yOf(levels[i] ?: 1).toPx())
                // 값이 있는 시각만 점을 찍고, 값이 이어지는 구간만 선으로 잇는다 (서울시 예측은 12시간 앞까지)
                val known = levels.indices.filter { levels[it] != null }
                drawLine(BorderDefault, Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
                if (known.isNotEmpty()) {
                    val line = Path(); val area = Path()
                    var open = false
                    for (i in levels.indices) {
                        if (levels[i] == null) { open = false; continue }
                        if (!open) { line.moveTo(pt(i).x, pt(i).y); area.moveTo(pt(i).x, baseY); area.lineTo(pt(i).x, pt(i).y); open = true }
                        else { line.lineTo(pt(i).x, pt(i).y); area.lineTo(pt(i).x, pt(i).y) }
                        if (i == levels.lastIndex || levels[i + 1] == null) { area.lineTo(pt(i).x, baseY); area.close() }
                    }
                    drawPath(area, Brush.verticalGradient(listOf(BrandPrimary.copy(alpha = 0.22f), BrandPrimary.copy(alpha = 0.03f))))
                    drawPath(line, BrandPrimary, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                }

                // 보이는 점에서 바닥선까지 점선 (값이 있을 때만)
                if (levels[shown] != null) drawLine(
                    BrandPrimary, Offset(pt(shown).x, pt(shown).y), Offset(pt(shown).x, baseY),
                    strokeWidth = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                known.forEach { i ->
                    val p = pt(i)
                    when {
                        i == nowIndex -> {
                            drawCircle(BrandPrimary.copy(alpha = 0.18f), 8.dp.toPx(), p)
                            drawCircle(BrandPrimary, 3.5.dp.toPx(), p)
                            drawCircle(BgSurface, 3.5.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                        }
                        i == shown -> drawCircle(BrandPrimary, 3.5.dp.toPx(), p)
                        else -> {
                            drawCircle(BgSurface, 3.5.dp.toPx(), p)
                            drawCircle(BrandPrimary, 3.5.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                levels.indices.forEach { i ->
                    val h = startHour + i
                    Text(
                        text = if (h % 2 == 0) "$h" else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (i == nowIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == nowIndex) BrandPrimary else TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (levels[shown] != null) PointTip(
            hour = startHour + shown,
            word = congestionWords[((levels[shown] ?: 1) - 1).coerceIn(0, 3)],
            index = shown,
            slotCount = levels.size,
            pointTop = tipRoom + yOf(levels[shown] ?: 1),
            modifier = Modifier.zIndex(1f),
        )
    }
}

/** 꺾은선 점 위의 흰 말풍선 "12시 보통". 점 바로 위에 꼬리가 닿는다 */
@Composable
private fun PointTip(hour: Int, word: String, index: Int, slotCount: Int, pointTop: Dp, modifier: Modifier = Modifier) {
    var tipSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val arrow = 5.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalPx = with(density) { maxWidth.toPx() }
        val center = totalPx / slotCount * (index + 0.5f)
        val x = (center - tipSize.width / 2f).coerceIn(0f, (totalPx - tipSize.width).coerceAtLeast(0f))
        val y = with(density) { (pointTop - arrow - 10.dp).toPx() } - tipSize.height
        Column(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt().coerceAtLeast(0)) }
                .onSizeChanged { tipSize = it },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = buildAnnotatedString {
                    append("${hour}시 ")
                    withStyle(SpanStyle(color = BrandPrimary, fontWeight = FontWeight.Bold)) { append(word) }
                },
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
                modifier = Modifier
                    .calmShadow(Shadows.tooltip, Radius.tooltip)
                    .clip(Radius.tooltip)
                    .background(BgSurface)
                    .border(1.dp, BorderDefault, Radius.tooltip)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Canvas(Modifier.size(width = arrow * 2, height = arrow)) {
                val tail = Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
                }
                drawPath(tail, BgSurface)
                drawPath(tail, BorderDefault, style = Stroke(1.dp.toPx()))
            }
        }
    }
}
