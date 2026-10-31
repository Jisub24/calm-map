package com.calmmap.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.ButtonSize
import com.calmmap.ui.component.CalmTabs
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.InfoButton
import com.calmmap.ui.component.InfoNote
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.PlaceDetailUi
import com.calmmap.ui.model.NoDataUi
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.TimeSlot
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSunken
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.LevelNoData
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import com.calmmap.ui.component.AreaCongestionSection
import com.calmmap.ui.component.areaCongestionInfo
import java.time.LocalTime
import java.time.ZoneId

/** 서울시 지역 혼잡도 4단계 */
private val congestionSteps = listOf("여유", "보통", "약간 붐빔", "붐빔")

/** 정보 부족 화면 탭. 시간마다 달라지는 점수 칸이 없어서 장소 상세보다 적다 */
private enum class NoDataTab(val label: String) {
    AREA("주변 혼잡"),
    SOUND("소리"),
    LIGHT("빛"),
}

/**
 * 최종안 10 정보 부족 장소 · ⓘ 눌렀을 때.
 *
 * 종합 점수를 내지 않는다. 소리 · 빛이 비면 실제 최댓값을 알 수 없어서,
 * 혼잡만으로 점수를 내면 부담을 낮게 잘못 안내하게 된다.
 * 대신 탭으로 ① 주변 지역 혼잡 ② 소리 ③ 빛 을 보여준다. 소리 · 빛은 평일 · 주말 상관없는 장소 고유값만
 * (답변서2 질의1). 측정 요청 · 직접 측정 카드는 어느 탭이든 아래에 둔다.
 */
