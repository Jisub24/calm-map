package com.calmmap.ui.visit

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.data.brief
import com.calmmap.ui.component.ActionChip
import com.calmmap.ui.component.BottomCta
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.HourlyBarChart
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.VisitDay
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.model.upcomingVisitDays
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// 최종안 11 방문 시간 선택: 날짜와 시간을 고르고 "오늘 15:00로 정하기"
// onConfirm 은 DB 에 쓰는 동안 기다린다. 실패하면 이 화면에 남아서 다시 누를 수 있다
@Composable
fun VisitTimeScreen(
    place: PlaceUi,
    nowHour: Int,
    onBack: () -> Unit,
    onConfirm: suspend (day: VisitDay, hour: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember { upcomingVisitDays() }
    var day by remember { mutableStateOf(days.first()) }
    // 처음엔 지금 이후 무던한 첫 시간을 골라 둔다. 없으면 정보가 있는 첫 시간
    var hour by remember {
        val first = place.hours.firstOrNull { it.hour >= nowHour && it.level == SensoryLevel.CALM }
            ?: place.hours.firstOrNull { it.level != null }
        mutableIntStateOf(first?.hour ?: nowHour)
    }
    var reminderOn by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val whenText = "${day.label} ${"%02d".format(hour)}:00"

    Column(modifier = modifier.fillMaxSize().background(BgCanvas)) {
        TopBar(title = "언제 갈까요?", onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CalmIcon(CalmIcons.Store, size = 16.dp, tint = TextSecondary)
                Text(text = place.name, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach {
                    FilterChip(label = dayChipLabel(it), selected = it == day, onClick = { day = it })
                }
            }

            WhiteCard {
                Text(text = "막대를 눌러 시간을 고르세요", style = MaterialTheme.typography.caption, color = TextTertiary)
                // TODO: 지금은 날짜와 상관없이 오늘 그래프. hourly_forecast 를 붙이면 요일별로
                HourlyBarChart(
                    bars = place.hours,
                    nowHour = nowHour,
                    selectedHour = hour,
                    pickMode = true,
                    onBarClick = { if (it.level != null) hour = it.hour },
                )
            }

            SelectedSummary(place = place, whenText = whenText, hour = hour)

            // TODO: 실제 알림은 알림 권한과 예약을 붙인 뒤
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalmIcon(CalmIcons.Bell, size = 20.dp, tint = TextSecondary)
                Text(
                    text = "출발 30분 전에 알려드릴게요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = reminderOn,
                    onCheckedChange = { reminderOn = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BrandPrimary,
                        checkedThumbColor = BgSurface,
                        uncheckedTrackColor = BorderStrong,
                        uncheckedThumbColor = BgSurface,
                        uncheckedBorderColor = BorderStrong,
                    ),
                )
            }
        }

        BottomCta {
            // 버튼 줄은 가로라서 안내 문구를 버튼 위에 두려고 세로로 한 번 감싼다
            Column(Modifier.weight(1f)) {
                saveError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.caption,
                        color = StatusCaution,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                PrimaryButton(
                    text = if (saving) "저장하는 중…" else "${whenText}로 정하기",
                    enabled = !saving,
                    onClick = {
                        scope.launch {
                            saving = true
                            saveError = null
                            try {
                                onConfirm(day, hour)
                            } catch (e: Exception) {
                                Log.w("VisitTime", "방문 저장 실패: ${e.brief()}")
                                saveError = "저장하지 못했어요. 인터넷 연결을 확인하고 다시 눌러 주세요"
                            } finally {
                                saving = false
                            }
                        }
                    },
                )
            }
        }
    }
}

/** "오늘 (금)" · "내일 (토)" · "일" · "11/7 (토)" */
private fun dayChipLabel(day: VisitDay): String =
    // 요일이 아닌 이름(오늘 · 내일 · 날짜)에는 요일을 붙인다
    if (day.label == "오늘" || day.label == "내일" || "/" in day.label) {
        "${day.label} (${day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
    } else {
        day.label
    }

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

// 고른 시간 요약: "오늘 15:00" + 단계 배지 + 요인 태그 + 근거
@Composable
private fun SelectedSummary(place: PlaceUi, whenText: String, hour: Int) {
    val bar = place.hours.firstOrNull { it.hour == hour }
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = whenText, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, modifier = Modifier.weight(1f))
            bar?.level?.let { LevelBadge(level = if (bar.estimated) SensoryLevel.CANDIDATE else it) }
        }
        // TODO: 요인별 예측(방송 · 혼잡 · 밝기)을 붙이면 실제 값으로. 지금은 단계에 맞춘 예시
        if (bar?.level != null) {
            val calm = bar.level == SensoryLevel.CALM
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip(label = if (calm) "방송 드묾" else "방송 가끔", icon = CalmIcons.Volume)
                ActionChip(label = if (calm) "혼잡 여유" else "혼잡 보통", icon = CalmIcons.Users)
                ActionChip(label = "밝기 보통", icon = CalmIcons.Sun)
            }
        }
        Text(
            text = when {
                bar?.level == null -> "이 시간은 아직 정보가 없어요"
                bar.estimated -> "측정이 없어 추정한 시간이에요"
                else -> "실측으로 확인된 시간이에요"
            },
            style = MaterialTheme.typography.caption,
            color = TextTertiary,
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
