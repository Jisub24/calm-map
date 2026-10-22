package com.calmmap.ui.measure

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.AnnounceAnswer
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.sampleSoundBase
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

// D3 측정 결과: 이번 측정 요약과 서버로 보낼 값을 보여주고 저장 / 버리기
// 저장할 때는 결과 화면에서 고른 안내방송 답까지 넣은 결과를 넘긴다
@Composable
fun MeasureResultScreen(
    result: MeasureResult,
    onSave: (MeasureResult) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 화면에는 위 3개만. 나머지는 거의 안 들린 소리다
    val top = result.sounds.take(3)
    var announceAnswer by remember { mutableStateOf<AnnounceAnswer?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "측정 완료")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperBox)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "이번 측정: ${result.level.label}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                Text(text = whenLabel(result), style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }

            SectionLabel("주요 소리")
            SoundBars(sounds = top)
            AnnounceResult(
                taps = result.announceTaps,
                answer = announceAnswer,
                onAnswer = { announceAnswer = it },
            )
            Text(
                text = "이 장소 측정 ${result.countBefore}회 → ${result.countBefore + 1}회. 시간대 예측이 더 정확해져요.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                text = "저장하기",
                onClick = { onSave(result.copy(announceAnswer = announceAnswer)) },
            )
            SecondaryButton(text = "버리기", onClick = onDiscard)
        }
    }
}

/**
 * 측정 중에 누른 안내방송 횟수.
 * 한 번도 안 눌렀으면 방송이 없었던 건지 못 누른 건지 물어본다. 답을 안 하면 미측정으로 둔다
 */
@Composable
private fun AnnounceResult(taps: Int, answer: AnnounceAnswer?, onAnswer: (AnnounceAnswer) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, LineSoft, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (taps > 0) {
            Text(
                text = "안내방송 ${taps}번 (직접 표시)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
        } else {
            Text(
                text = "측정하는 동안 안내방송이 있었나요?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnounceAnswer.entries.forEach {
                    FilterChip(label = it.label, selected = it == answer, onClick = { onAnswer(it) })
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = InkSoft)
}

/** "화요일 14:12 · 1분" */
private fun whenLabel(result: MeasureResult): String {
    val day = result.endedAt.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    val time = "%02d:%02d".format(result.endedAt.hour, result.endedAt.minute)
    val length = if (result.seconds >= 60) "${result.seconds / 60}분" else "${result.seconds}초"
    return "$day $time · $length"
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MeasureResultScreenPreview() {
    CalmMapTheme {
        MeasureResultScreen(
            result = MeasureResult(
                place = samplePlaces[1],
                countBefore = 12,
                db = 66,
                sounds = sampleSoundBase,
                rawLabels = mapOf("Speech" to 0.48f, "Music" to 0.21f),
                flicker = null,
                endedAt = LocalDateTime.now(),
                seconds = 60,
            ),
            onSave = {},
            onDiscard = {},
        )
    }
}
