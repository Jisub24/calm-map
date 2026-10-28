package com.calmmap.ui.detail

import com.calmmap.ui.theme.LevelNoData
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.ActionChip
import com.calmmap.ui.component.BottomCta
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.CalmTabs
import com.calmmap.ui.component.ChartLegend
import com.calmmap.ui.component.FrequencyMeter
import com.calmmap.ui.component.FullSegmented
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.InfoButton
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.LevelGlyph
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.PlaceDetailUi
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.ScoreBreakdown
import com.calmmap.ui.model.SoundRow
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSunken
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.LevelOutline
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.StatusCautionSoft
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.BorderStrong
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** 최종안 상세 탭 다섯 개 */
private enum class DetailTab(val label: String) {
    HOURLY("시간대별"),
    SCORE("점수"),
    SOUND("소리"),
    LIGHT("빛"),
    BUSINESS("사업장"),
}

/**
 * 최종안 09 장소 상세 (시간대별 · 점수 · 소리 · 빛 · 사업장 · 우리 아이 맞춤).
 *
 * 장소 카드(07)가 "언제 가면 되나" 를 말한다면 여기는 **"왜 그런가"** 를 말한다.
 * 점수를 보여주는 것이 목적이 아니라, 그 점수가 어디서 나왔는지를 열어 보이는 화면이다.
 *
 * 회색 바탕 위에 흰 카드: 권고 카드 → 탭 → 탭 내용 카드. 버튼은 아래에 고정.
 */
@Composable
fun PlaceDetailScreen(
    place: PlaceUi,
    detail: PlaceDetailUi?,
    nowHour: Int,
    onBack: () -> Unit,
    onMeasureClick: () -> Unit,
    onVisitClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 우리 아이 맞춤은 로그인해야 쓸 수 있다. false 면 누를 때 onLoginNeeded */
    canPersonalize: Boolean = true,
    onLoginNeeded: () -> Unit = {},
) {
    // 0 모두 기준, 1 우리 아이 맞춤
    var basis by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(DetailTab.HOURLY) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    val personalized = basis == 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        TopBar(title = place.name, onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // 빈 곳을 누르면 그래프 말풍선을 닫는다. 장소 카드와 같은 그래프라 닫는 방법도 같다
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    selectedHour = null
                }
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = place.infoLine,
                    style = MaterialTheme.typography.caption,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                LevelBadge(level = place.level, prefix = if (personalized) "우리 아이 기준 ·" else null)
            }

            FullSegmented(
                options = listOf("모두 기준", "우리 아이 맞춤"),
                selectedIndex = basis,
                onSelect = { if (it == 1 && !canPersonalize) onLoginNeeded() else basis = it },
            )

            // '매우'로 고른 자극이 관측된 곳이면 맞춤 기준에서 먼저 알린다
            detail?.warning?.takeIf { personalized }?.let { CautionBanner(text = it) }

            RecommendationCard(recommendation = place.recommendation, basis = place.recommendationBasis)

            if (detail != null) {
                CalmTabs(
                    tabs = DetailTab.entries.map { it.label },
                    selectedIndex = tab.ordinal,
                    onSelect = { tab = DetailTab.entries[it] },
                )
            }

            when {
                detail == null || tab == DetailTab.HOURLY -> HourlyCard(
                    place = place,
                    nowHour = nowHour,
                    selectedHour = selectedHour,
                    onBarClick = { selectedHour = if (selectedHour == it) null else it },
                )
                tab == DetailTab.SCORE -> ScoreCard(detail.score)
                tab == DetailTab.SOUND -> SoundCard(detail.sounds)
                tab == DetailTab.LIGHT -> LightCard(detail)
                else -> BusinessCard(detail)
            }
        }

        BottomCta {
            SecondaryButton(text = "이 장소 측정", onClick = onMeasureClick, modifier = Modifier.weight(1f))
            PrimaryButton(text = "이 시간에 방문", onClick = onVisitClick, modifier = Modifier.weight(1f))
        }
    }
}

