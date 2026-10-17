package com.calmmap.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BackIcon
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.pressHighlight
import com.calmmap.ui.model.PlaceDetailUi
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.ScoreBreakdown
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineMid
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import com.calmmap.ui.theme.PaperSoft

/** 상세 화면 아래쪽 탭 */
private enum class DetailTab(val label: String) {
    SCORE("점수 구성"),
    SOUND("소리"),
    LIGHT("빛"),
    CALM("무던한 시간"),
}

/**
 * 와이어프레임 B4 장소 상세.
 *
 * 하단바(B2)가 "언제 가면 되나" 를 말한다면 여기는 **"왜 그런가"** 를 말한다.
 * 점수를 보여주는 것이 목적이 아니라, 그 점수가 어디서 나왔는지를 열어 보이는 화면이다.
 *
 * 내용이 화면 하나에 안 들어가 세로로 스크롤한다.
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
) {
    var personalized by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(DetailTab.SCORE) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        Header(title = place.name, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // 빈 곳을 누르면 그래프 말풍선을 닫는다.
                // 하단바(B2)와 같은 그래프를 쓰므로 닫는 방법도 같아야 한다
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { selectedHour = null }
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PersonalizeRow(
                on = personalized,
                onToggle = { personalized = !personalized },
            )

            detail?.warning?.takeIf { personalized }?.let { WarningBox(text = it) }

            RecommendationBox(
                recommendation = place.recommendation,
                basis = place.recommendationBasis,
            )

            if (place.hours.isNotEmpty()) {
                ChartSection(
                    place = place,
                    nowHour = nowHour,
                    selectedHour = selectedHour,
                    onBarClick = { selectedHour = if (selectedHour == it.hour) null else it.hour },
                )
            }

            if (detail != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailTab.entries.forEach { entry ->
                        FilterChip(
                            label = entry.label,
                            selected = tab == entry,
                            onClick = { tab = entry },
                        )
                    }
                }

                when (tab) {
                    DetailTab.SCORE -> ScoreCard(detail.score)
                    DetailTab.SOUND -> SoundCard(detail)
                    DetailTab.LIGHT -> LightCard(detail)
                    DetailTab.CALM -> CalmCard(detail)
                }
            }

            Row(
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(
                    text = "이 장소 측정",
                    onClick = onMeasureClick,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "이 시간에 방문",
                    onClick = onVisitClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressHighlight(
                    shape = RoundedCornerShape(percent = 50),
                    role = Role.Button,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BackIcon(size = 22.dp, color = Ink)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/**
 * 우리 아이 맞춤 스위치.
 *
 * 켜면 아이가 '매우 민감' 으로 고른 자극이 관측된 장소에 주의 표시가 뜬다.
 * 끄면 그 표시가 사라진다. 같은 장소도 아이마다 다르게 보여야 한다는 것이
 * 이 앱의 핵심이라 상세 화면 맨 위에 둔다.
 */
@Composable
private fun PersonalizeRow(on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Switch, onClick = onToggle)
            .clip(RoundedCornerShape(8.dp))
            .background(PaperSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "우리 아이 맞춤",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (on) LineStrong else Line),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Paper),
            )
        }
    }
}

/** 민감 자극이 관측됐을 때만 뜨는 줄 */
@Composable
private fun WarningBox(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .outlineBorder(Ink, shape, width = 1.5.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Ink)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "주의",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Paper,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Ink,
        )
    }
}

@Composable
private fun RecommendationBox(recommendation: String, basis: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PaperBox)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = recommendation,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
        )
        Text(
            text = "근거: $basis",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
        )
        // 예측이라는 것을 분명히 적는다. 빠지면 보장으로 읽힌다
        Text(
            text = "예측이에요. 아이의 반응을 보장하지는 않아요.",
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
        )
    }
}

@Composable
private fun ChartSection(
    place: PlaceUi,
    nowHour: Int,
    selectedHour: Int?,
    onBarClick: (com.calmmap.ui.model.HourBar) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "오늘 시간대별 감각부하",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "채움 = 실측 · 점선 = 추정 · ? = 정보 부족",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                )
            }
            // '감각부하' 는 우리가 만든 말이라 처음 보는 사람은 모른다.
            // 이 말이 사용자 눈에 보이는 곳이 여기뿐이라 여기서 한 번 풀어 준다
            Text(
                text = "소리 · 혼잡 · 밝기가 아이에게 주는 부담이에요",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
        HourlyBarChart(
            bars = place.hours,
            nowHour = nowHour,
            compact = false,
            selectedHour = selectedHour,
            onBarClick = onBarClick,
        )
    }
}

