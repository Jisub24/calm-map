package com.calmmap.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BackIcon
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressHighlight
import com.calmmap.ui.model.NoDataUi
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import com.calmmap.ui.theme.PaperSoft
import com.calmmap.ui.theme.SensoryLevel

/**
 * 와이어프레임 B5. 소리 · 빛 측정이 없는 장소의 상세.
 *
 * B4 와 가장 다른 점은 **종합 점수를 보여주지 않는다**는 것이다.
 * 혼잡 정보는 있지만 그것만으로 점수를 내면 부담을 낮게 잘못 안내하게 된다.
 * 모르면 모른다고 말하고, 대신 측정을 요청할 길을 준다.
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
            .background(Paper)
            .safeDrawingPadding(),
    ) {
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
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "${place.category} · 도보 ${place.walkMinutes}분 · ${if (place.openNow) "영업 중" else "영업 종료"}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )

            NoScoreBox()
            AreaCongestionCard(info)
            ReviewCard(info)
        }

        // 측정 요청은 화면 아래에 고정한다. 스크롤해서 찾게 하면
        // 정보가 없는 곳에서 할 수 있는 유일한 일을 못 보고 나가게 된다
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "요청이 많은 곳부터 측정해요 · 지금 ${info.requestCount}명 요청함",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = if (requested) "요청함" else "측정 요청하기",
                onClick = { requested = true },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "내가 직접 측정하기",
                onClick = onMeasureClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 종합 점수 자리에 들어가는 상자.
 *
 * 여기에 숫자나 단계 색을 넣지 않는다. 색을 붙이면 그 자체가 판정으로 읽힌다.
 */
@Composable
private fun NoScoreBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PaperBox)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LevelBadge(level = SensoryLevel.NO_DATA)
        Text(
            text = "이 장소의 소리 · 빛 정보가 아직 없어요",
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
        )
    }
}

/**
 * 주변 지역 혼잡.
 *
 * '주변 지역' 이라고 분명히 쓰는 것이 중요하다.
 * 서울시 값은 통신 기지국으로 추정한 **지역 인구**라서 매장 안과 다를 수 있다.
 * 홍대에서 지역 전체가 '보통' 인데 한식은 '분주한', 마트는 '한산한' 인 것을
 * 실제로 확인했다.
 */
@Composable
private fun AreaCongestionCard(info: NoDataUi) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .outlineBorder(LineSoft, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "주변 지역 혼잡",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "지금 ${info.nowCongestion}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            info.forecast.forEach { hour ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LineFaint)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${hour.hour}시",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    Text(
                        text = hour.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text = "서울시 실시간 도시데이터 4단계(여유 · 보통 · 약간 붐빔 · 붐빔). " +
                "통신 신호로 추정한 ${info.areaName} 지역 인구라 매장 안 혼잡과 다를 수 있어요.",
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
        )
    }
}

/**
 * 보호자 후기. 개수만 보여주고 점수로 환산하지 않는다.
 *
 * 후기는 몇 명이 어떤 날 어떤 상태로 갔는지에 따라 흔들려서,
 * 숫자로 바꾸면 측정값과 같은 무게로 읽히게 된다.
 */
@Composable
private fun ReviewCard(info: NoDataUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PaperSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "보호자 후기 ${info.reviewTotal}건",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        Text(
            text = "괜찮았어요 ${info.reviewGood} · 힘들었어요 ${info.reviewHard}",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
        )
    }
}
