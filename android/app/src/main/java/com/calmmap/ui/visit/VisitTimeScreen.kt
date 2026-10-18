package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.VisitDay
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.model.upcomingVisitDays
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.SensoryLevel

// 고른 시간을 눈에 띄게 하는 바탕 (와이어프레임 그대로)
private val SelectedBox = Color(0xFFFFFBE8)

// C1 방문 시간 선택: 날짜와 시간을 고르고 "이 시간으로 정하기"
@Composable
fun VisitTimeScreen(
    place: PlaceUi,
    nowHour: Int,
    onBack: () -> Unit,
    onConfirm: (day: VisitDay, hour: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember { upcomingVisitDays() }
    var day by remember { mutableStateOf(days.first()) }
    // 처음엔 지금 이후 무던한 첫 시간을 골라 둔다. 없으면 정보가 있는 첫 시간
    var hour by remember {
        val bars = place.hours
        val first = bars.firstOrNull { it.hour >= nowHour && it.level == SensoryLevel.CALM }
            ?: bars.firstOrNull { it.level != null }
        mutableIntStateOf(first?.hour ?: nowHour)
    }
    var reminderOn by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "언제 갈까요?", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = place.name, style = MaterialTheme.typography.bodyMedium, color = InkMuted)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach {
                    FilterChip(label = it.label, selected = it == day, onClick = { day = it })
                }
            }

            // TODO: 지금은 날짜와 상관없이 오늘 그래프를 보여준다. hourly_forecast 를 붙이면 요일별로
            HourlyBarChart(
                bars = place.hours,
                nowHour = nowHour,
                selectedHour = hour,
                onBarClick = { if (it.level != null) hour = it.hour },
            )

            SelectedSummary(place = place, day = day, hour = hour)

            // TODO: 실제 알림은 알림 권한과 예약을 붙인 뒤
            ReminderRow(on = reminderOn, onToggle = { reminderOn = !reminderOn })
        }

        PrimaryButton(
            text = "이 시간으로 정하기",
            onClick = { onConfirm(day, hour) },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )
    }
}

// 고른 시간 요약: "오늘 15:00" + 그 시간 단계
@Composable
private fun SelectedSummary(place: PlaceUi, day: VisitDay, hour: Int) {
    val bar = place.hours.firstOrNull { it.hour == hour }
    val detail = when {
        bar?.level == null -> "이 시간은 정보가 없어요"
        bar.estimated -> "${bar.level.label} 예상 · 추정값이에요"
        else -> "${bar.level.label} 예상 · 측정으로 확인했어요"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SelectedBox)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "${day.label} ${"%02d".format(hour)}:00", style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(text = detail, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
    }
}

// 출발 30분 전 알림 켜고 끄기
@Composable
private fun ReminderRow(on: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Switch, onClick = onToggle)
            .clip(shape)
            .border(1.dp, LineSoft, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "출발 30분 전 알림", style = MaterialTheme.typography.bodyMedium, color = Ink)
        Text(
            text = if (on) "켜짐 ●" else "꺼짐 ○",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (on) LineStrong else InkMuted,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VisitTimeScreenPreview() {
    CalmMapTheme {
        VisitTimeScreen(place = samplePlaces[0], nowHour = 13, onBack = {}, onConfirm = { _, _ -> })
    }
}