// 탭 내용을 담는 흰 카드 (둥근 20 · 안쪽 16)
@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// 카드 제목 줄. 오른쪽에 보조 글 또는 ⓘ
@Composable
private fun CardTitle(title: String, trailing: String? = null, info: String? = null) {
    var showInfo by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            if (trailing != null) Text(text = trailing, style = MaterialTheme.typography.caption, color = TextTertiary)
            if (info != null) InfoButton(onClick = { showInfo = !showInfo }, modifier = Modifier.size(28.dp))
        }
        // ⓘ 를 누르면 제목 아래에 설명을 펼친다
        if (info != null && showInfo) {
            Text(text = info, style = MaterialTheme.typography.caption, color = TextSecondary)
        }
    }
}

/** 우리 아이 맞춤에서 '매우' 자극이 관측됐을 때 (주의 색 상자) */
@Composable
private fun CautionBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.banner)
            .background(StatusCautionSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "주의",
            style = MaterialTheme.typography.labelSmall,
            color = BgSurface,
            modifier = Modifier
                .clip(Radius.tag)
                .background(StatusCaution)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = StatusCaution)
    }
}

/**
 * 권고 카드. 시간대("14시 이후")만 파랗게 강조한다.
 * 점수가 아니라 "언제" 를 먼저 말하는 자리다.
 */
@Composable
private fun RecommendationCard(recommendation: String, basis: String) {
    val hour = Regex("\\d+(~\\d+)?시( 이후| 이전|까지|부터)?").find(recommendation)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                if (hour == null) {
                    append(recommendation)
                } else {
                    append(recommendation.substring(0, hour.range.first))
                    withStyle(SpanStyle(color = BrandPrimary)) { append(hour.value) }
                    append(recommendation.substring(hour.range.last + 1))
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Text(text = basis, style = MaterialTheme.typography.caption, color = TextTertiary)
    }
}

// ── 시간대별 ─────────────────────────────────────────────

@Composable
private fun HourlyCard(place: PlaceUi, nowHour: Int, selectedHour: Int?, onBarClick: (Int) -> Unit) {
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val week = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    val weekend = today.dayOfWeek.value >= 6
    DetailCard {
        CardTitle(title = "오늘 시간대별 감각부하", trailing = "$week · ${if (weekend) "주말" else "평일"}")
        if (place.hours.isEmpty()) {
            Text(text = "아직 시간대 정보가 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            HourlyBarChart(
                bars = place.hours,
                nowHour = nowHour,
                selectedHour = selectedHour,
                onBarClick = { onBarClick(it.hour) },
            )
            ChartLegend()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CalmIcon(CalmIcons.Check, size = 14.dp, tint = BrandPrimary)
                Text(text = "예측 결과가 아이의 반응을 보장하지는 않아요", style = MaterialTheme.typography.caption, color = TextTertiary)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderDefault))
        CardTitle(title = "주변 지역 혼잡", info = "서울시가 추정한 혼잡도예요. 매장 안 혼잡도와 다를 수 있어요.")
        // 오늘 줄이 있으면 실제 지역 혼잡, 없으면 예시 값
        AreaCongestionChart(levels = place.areaCrowd.ifEmpty { sampleAreaCongestion }, nowHour = nowHour)
    }
}

// 8 ~ 22시 지역 혼잡 1 여유 · 2 보통 · 3 약간 붐빔 · 4 붐빔 (예시)
private val sampleAreaCongestion: List<Int?> = listOf(1, 1, 1, 2, 2, 3, 3, 2, 2, 3, 4, 4, 3, 2, 1)
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
private fun AreaCongestionChart(levels: List<Int?>, nowHour: Int) {
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

// ── 점수 ────────────────────────────────────────────────

@Composable
private fun ScoreCard(score: ScoreBreakdown) {
    DetailCard {
        CardTitle(
            title = "${score.hour}시는 왜 ${levelWord(score.total)}인가요",
            info = "네 가지 중 가장 높은 점수가 종합 점수예요.",
        )
        score.factors.forEach { factor ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalmIcon(factorIcon(factor.label), size = 18.dp, tint = if (factor.isMain) BrandPrimary else TextSecondary)
                    Text(text = factor.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                    ScoreBar(score = factor.score, main = factor.isMain)
                    Text(
                        text = "${factor.score}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (factor.isMain) BrandPrimary else TextPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(24.dp),
                    )
                }
                if (factor.isMain) MainFactorNote(text = score.mainFactor)
            }
        }
        // 종합 점수
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.tile)
                .background(BgSubtle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "종합 점수", style = MaterialTheme.typography.caption, color = TextTertiary)
                Text(text = "${score.total}", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            }
            LevelBadge(level = levelOf(score.total))
        }
        Text(text = score.basis, style = MaterialTheme.typography.caption, color = TextTertiary)
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderDefault))
        CardTitle(title = "주변 지역 혼잡", info = "서울시가 추정한 혼잡도예요. 매장 안 혼잡도와 다를 수 있어요.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AreaTile(name = score.areaName, value = score.areaCrowd, modifier = Modifier.weight(1f))
            AreaTile(name = score.commercialName, value = score.commercialCrowd, modifier = Modifier.weight(1f))
        }
    }
}