@Composable
fun NoDataScreen(
    place: PlaceUi,
    /** 지역 혼잡 · 후기 · 요청 수. 아직 DB 에서 못 가져오는 장소는 null 이고 그 카드들을 뺀다 */
    info: NoDataUi?,
    onBack: () -> Unit,
    onMeasureClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** "주말 기록 보기". 그 묶음 · 시각으로 장소 상세를 연다 */
    onOtherRecordClick: (TimeSlot) -> Unit = {},
    /** 소리 · 빛 탭 값. 장소 상세와 같이 읽는다. 읽는 중이면 null */
    detail: PlaceDetailUi? = null,
    detailFailed: Boolean = false,
    onRetry: () -> Unit = {},
    /** 이 장소의 측정 요청 수 (measurement_requests). 로그인 전이거나 아직 못 읽었으면 null */
    requestCount: Int? = null,
    /** 내가 이미 요청했는지. 그러면 버튼이 '요청 취소' 가 된다 */
    requested: Boolean = false,
    /** '측정 요청하기' · '요청 취소'. 로그인 확인 · 저장은 부르는 쪽에서 */
    onRequestClick: () -> Unit = {},
    /** 요청을 저장하는 중. 버튼을 막아 두 번 누르지 않게 */
    requesting: Boolean = false,
) {
    val other = place.otherRecord
    var tab by remember(place.id, place.slot) { mutableStateOf(NoDataTab.AREA) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = place.name, onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
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

            // 점수가 없어도 소리 종류 · 밝기는 장소 고유값이라 우리 아이 맞춤의 주의는 알린다
            place.caution?.let { CautionBanner(text = it) }

            // 모르는 것은 모른다고: 점선 테두리 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .outlineBorder(LevelNoData, Radius.card, dashed = true)
                    .padding(horizontal = 19.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (other == null) {
                    Text(text = "이 장소의 소리·빛 정보가 아직 없어요", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = "모르는 것을 '무던함'으로 추측하지 않아요. 종합 점수는 측정이 쌓이면 보여드려요.",
                        style = MaterialTheme.typography.caption,
                        color = TextSecondary,
                    )
                } else {
                    // 다른 묶음에는 잰 기록이 있다. 붐비는 정도가 달라 이쪽 점수로 쓰지 않고 그 기록으로 보내 준다
                    Text(text = "${other.emptyGroupLabel}에 잰 기록은 아직 없어요", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(text = "${other.slot.groupLabel}에는 ${other.count}번 쟀어요", style = MaterialTheme.typography.caption, color = TextSecondary)
                    other.summary?.let { Text(text = it, style = MaterialTheme.typography.caption, color = TextSecondary) }
                    SecondaryButton(
                        text = "${other.slot.groupLabel} 기록 보기",
                        onClick = { onOtherRecordClick(other.slot) },
                        size = ButtonSize.M,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }

            CalmTabs(
                tabs = NoDataTab.entries.map { it.label },
                selectedIndex = tab.ordinal,
                onSelect = { tab = NoDataTab.entries[it] },
            )

            when {
                tab == NoDataTab.AREA -> AreaTab(place, info)
                detail == null && detailFailed -> FailedCard(onRetry = onRetry)
                detail == null -> LoadingCard()
                tab == NoDataTab.SOUND -> SoundCard(
                    sounds = detail.fixedSounds,
                    measureCount = detail.measureCount,
                    info = "안내방송 · 배경 음악은 평일 · 주말 상관없이 이 장소에서 잰 모든 측정으로 보여드려요. " +
                        "말소리 · 돌발 소리는 시간마다 달라서 그 시간대 기록에서 보여드려요.",
                )
                else -> LightCard(detail.light)
            }

            // 측정 요청 카드 (연한 파랑). 어느 탭이든 보이게 탭 내용 아래에 둔다
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BrandPrimarySoft)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CalmIcon(CalmIcons.Users, size = 20.dp, tint = BrandPrimaryPressed)
                    Text(
                        text = when {
                            // 요청 수는 로그인해야 읽을 수 있다 (014 RLS)
                            requestCount == null -> "측정을 요청할 수 있어요"
                            requestCount == 0 -> "아직 측정 요청이 없어요"
                            else -> "지금 ${requestCount}명이 측정을 요청했어요"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandPrimaryPressed,
                    )
                }
                Text(
                    text = if (other != null && place.slot == null) {
                        "${other.emptyGroupLabel}에 직접 재면 ${other.emptyGroupLabel} 점수가 생겨요. 원음은 저장하지도 보내지도 않아요."
                    } else {
                        "요청이 많은 곳부터 측정해요. 원음은 저장하지도 보내지도 않아요."
                    },
                    style = MaterialTheme.typography.caption,
                    color = TextSecondary,
                )
                // 시간 선택 중에는 측정을 막는다
                val measureLocked = place.slot != null
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "직접 측정하기", onClick = onMeasureClick, enabled = !measureLocked, size = ButtonSize.M, modifier = Modifier.weight(1f))
                    // 요청한 뒤에는 취소할 수 있다. 취소는 되돌리는 일이라 흰 버튼
                    if (requested) {
                        SecondaryButton(
                            text = "요청 취소",
                            onClick = onRequestClick,
                            enabled = !requesting,
                            size = ButtonSize.M,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        PrimaryButton(
                            text = "측정 요청하기",
                            onClick = onRequestClick,
                            enabled = !requesting,
                            size = ButtonSize.M,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 주변 혼잡 탭. 점수는 없어도 주변 지역 혼잡은 서울시 자료라 보여줄 수 있다. 장소 상세와 같은 꺾은선.
 * 지도에서 시간을 골랐으면 쌓인 기록, 아니면 오늘 값
 */
@Composable
private fun AreaTab(place: PlaceUi, info: NoDataUi?) {
    if (info == null) {
        val slot = place.slot
        var showInfo by remember { mutableStateOf(false) }
        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "주변 지역 혼잡", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                InfoButton(open = showInfo, onClick = { showInfo = !showInfo })
            }
            if (showInfo) InfoNote(areaCongestionInfo(slot), modifier = Modifier.padding(bottom = 8.dp))
            AreaCongestionSection(
                levels = place.areaCrowd,
                markHour = slot?.hour ?: LocalTime.now(ZoneId.of("Asia/Seoul")).hour,
                slot = slot,
                days = place.areaDays,
            )
        }
        return
    }

    AreaCongestionCard(info)
    WhiteCard {
        Text(text = "보호자 후기 ${info.reviewTotal}건", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = "점수에는 넣지 않아요", style = MaterialTheme.typography.caption, color = TextTertiary)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewPill("괜찮았어요", info.reviewGood, Modifier.weight(1f))
            ReviewPill("힘들었어요", info.reviewHard, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(16.dp),
        content = content,
    )
}

/** 주변 지역 혼잡: 4칸 막대 + 지금 · 다음 변화. ⓘ 를 누르면 출처를 알린다 */
@Composable
private fun AreaCongestionCard(info: NoDataUi) {
    var showInfo by remember { mutableStateOf(false) }
    val nowStep = congestionSteps.indexOf(info.nowCongestion).coerceAtLeast(0)
    // 지금과 달라지는 첫 시간
    val next = info.forecast.firstOrNull { it.label != info.nowCongestion }

    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "주변 지역 혼잡", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = "${info.areaName} · 서울시 예측", style = MaterialTheme.typography.caption, color = TextTertiary)
            }
            InfoButton(open = showInfo, onClick = { showInfo = !showInfo })
        }
        if (showInfo) InfoNote("서울시가 측정한 지역 혼잡도 예측이에요. 매장 안 혼잡도와 다를 수 있어요.", modifier = Modifier.padding(top = 8.dp))
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            congestionSteps.forEachIndexed { i, step ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i <= nowStep) TextSecondary else BgSunken),
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == nowStep) TextPrimary else TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text = "지금 ${info.nowCongestion}" + (next?.let { " · ${it.hour}시부터 ${it.label}" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun ReviewPill(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(BgSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = "$count", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}
