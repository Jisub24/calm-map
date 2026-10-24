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
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.InfoButton
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TooltipBubble
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.NoDataUi
import com.calmmap.ui.model.PlaceUi
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

/** 서울시 지역 혼잡도 4단계 */
private val congestionSteps = listOf("여유", "보통", "약간 붐빔", "붐빔")

/**
 * 최종안 10 정보 부족 장소 · ⓘ 눌렀을 때.
 *
 * 종합 점수를 내지 않는다. 소리 · 빛이 비면 실제 최댓값을 알 수 없어서,
 * 혼잡만으로 점수를 내면 부담을 낮게 잘못 안내하게 된다.
 * 대신 ① 주변 지역 혼잡 ② 보호자 후기 ③ 측정 요청 · 직접 측정 을 보여준다.
 */
@Composable
fun NoDataScreen(
    place: PlaceUi,
    info: NoDataUi,
    onBack: () -> Unit,
    onMeasureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var requested by remember { mutableStateOf(false) }

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
                    text = "${place.category} · 도보 ${place.walkMinutes}분 · ${if (place.openNow) "영업 중" else "영업 종료"}",
                    style = MaterialTheme.typography.caption,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                LevelBadge(level = place.level)
            }

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
                Text(text = "이 장소의 소리·빛 정보가 아직 없어요", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = "모르는 것을 '무던함'으로 추측하지 않아요. 종합 점수는 측정이 쌓이면 보여드려요.",
                    style = MaterialTheme.typography.caption,
                    color = TextSecondary,
                )
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

            // 측정 요청 카드 (연한 파랑)
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
                        text = "지금 ${info.requestCount + if (requested) 1 else 0}명이 측정을 요청했어요",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandPrimaryPressed,
                    )
                }
                Text(
                    text = "요청이 많은 곳부터 측정해요. 원음은 저장하지도 보내지도 않아요.",
                    style = MaterialTheme.typography.caption,
                    color = TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(text = "직접 측정하기", onClick = onMeasureClick, size = ButtonSize.M, modifier = Modifier.weight(1f))
                    // TODO: measurement_requests 에 저장
                    PrimaryButton(
                        text = if (requested) "요청했어요" else "측정 요청하기",
                        onClick = { requested = true },
                        enabled = !requested,
                        size = ButtonSize.M,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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

/** 주변 지역 혼잡: 4칸 막대 + 지금 · 다음 변화. ⓘ 를 누르면 말풍선으로 출처를 알린다 */
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
            InfoButton(onClick = { showInfo = !showInfo })
        }
        if (showInfo) {
            TooltipBubble(
                title = "서울시가 측정한 지역 혼잡도 예측이에요.",
                body = "매장 안 혼잡도와 다를 수 있어요.",
                modifier = Modifier.align(Alignment.End),
            )
        }
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