/** 주변 지역 · 상권 한 칸. 이름은 작게, 단계는 굵게 */
@Composable
private fun AreaTile(name: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Radius.tile)
            .background(BgSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = name, style = MaterialTheme.typography.caption, color = TextTertiary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

// 점수 막대 112 × 8. 주 요인은 파랑, 나머지는 단계 진하기
@Composable
private fun ScoreBar(score: Int, main: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BgSunken),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(score.coerceIn(0, 100) / 100f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        main -> BrandPrimary
                        score >= 67 -> LevelHigh
                        score >= 40 -> LevelMid
                        else -> LevelCalm
                    },
                ),
        )
    }
}

// '주 요인' 표시 상자
@Composable
private fun MainFactorNote(text: String) {
    Row(
        modifier = Modifier
            .padding(start = 28.dp)
            .fillMaxWidth()
            .clip(Radius.tile)
            .background(BrandPrimarySoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "주 요인",
            style = MaterialTheme.typography.labelSmall,
            color = BgSurface,
            modifier = Modifier
                .clip(Radius.pill)
                .background(BrandPrimary)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

private fun factorIcon(label: String): CalmIcons = when {
    "종류" in label || "돌발" in label -> CalmIcons.Volume
    "크기" in label -> CalmIcons.VolumeLow
    "혼잡" in label -> CalmIcons.Users
    "밝기" in label || "빛" in label -> CalmIcons.Sun
    else -> CalmIcons.Info
}

// ── 소리 ────────────────────────────────────────────────

@Composable
private fun SoundCard(sounds: List<SoundRow>) {
    DetailCard {
        CardTitle(title = "이 장소에서 들리는 소리")
        sounds.forEach { row ->
            if (row.emphasized) {
                // 강조 줄: 연한 파랑 상자 + 횟수 크게
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius.tile)
                        .background(BrandPrimarySoft)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalmIcon(soundIcon(row.label), size = 18.dp, tint = BrandPrimaryPressed)
                        Text(text = row.label, style = MaterialTheme.typography.labelLarge, color = BrandPrimaryPressed, modifier = Modifier.weight(1f))
                        Text(text = row.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = BrandPrimaryPressed)
                    }
                    if (row.subLabel != null) {
                        Text(
                            text = listOfNotNull(row.subLabel, row.subValue).joinToString(" · "),
                            style = MaterialTheme.typography.caption,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 28.dp),
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalmIcon(soundIcon(row.label), size = 18.dp, tint = TextSecondary)
                    Text(text = row.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                    FrequencyMeter(level = frequencyOf(row.value))
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(64.dp),
                    )
                }
            }
        }
        Text(text = "원음은 저장하지 않고 소리 종류만 기록해요", style = MaterialTheme.typography.caption, color = TextTertiary)
    }
}

private fun soundIcon(label: String): CalmIcons = when {
    "갑작" in label || "안내" in label -> CalmIcons.Volume
    "음악" in label -> CalmIcons.Music
    "웅성" in label || "말" in label -> CalmIcons.Users
    "기계" in label -> CalmIcons.Machine
    "경보" in label || "큰 소리" in label -> CalmIcons.Warning
    else -> CalmIcons.Volume
}

/** "계속 들림 / 자주 / 가끔 / 드묾 / 관측 없음" → 소리 빈도 막대 0~4 */
private fun frequencyOf(value: String): Int = when {
    "계속" in value || "항상" in value -> 4
    "자주" in value -> 3
    "가끔" in value -> 2
    "드묾" in value || "회" in value -> 1
    else -> 0
}

// ── 빛 ─────────────────────────────────────────────────

@Composable
private fun LightCard(detail: PlaceDetailUi) {
    val light = detail.light
    DetailCard {
        CardTitle(title = "빛")
        if (light == null) {
            Text(text = "아직 빛 측정이 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            return@DetailCard
        }
        // 두 칸 높이를 맞춘다. 줄 수가 달라도 키 큰 칸에 맞춰 늘어난다
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LightTile(icon = CalmIcons.Sun, label = "밝기", measured = true, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "${light.lux}", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                    Text(
                        text = "lux",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                LevelBadge(level = light.luxLevel, modifier = Modifier.padding(top = 4.dp))
            }
            LightTile(icon = CalmIcons.Bolt, label = "깜빡임", measured = light.flickerMeasured, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = light.flicker,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (light.flickerMeasured) TextPrimary else TextSecondary,
                )
                Text(
                    text = if (light.flickerMeasured) "측정으로 확인했어요" else "폰으로 잴 수 없어요",
                    style = MaterialTheme.typography.caption,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.tile)
                .background(if (light.flickerMeasured) BgSubtle else StatusCautionSoft)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 확인된 경우는 안내라 체크, 미측정은 주의
            CalmIcon(
                if (light.flickerMeasured) CalmIcons.Check else CalmIcons.Alert,
                size = 16.dp,
                tint = if (light.flickerMeasured) TextSecondary else StatusCaution,
            )
            Text(
                text = light.note,
                style = MaterialTheme.typography.caption,
                color = if (light.flickerMeasured) TextSecondary else StatusCaution,
            )
        }
        light.report?.let { Text(text = it, style = MaterialTheme.typography.caption, color = TextTertiary) }
    }
}

// 밝기 · 깜빡임 칸. 재지 않은 값은 흰 바탕 + 점선 테두리로 '확인 안 됨' 을 보여준다
@Composable
private fun LightTile(
    icon: CalmIcons,
    label: String,
    measured: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(Radius.tile)
            .then(
                if (measured) Modifier.background(BgSubtle)
                else Modifier.background(BgSurface).outlineBorder(LevelNoData, Radius.tile, dashed = true),
            )
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalmIcon(icon, size = 14.dp, tint = TextTertiary)
            Text(text = label, style = MaterialTheme.typography.caption, color = TextTertiary)
        }
        content()
    }
}