/**
 * 점수 구성. 이 화면에서 가장 중요한 카드다.
 *
 * 하위 항목 4개를 **평균 내지 않고 가장 높은 값**을 종합 지수로 쓴다.
 * 평균을 내면 한 항목이 아주 높아도 나머지가 낮으면 묻혀서,
 * 안내방송이 잦은 곳을 '무던함' 으로 잘못 안내하게 된다.
 */
@Composable
private fun ScoreCard(score: ScoreBreakdown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .outlineBorder(LineSoft, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${score.hour}시는 왜 ${levelWord(score.total)}인가요",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (score.total >= 67) LevelHigh else LevelMid)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = score.total.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (score.total >= 67) Paper else Ink,
                )
            }
        }

        score.factors.forEach { factor ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = factor.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (factor.isMain) FontWeight.Bold else FontWeight.Normal,
                    color = Ink,
                    modifier = Modifier.size(width = 104.dp, height = 18.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(LineFaint),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(factor.score / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (factor.isMain) Ink else LevelMid),
                    )
                }
                Text(
                    text = factor.score.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (factor.isMain) FontWeight.Bold else FontWeight.Normal,
                    color = Ink,
                    modifier = Modifier.size(width = 26.dp, height = 18.dp),
                )
            }
        }

        Text(
            text = "평균 내지 않고 가장 높은 값을 써요 · 주 요인: ${score.mainFactor}",
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
        Text(
            text = score.basis,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LineFaint),
        )
        Text(
            text = score.areaNote,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
    }
}

@Composable
private fun SoundCard(detail: PlaceDetailUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .outlineBorder(LineSoft, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "이 장소에서 들리는 소리",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        detail.sounds.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (index != 0) it.padding(top = 8.dp) else it },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (row.emphasized) FontWeight.Bold else FontWeight.Normal,
                    color = Ink,
                )
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (row.emphasized) FontWeight.Bold else FontWeight.Normal,
                    color = Ink,
                )
            }
            if (row.subLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.subLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    Text(
                        text = row.subValue.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun LightCard(detail: PlaceDetailUi) {
    val light = detail.light ?: return
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .outlineBorder(LineSoft, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LightTile(
                label = "밝기",
                value = light.lux,
                dashed = false,
                shape = shape,
                modifier = Modifier.weight(1f),
            )
            LightTile(
                label = "깜빡임",
                value = light.flicker,
                // 안 잰 것은 점선으로. 다른 화면의 '추정' 표시와 같은 뜻이다
                dashed = !light.flickerMeasured,
                shape = shape,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = light.note,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
        light.report?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(PaperSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun LightTile(
    label: String,
    value: String,
    dashed: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .outlineBorder(if (dashed) LineMid else LineSoft, shape, dashed = dashed)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

/**
 * 사업장이 등록한 시간과 우리가 관측으로 뽑은 시간을 나란히 둔다.
 *
 * 등록만으로 점수를 낮추지 않는다. 사업장이 "우리는 조용해요" 라고 적는다고
 * 조용해지는 것이 아니기 때문이다. 둘을 같이 보여주고 판단은 보호자가 한다.
 */
@Composable
private fun CalmCard(detail: PlaceDetailUi) {
    val calm = detail.calmHours ?: run {
        Text(
            text = "아직 사업장이 등록한 무던한 시간이 없어요.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
        )
        return
    }
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .outlineBorder(LineSoft, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmTile(
                badge = "사업장 등록 · 운영 계획",
                badgeFilled = false,
                title = calm.businessHours.orEmpty(),
                body = calm.businessActions.orEmpty(),
                strong = true,
                shape = shape,
                modifier = Modifier.weight(1f),
            )
            CalmTile(
                badge = "AI 예측 · 관측 기반",
                badgeFilled = true,
                title = calm.aiHours,
                body = calm.aiBasis,
                strong = false,
                shape = shape,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "방문 권고는 관측으로 확인된 시간을 먼저 써요. 등록만으로 점수를 낮추지 않아요.",
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
    }
}

@Composable
private fun CalmTile(
    badge: String,
    badgeFilled: Boolean,
    title: String,
    body: String,
    strong: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .outlineBorder(if (strong) LineStrong else LineSoft, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .let {
                    if (badgeFilled) it.background(LineFaint)
                    else it.outlineBorder(LineStrong, RoundedCornerShape(3.dp))
                }
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
    }
}

/** 점수를 단계 말로 바꾼다. 숫자만 보여주면 80이 높은 건지 낮은 건지 모른다 */
private fun levelWord(score: Int): String = when {
    score >= 67 -> "부담 높음"
    score >= 34 -> "보통"
    else -> "무던함"
}
