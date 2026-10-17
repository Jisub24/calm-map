package com.calmmap.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox

/**
 * 와이어프레임 B2. 지도에서 장소를 누르면 아래에서 올라오는 카드.
 *
 * 순서가 중요하다. **권고 시간대를 점수보다 먼저, 가장 크게** 둔다.
 * 보호자가 알고 싶은 것은 "몇 점인가" 가 아니라 "언제 가면 되나" 이고,
 * 점수를 앞세우면 장소를 줄 세우게 된다.
 */
@Composable
fun PlaceSheet(
    place: PlaceUi,
    nowHour: Int,
    onDetailClick: () -> Unit,
    onVisitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 그래프에서 누른 시각. 말풍선이 떠 있는 동안만 값이 있다
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Paper)
            // 카드의 빈 곳을 누르면 말풍선을 닫는다.
            // 말풍선을 닫는 방법이 같은 막대를 또 누르는 것뿐이면,
            // 어느 막대를 눌렀는지 잊었을 때 닫을 길이 없다
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { selectedHour = null }
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
    ) {
        // 끌어올릴 수 있다는 표시. 아직 끄는 동작은 없고 모양만 둔다
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Line),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
            )
            LevelBadge(level = place.level, prefix = "지금")
        }

        Text(
            text = buildString {
                append(place.category)
                append(" · 도보 ${place.walkMinutes}분")
                append(if (place.openNow) " · 영업 중" else " · 영업 종료")
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            modifier = Modifier.padding(top = 4.dp),
        )

        RecommendationBox(
            recommendation = place.recommendation,
            basis = place.recommendationBasis,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (place.hours.isNotEmpty()) {
            HourlyBarChart(
                bars = place.hours,
                nowHour = nowHour,
                compact = true,
                selectedHour = selectedHour,
                // 같은 막대를 다시 누르면 말풍선을 닫는다
                onBarClick = { selectedHour = if (selectedHour == it.hour) null else it.hour },
                modifier = Modifier.padding(top = 12.dp),
            )
            MissingHoursNote(
                place = place,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        place.businessNote?.let { note ->
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BusinessBadge()
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                text = "상세 보기",
                onClick = onDetailClick,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "방문 계획",
                onClick = onVisitClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 카드에서 가장 크게 보이는 자리. 점수가 아니라 시간대를 말한다 */
@Composable
private fun RecommendationBox(
    recommendation: String,
    basis: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PaperBox)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = recommendation,
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
        )
        Text(
            text = basis,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
    }
}

/**
 * 그래프에서 비어 있는 시간을 글자로 알려준다.
 *
 * 빈칸만 두면 "조용한 시간" 으로 잘못 읽힌다.
 * 모르는 것은 모른다고 말해야 한다.
 */
@Composable
private fun MissingHoursNote(place: PlaceUi, modifier: Modifier = Modifier) {
    val missing = place.hours.filter { it.level == null }
    if (missing.isEmpty()) return

    // 비어 있는 시간이 끝에 몰려 있으면 하나씩 적지 않고 'N시 이후' 로 줄인다.
    // 영업이 끝난 뒤라 비는 경우가 많은데, 다 적으면 한 줄이 넘어간다
    val tail = place.hours.takeLastWhile { it.level == null }
    val text = if (tail.size == missing.size && missing.size > 2) {
        "${tail.first().hour}시 이후는 아직 정보가 없어요"
    } else {
        "${missing.joinToString(" · ") { "${it.hour}시" }}는 아직 정보가 없어요"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = InkMuted,
        modifier = modifier,
    )
}

/** 사업장이 직접 등록한 시간이라는 표시. AI 예측과 구분해야 한다 */
@Composable
private fun BusinessBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .outlineBorder(LineStrong, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "사업장 등록",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
    }
}
