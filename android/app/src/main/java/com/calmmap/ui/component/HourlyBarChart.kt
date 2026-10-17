package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.calmmap.ui.model.HourBar
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineMid
import com.calmmap.ui.theme.Paper
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
 * 막대를 누르면 그 시간의 말풍선이 뜬다.
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
) {
    val barHeight: Dp = if (compact) 54.dp else 104.dp
    val gap: Dp = if (compact) 3.dp else 4.dp
    val selectedIndex = bars.indexOfFirst { it.hour == selectedHour }.takeIf { it >= 0 }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex != null) {
            Tooltip(
                bar = bars[selectedIndex],
                index = selectedIndex,
                slotCount = bars.size,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .let { if (compact) it else it.bottomLine(Line) },
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                BarSlot(
                    bar = bar,
                    maxHeight = barHeight,
                    isSelected = bar.hour == selectedHour,
                    onClick = { onBarClick(bar) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        EvenHourAxis(bars = bars, nowHour = nowHour, gap = gap)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(2.dp)

    Column(
        modifier = modifier
            .fillMaxHeight()
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
                    .outlineBorder(
                        color = if (isSelected) Ink else Line,
                        shape = shape,
                        width = if (isSelected) 2.dp else 1.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Ink else InkMuted,
                )
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * bar.ratio.coerceIn(0.1f, 1f))
                .clip(shape)
                .let { base ->
                    if (bar.estimated) {
                        // 보간으로 채운 시간 — 채우지 않고 점선으로
                        base.outlineBorder(LineMid, shape, width = 1.5.dp, dashed = true)
                    } else {
                        base.background(level.fill ?: Paper, shape)
                            .let { if (level.fill == null) it.outlineBorder(LineMid, shape) else it }
                    }
                }
                .let { if (isSelected) it.outlineBorder(Ink, shape, width = 2.dp) else it },
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
    modifier: Modifier = Modifier,
) {
    var tipWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalPx = with(density) { maxWidth.toPx() }
        val slot = totalPx / slotCount
        // 누른 칸의 한가운데에 말풍선 가운데를 맞춘다.
        // 양 끝 칸에서는 화면 밖으로 나가므로 안쪽으로 당긴다
        val wanted = slot * (index + 0.5f) - tipWidth / 2f
        val x = wanted.coerceIn(0f, (totalPx - tipWidth).coerceAtLeast(0f))

        Column(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                .onSizeChanged { tipWidth = it.width }
                .clip(RoundedCornerShape(6.dp))
                .background(Ink)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Text(
                text = "${bar.hour}시 · ${bar.level?.label ?: "정보 없음"}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Paper,
            )
            Text(
                text = when {
                    bar.level == null -> "아직 아무 정보도 없는 시간이에요"
                    bar.estimated -> "측정된 값이 없어 추정한 값이에요"
                    else -> "직접 측정한 시간이에요"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Paper,
            )
        }
    }
}

/**
 * 시간 축. 짝수 시간만 적고, 지금 시각 아래에 '지금' 을 붙인다.
 *
 * 15칸을 다 적으면 글자가 서로 붙어 읽기 어렵다.
 * 칸 자체는 남겨두고 글자만 비워서 막대와 어긋나지 않게 한다.
 *
 * '지금' 을 막대 테두리가 아니라 축에 둔 이유:
 * 막대에 테두리를 치면 누른 막대와 생김새가 같아져 둘을 구분할 수 없다.
 */
@Composable
private fun EvenHourAxis(bars: List<HourBar>, nowHour: Int, gap: Dp) {
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNow) Ink else InkMuted,
                    textAlign = TextAlign.Center,
                )
                if (isNow) {
                    Text(
                        text = "지금",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
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