// ── 사업장 ──────────────────────────────────────────────

@Composable
private fun BusinessCard(detail: PlaceDetailUi) {
    val calm = detail.calmHours
    DetailCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalmIcon(CalmIcons.Store, size = 18.dp, tint = TextSecondary)
            Text(text = "사업장이 등록한 조용한 시간", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        if (calm?.businessHours == null) {
            Text(text = "아직 사업장이 등록한 시간이 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "매주", style = MaterialTheme.typography.caption, color = TextTertiary)
                Text(text = calm.businessHours, style = MaterialTheme.typography.headlineSmall, color = BrandPrimary)
                Text(text = "이 시간에 사업장이 하는 것", style = MaterialTheme.typography.caption, color = TextTertiary)
            }
            calm.businessActions?.let { actions ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.split("·").map { it.trim() }.forEach { action ->
                        ActionChip(label = action, icon = actionIcon(action))
                    }
                }
            }
            Text(
                text = "우리 관측: ${calm.aiHours} (${calm.aiBasis}). 방문 혼잡은 관측으로 확인된 시간대만 쓰고, 등록한 시간은 점수에 넣지 않아요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
        }
    }
}

private fun actionIcon(action: String): CalmIcons = when {
    "음악" in action -> CalmIcons.MusicOff
    "조명" in action -> CalmIcons.LightDim
    "방송" in action -> CalmIcons.VolumeOff
    else -> CalmIcons.Check
}

/** 점수를 단계 말로 바꾼다. 숫자만 보여주면 80이 높은 건지 낮은 건지 모른다 */
private fun levelWord(score: Int): String = levelOf(score).label

private fun levelOf(score: Int): SensoryLevel = when {
    score >= 67 -> SensoryLevel.HIGH
    score >= 34 -> SensoryLevel.MID
    else -> SensoryLevel.CALM
}
