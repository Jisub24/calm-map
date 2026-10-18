package com.calmmap.ui.measure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.model.FlickerAnswer
import com.calmmap.ui.model.MEASURE_SECONDS
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.SoundScore
import com.calmmap.ui.model.sampleMeasureCount
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.model.sampleSoundBase
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.random.Random

// D2 측정 중: 1분 동안 소리 크기와 소리 종류를 보여준다. 1분이 되면 저절로 끝난다
// TODO: 지금은 예시 값이 조금씩 흔들리는 것. 마이크(dB)와 YAMNet 을 붙이면 실제 값으로
@Composable
fun MeasuringScreen(
    place: PlaceUi,
    onBack: () -> Unit,
    onFinish: (MeasureResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var seconds by remember { mutableIntStateOf(0) }
    var db by remember { mutableIntStateOf(66) }
    val sounds = remember { mutableStateListOf<SoundScore>().apply { addAll(sampleSoundBase) } }
    // 평균을 내려고 매초 소리 크기를 모은다
    val dbHistory = remember { mutableListOf<Int>() }
    var flicker by remember { mutableStateOf<FlickerAnswer?>(null) }

    fun finish() {
        onFinish(
            MeasureResult(
                place = place,
                countBefore = sampleMeasureCount(place.id),
                db = if (dbHistory.isEmpty()) db else dbHistory.average().toInt(),
                sounds = sounds.sortedByDescending { it.score },
                flicker = flicker,
                endedAt = LocalDateTime.now(),
                seconds = seconds,
            ),
        )
    }

    // 1초마다 시간과 예시 값을 바꾼다
    LaunchedEffect(Unit) {
        while (seconds < MEASURE_SECONDS) {
            delay(1000)
            seconds++
            db = (db + Random.nextInt(-2, 3)).coerceIn(58, 74)
            dbHistory.add(db)
            sampleSoundBase.forEachIndexed { i, base ->
                sounds[i] = base.copy(score = (base.score + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0f, 1f))
            }
        }
        finish()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = place.name, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoudnessBox(db = db)
            Text(
                text = "지금 들리는 소리 (YAMNet)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkSoft,
            )
            SoundBars(sounds = sounds)
            FlickerBox(answer = flicker, onAnswer = { flicker = it })
            Text(text = "🔒 소리 종류·크기·시각만 전송해요", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }

        Text(
            text = "${formatTime(seconds)} / ${formatTime(MEASURE_SECONDS)}",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        PrimaryButton(
            text = "측정 끝내기",
            onClick = { finish() },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )
    }
}

// 상대 소리 크기를 크게
@Composable
private fun LoudnessBox(db: Int) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(PaperBox)
            .border(1.dp, LineSoft, shape),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "상대 소리 크기", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Text(text = "$db dB", fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(text = "보정 전 값 · 같은 기기 비교용", style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}

// 조명 깜빡임은 소리로 알 수 없어서 직접 고른다
@Composable
private fun FlickerBox(answer: FlickerAnswer?, onAnswer: (FlickerAnswer) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, LineSoft, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "조명이 깜빡이나요? (수동 입력)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlickerAnswer.entries.forEach {
                FilterChip(label = it.label, selected = it == answer, onClick = { onAnswer(it) })
            }
        }
    }
}

/** 42 → "0:42" */
internal fun formatTime(seconds: Int): String = "${seconds / 60}:${"%02d".format(seconds % 60)}"

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MeasuringScreenPreview() {
    CalmMapTheme {
        MeasuringScreen(place = samplePlaces[1], onBack = {}, onFinish = {})
    }
}
