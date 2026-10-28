package com.calmmap.ui.component

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.caption
import com.calmmap.ui.model.HourBar
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.LevelOutline
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.axis
import kotlin.math.roundToInt

/**
 * 오늘 시간대별 감각부하 막대 그래프.
 *
 * 하단바(작게)와 장소 상세(크게)가 같은 것을 쓴다.
 * 크기와 간격은 다르지만 **막대를 어떻게 그릴지 판단하는 규칙은 같아야 한다.**
 *
 *   실측      채워서 그림
 *   추정      점선 테두리, 채우지 않음
 *   정보 없음  높이가 같은 빈 막대에 물음표
 *
 * 따로 만들면 한쪽만 고치고 다른 쪽을 빠뜨린다.
 * 낮게 잘못 안내하는 오류가 거기서 나온다.
 *
 * 막대를 누르면 그 시간의 말풍선이 뜬다 (작은 그래프는 SelectedHourNote 로 대신).
 * 높이만 봐서는 몇 시인지, 실측인지 추정인지 읽을 수 없기 때문이다.
 */
@Composable
fun HourlyBarChart(
    bars: List<HourBar>,
    nowHour: Int,
    modifier: Modifier = Modifier,
    /** 하단바용 작은 그래프인지. false 면 장소 상세용 큰 그래프 */
    compact: Boolean = false,
    /** 지금 누른 시각. 말풍선이 뜨고 그 막대에 테두리가 생긴다 */
    selectedHour: Int? = null,
    onBarClick: (HourBar) -> Unit = {},
    /** 시간을 '고르는' 그래프인지 (방문 시간 선택). 말풍선 대신 고른 막대 위에 '선택' 표시 */
    pickMode: Boolean = false,
    /** 축에서 [nowHour] 아래에 붙는 글. 지도에서 시간을 골랐으면 그 시각에 '선택' */
    nowLabel: String = "지금",
) {
    val barHeight: Dp = if (compact) 54.dp else 104.dp
    val gap: Dp = if (compact) 3.dp else 4.dp
    val selectedIndex = bars.indexOfFirst { it.hour == selectedHour }.takeIf { it >= 0 }

    // 말풍선은 그래프 위에 띄운다. 자리를 차지하면 누를 때마다 카드 높이가 바뀌어 아래가 밀린다.
    // 큰 그래프는 말풍선이 제목을 가리지 않게 막대 위에 빈 자리를 늘 둔다 (최종안 09)
    val tipRoom = if (!compact && !pickMode) TooltipRoom else 0.dp
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = tipRoom)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .let { if (compact) it else it.bottomLine(BorderStrong) },
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    BarSlot(
                        bar = bar,
                        maxHeight = barHeight,
                        isSelected = bar.hour == selectedHour,
                        // 하나를 고르면 나머지를 흐리게 한다. 진한 막대는 테두리만으로는
                        // 골랐는지 안 보여서, 고른 것만 또렷하게 남기는 쪽이 분명하다
                        dimmed = selectedIndex != null && bar.hour != selectedHour && !pickMode,
                        pickMode = pickMode,
                        onClick = { onBarClick(bar) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            EvenHourAxis(bars = bars, nowHour = nowHour, nowLabel = nowLabel, gap = gap)
        }

        // 작은 그래프(장소 카드)는 위에 띄울 자리가 없어서 말풍선 대신 SelectedHourNote 를 쓴다
        if (selectedIndex != null && !pickMode && !compact) {
            Tooltip(
                bar = bars[selectedIndex],
                index = selectedIndex,
                slotCount = bars.size,
                bottom = tipRoom,
                modifier = Modifier.zIndex(1f),
            )
        }
    }
}

/**
 * 막대 한 칸.
 *
 * 칸 전체를 누를 수 있게 한다. 막대만 누르게 하면
 * 낮은 막대나 비어 있는 시간을 고를 수 없다.
 */
