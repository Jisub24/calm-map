package com.calmmap.ui.detail

import com.calmmap.ui.theme.LevelNoData
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
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
import com.calmmap.ui.component.AreaCongestionSection
import com.calmmap.ui.component.areaCongestionInfo
import com.calmmap.ui.component.ChartLegend
import com.calmmap.ui.component.FrequencyMeter
import com.calmmap.ui.component.FullSegmented
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.InfoButton
import com.calmmap.ui.component.InfoNote
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.ButtonSize
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.LightInfo
import com.calmmap.ui.model.PlaceDetailUi
import com.calmmap.ui.model.MEASURE_NOW_ONLY
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
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.StatusCautionSoft
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
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
    /**
     * '모두 기준 / 우리 아이 맞춤' 을 눌렀을 때. 지도 칩과 같은 상태라 부르는 쪽이 들고 있다.
     * 맞춤이 켜져 있으면 [place] 는 이미 맞춤 값으로 바꾼 것이다 (place.personal)
     */
    onPersonalizeChange: (Boolean) -> Unit = {},
    /** 점수 · 소리 · 빛 · 사업장 탭 값을 못 읽었는지. 그러면 다시 시도 버튼을 띄운다 */
    detailFailed: Boolean = false,
    onRetry: () -> Unit = {},
    /** 우리 아이 맞춤을 켰는지. 고른 자극이 없어 place 가 그대로여도 세그먼트는 켜진 쪽으로 */
    personalized: Boolean = place.personal,
) {
    var tab by remember { mutableStateOf(DetailTab.HOURLY) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }

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
                LevelBadge(level = place.level, prefix = if (place.personal) "우리 아이 기준 ·" else null)
            }

            FullSegmented(
                options = listOf("모두 기준", "우리 아이 맞춤"),
                selectedIndex = if (personalized) 1 else 0,
                onSelect = { onPersonalizeChange(it == 1) },
            )

            // '매우'로 고른 자극이 관측된 곳이면 맞춤 기준에서 먼저 알린다
            place.caution?.let { CautionBanner(text = it) }

            RecommendationCard(recommendation = place.recommendation, basis = place.recommendationBasis)

            CalmTabs(
                tabs = DetailTab.entries.map { it.label },
                selectedIndex = tab.ordinal,
                onSelect = { tab = DetailTab.entries[it] },
            )

            when {
                tab == DetailTab.HOURLY -> HourlyCard(
                    place = place,
                    nowHour = nowHour,
                    selectedHour = selectedHour,
                    onBarClick = { selectedHour = if (selectedHour == it) null else it },
                )
                // 시간대별 말고는 DB 에서 따로 읽는다. 오는 동안 빈 카드, 못 읽었으면 다시 시도
                detail == null && detailFailed -> FailedCard(onRetry = onRetry)
                detail == null -> LoadingCard()
                tab == DetailTab.SCORE -> ScoreCard(detail.score)
                tab == DetailTab.SOUND -> SoundCard(detail.sounds, detail.measureCount)
                tab == DetailTab.LIGHT -> LightCard(detail.light)
                else -> BusinessCard(detail)
            }
        }

        // 시간 선택 중에는 측정을 막는다
        val measureLocked = place.slot != null
        BottomCta(note = if (measureLocked) MEASURE_NOW_ONLY else null) {
            SecondaryButton(text = "이 장소 측정", onClick = onMeasureClick, enabled = !measureLocked, modifier = Modifier.weight(1f))
            PrimaryButton(text = "이 시간에 방문", onClick = onVisitClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun LoadingCard() {
    DetailCard {
        Text(text = "불러오는 중…", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
}

/** 탭 값을 못 읽었을 때. 계속 '불러오는 중' 으로 두면 기다려야 하는지 알 수 없다 */
@Composable
internal fun FailedCard(onRetry: () -> Unit) {
    DetailCard {
        Text(text = "정보를 불러오지 못했어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        SecondaryButton(text = "다시 시도", onClick = onRetry, size = ButtonSize.M, modifier = Modifier.fillMaxWidth())
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
            if (info != null) InfoButton(open = showInfo, onClick = { showInfo = !showInfo })
        }
        // ⓘ 를 누르면 제목 아래에 설명을 펼친다
        if (info != null && showInfo) InfoNote(info)
    }
}

/** 우리 아이 맞춤에서 '매우' 자극이 관측됐을 때 (주의 색 상자). 장소 카드 · 정보 부족 화면도 쓴다 */
@Composable
internal fun CautionBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
    val slot = place.slot
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val week = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    // 지도에서 시간을 골랐으면 그 시각을 '선택' 으로 표시한다. 값이 특정 날짜가 아니라 쌓인 기록이라 날짜는 안 쓴다
    val markHour = slot?.hour ?: nowHour
    val markLabel = if (slot == null) "지금" else "선택"
    DetailCard {
        if (slot == null) {
            CardTitle(title = "오늘 시간대별 감각부하", trailing = "$week · ${if (today.dayOfWeek.value >= 6) "주말" else "평일"}")
        } else {
            CardTitle(title = "${slot.groupLabel} 시간대별 감각부하")
        }
        if (place.hours.isEmpty()) {
            Text(text = "아직 시간대 정보가 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            HourlyBarChart(
                bars = place.hours,
                nowHour = markHour,
                nowLabel = markLabel,
                selectedHour = selectedHour,
                onBarClick = { onBarClick(it.hour) },
            )
            ChartLegend(bars = place.hours)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CalmIcon(CalmIcons.Check, size = 14.dp, tint = BrandPrimary)
                Text(text = "예측 결과가 아이의 반응을 보장하지는 않아요", style = MaterialTheme.typography.caption, color = TextTertiary)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderDefault))
        CardTitle(title = "주변 지역 혼잡", info = areaCongestionInfo(slot))
        AreaCongestionSection(levels = place.areaCrowd, markHour = markHour, slot = slot, days = place.areaDays)
    }
}

// ── 점수 ────────────────────────────────────────────────

@Composable
private fun ScoreCard(score: ScoreBreakdown?) {
    if (score == null) {
        DetailCard {
            CardTitle(title = "점수")
            Text(text = "아직 점수가 없어요. 측정이 쌓이면 보여드려요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        return
    }
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
                    ScoreBar(score = factor.score ?: 0, main = factor.isMain)
                    Text(
                        text = factor.score?.toString() ?: "–",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (factor.isMain) BrandPrimary else TextPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(24.dp),
                    )
                }
                if (factor.isMain && score.mainFactor.isNotBlank()) MainFactorNote(text = score.mainFactor)
                else if (factor.note != null) Text(
                    text = factor.note,
                    style = MaterialTheme.typography.caption,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 28.dp),
                )
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
                Text(text = "종합 점수 · 4개 중 가장 높은 값", style = MaterialTheme.typography.caption, color = TextTertiary)
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
                        score >= 70 -> LevelHigh
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
internal fun SoundCard(sounds: List<SoundRow>, measureCount: Int?, info: String? = null) {
    DetailCard {
        CardTitle(title = "이 장소에서 들리는 소리", trailing = measureCount?.let { "측정 ${it}회" }, info = info)
        if (sounds.isEmpty()) {
            Text(text = "아직 소리 측정이 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            return@DetailCard
        }
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

/**
 * 빛 카드. 장소 상세 빛 탭과 정보 부족 화면이 같이 쓴다.
 * 밝기는 장소 고유값이라(답변서2 질의1) 평일 · 주말 · 시각과 상관없이 같은 값이다
 */
@Composable
internal fun LightCard(light: LightInfo?) {
    DetailCard {
        CardTitle(title = "빛", info = "밝기는 평일 · 주말 · 시간과 상관없이 이 장소에서 잰 모든 측정의 가운데 값이에요.")
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
                // 밝기 점수를 아직 못 냈으면 배지를 빼고 숫자만 둔다. '정보 부족' 배지가 붙으면 밝기를 모르는 것처럼 읽힌다
                if (light.luxLevel != SensoryLevel.NO_DATA) LevelBadge(level = light.luxLevel, modifier = Modifier.padding(top = 4.dp))
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

/** 사업장이 등록한 무던한 시간 + 요일별 영업시간. 정보 부족 화면의 '사업장' 탭도 쓴다 */
@Composable
internal fun BusinessCard(detail: PlaceDetailUi) {
    val calm = detail.calmHours
    DetailCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalmIcon(CalmIcons.Store, size = 18.dp, tint = TextSecondary)
            Text(text = "사업장이 등록한 무던한 시간", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        if (calm?.businessHours == null) {
            Text(text = "아직 사업장이 등록한 시간이 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "매주", style = MaterialTheme.typography.caption, color = TextTertiary)
                Text(text = calm.businessHours, style = MaterialTheme.typography.headlineSmall, color = BrandPrimary)
                Text(text = "이렇게 운영해요", style = MaterialTheme.typography.caption, color = TextTertiary)
            }
            calm.businessActions?.let { actions ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.split("·").map { it.trim() }.forEach { action ->
                        ActionChip(label = action, icon = actionIcon(action))
                    }
                }
            }
            Text(
                text = "방문 권고는 관측으로 확인된 시간을 먼저 써요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderDefault))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalmIcon(CalmIcons.Clock, size = 18.dp, tint = TextSecondary)
            Text(text = "영업시간", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        if (detail.openHours.isEmpty()) {
            Text(text = "등록된 영업시간이 없어요", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                detail.openHours.forEach { line ->
                    Row {
                        Text(text = line.days, style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.width(72.dp))
                        Text(text = line.time, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
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

// 기준선은 settings.level_thresholds(40 · 70)와 같은 값. 지도 그래프(PlaceRepository)와도 같다
private fun levelOf(score: Int): SensoryLevel = when {
    score >= 70 -> SensoryLevel.HIGH
    score >= 40 -> SensoryLevel.MID
    else -> SensoryLevel.CALM
}