@Composable
private fun BarSlot(
    bar: HourBar,
    maxHeight: Dp,
    isSelected: Boolean,
    /** 다른 막대가 선택돼 있어서 이 막대를 흐리게 그릴지 */
    dimmed: Boolean,
    pickMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 최종안 그래프 막대 모서리 4
    val shape = Radius.bar

    Column(
        modifier = modifier
            .fillMaxHeight()
            .alpha(if (dimmed) 0.45f else 1f)
            .clickable(
                // 누를 때 생기는 물결 효과를 끈다. 막대가 좁아서 지저분해 보인다
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 정보가 없는 시간은 **높이가 같은 낮은 빈 막대**에 물음표만 넣는다.
        //
        // 값에 따라 높이를 다르게 하면 없는 값을 읽어낸 것처럼 보이고,
        // 아예 비워두면 누를 수 있다는 것을 알 수 없다.
        // 모든 빈 시간이 같은 높이라 "값이 아니라 자리 표시" 라는 게 드러난다.
        val level = bar.level
        if (level == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((maxHeight * 0.28f).coerceAtLeast(20.dp))
                    .clip(shape)
                    .background(BgSurface)
                    .outlineBorder(
                        color = if (isSelected) TextPrimary else BorderStrong,
                        shape = shape,
                        width = if (isSelected) 2.dp else 1.dp,
                        dashed = !isSelected,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextTertiary,
                )
            }
            return@Column
        }

        if (isSelected && pickMode) {
            Text(
                text = "선택",
                style = MaterialTheme.typography.axis,
                fontWeight = FontWeight.Bold,
                color = BgSurface,
                softWrap = false,
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .wrapContentWidth(unbounded = true)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BrandPrimary)
                    .padding(horizontal = 3.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * bar.ratio.coerceIn(0.1f, 1f))
                .clip(shape)
                .let { base ->
                    if (bar.estimated && level == SensoryLevel.HIGH) {
                        // 추정이어도 부담 높음은 알린다 (답변서2). 진한 색을 반투명으로 + 점선이라 실측 막대와는 구분된다
                        base.background(LevelHigh.copy(alpha = 0.45f), shape)
                            .outlineBorder(LevelHigh, shape, width = 1.5.dp, dashed = true)
                    } else if (bar.estimated) {
                        // 측정이 모자라거나 보간으로 채운 시간 — 채우지 않고 점선으로 (후보)
                        base.outlineBorder(LevelOutline, shape, width = 1.5.dp, dashed = true)
                    } else {
                        base.background(level.fill ?: BgSurface, shape)
                            .let { if (level.fill == null) it.outlineBorder(LevelOutline, shape) else it }
                    }
                }
                .let { if (isSelected) it.outlineBorder(if (pickMode) BrandPrimary else TextPrimary, shape, width = 2.dp) else it },
        )
    }
}

/**
 * 누른 시간의 말풍선.
 *
 * 막대 높이만 봐서는 몇 시인지, 실측인지 추정인지 알 수 없다.
 */
@Composable
private fun Tooltip(
    bar: HourBar,
    index: Int,
    slotCount: Int,
    /** 말풍선 꼬리 끝이 닿을 높이 (막대 영역 맨 위) */
    bottom: Dp,
    modifier: Modifier = Modifier,
) {
    var tipSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val arrow = 6.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalPx = with(density) { maxWidth.toPx() }
        val arrowPx = with(density) { arrow.toPx() }
        val bottomPx = with(density) { bottom.toPx() }
        val center = totalPx / slotCount * (index + 0.5f)
        // 누른 칸 한가운데에 말풍선 가운데를 맞춘다. 양 끝 칸에서는 안쪽으로 당긴다
        val x = (center - tipSize.width / 2f).coerceIn(0f, (totalPx - tipSize.width).coerceAtLeast(0f))

        // 꼬리 끝이 막대 영역 맨 위에 닿게 띄운다
        // 꼬리 줄이 말풍선 너비를 따르도록 가장 넓은 자식 너비에 맞춘다
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .offset { IntOffset(x.roundToInt(), (bottomPx - tipSize.height - 2).roundToInt()) }
                .onSizeChanged { tipSize = it },
        ) {
            Column(
                modifier = Modifier
                    .calmShadow(Shadows.tooltip, Radius.tooltip)
                    .clip(Radius.tooltip)
                    .background(TextPrimary)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${bar.hour}시 · ${bar.level?.label ?: "정보 없음"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BgSurface,
                )
                Text(
                    text = sourceLabel(bar),
                    style = MaterialTheme.typography.labelSmall,
                    color = BgSurface,
                )
            }
            // 누른 막대를 가리키는 꼬리. 말풍선이 안쪽으로 당겨져도 막대 위에 온다
            Canvas(Modifier.fillMaxWidth().height(arrow)) {
                val tipX = (center - x).coerceIn(arrowPx * 2, size.width - arrowPx * 2)
                drawPath(
                    Path().apply {
                        moveTo(tipX - arrowPx, 0f)
                        lineTo(tipX + arrowPx, 0f)
                        lineTo(tipX, size.height)
                        close()
                    },
                    TextPrimary,
                )
            }
        }
    }
}

/** 큰 그래프 막대 위 빈 자리. 말풍선 두 줄 + 꼬리가 들어가는 높이 */
private val TooltipRoom = 46.dp

/** 그 시간 값이 어디서 왔는지 */
private fun sourceLabel(bar: HourBar): String = when {
    bar.level == null -> "아직 아무 정보도 없는 시간이에요"
    bar.estimated -> "측정이 부족해 추정한 값이에요"
    else -> "직접 측정한 시간이에요"
}

/**
 * 장소 카드에서 누른 시간 한 줄. 범례 자리에 바꿔 넣어서 높이가 그대로다
 * 예) "12시 · 부담 높음 · 직접 측정한 시간이에요"
 */
@Composable
fun SelectedHourNote(bar: HourBar, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append("${bar.hour}시 · ${bar.level?.label ?: "정보 없음"}")
            }
            append(" · ${sourceLabel(bar)}")
        },
        style = MaterialTheme.typography.caption,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * 시간 축. 짝수 시간만 적고, 지금 시각 아래에 '지금' 을 붙인다 (지도에서 고른 시각이면 '선택').
 *
 * 15칸을 다 적으면 글자가 서로 붙어 읽기 어렵다.
 * 칸 자체는 남겨두고 글자만 비워서 막대와 어긋나지 않게 한다.
 *
 * '지금' 을 막대 테두리가 아니라 축에 둔 이유:
 * 막대에 테두리를 치면 누른 막대와 생김새가 같아져 둘을 구분할 수 없다.
 */
@Composable
private fun EvenHourAxis(bars: List<HourBar>, nowHour: Int, nowLabel: String, gap: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        bars.forEach { bar ->
            val isNow = bar.hour == nowHour
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (bar.hour % 2 == 0) bar.hour.toString() else "",
                    style = MaterialTheme.typography.axis,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNow) BrandPrimary else TextTertiary,
                    textAlign = TextAlign.Center,
                )
                if (isNow) {
                    Text(
                        text = nowLabel,
                        style = MaterialTheme.typography.axis,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        softWrap = false,
                        // 칸(약 20dp)보다 글자가 넓다. 칸 밖으로 넘치더라도
                        // 잘리지 않고 가운데 정렬로 보이게 한다
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                }
            }
        }
    }
}

/**
 * 그래프 아래 범례 (최종안). 직접 측정 · 추정 · 정보 부족.
 * 막대 모양만으로는 실측인지 추정인지 알 수 없어서 늘 같이 둔다.
 *
 * [bars] 에 추정인 부담 높음 막대(반투명 진한 색 + 점선)가 있으면 그 칸을 하나 더 보여준다.
 * 없는 모양을 범례에 늘 두면 헷갈려서, 그래프에 있을 때만 붙인다. 폭이 모자라면 다음 줄로 넘어간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(modifier: Modifier = Modifier, bars: List<HourBar> = emptyList()) {
    val swatch = Modifier.size(12.dp)
    val shape = RoundedCornerShape(3.dp)
    val highEstimate = bars.any { it.estimated && it.level == SensoryLevel.HIGH }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(swatch.clip(shape).background(LevelMid))
            Text(text = "직접 측정", style = MaterialTheme.typography.caption, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(swatch.outlineBorder(LevelOutline, shape, width = 1.5.dp, dashed = true))
            Text(text = "추정", style = MaterialTheme.typography.caption, color = TextSecondary)
        }
        if (highEstimate) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    swatch.clip(shape)
                        .background(LevelHigh.copy(alpha = 0.45f), shape)
                        .outlineBorder(LevelHigh, shape, width = 1.5.dp, dashed = true),
                )
                Text(text = "부담 높음 추정", style = MaterialTheme.typography.caption, color = TextSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                swatch.background(BgSurface, shape).outlineBorder(BorderStrong, shape, dashed = true),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "?", fontSize = 9.sp, color = TextTertiary)
            }
            Text(text = "정보 부족", style = MaterialTheme.typography.caption, color = TextSecondary)
        }
    }
}
