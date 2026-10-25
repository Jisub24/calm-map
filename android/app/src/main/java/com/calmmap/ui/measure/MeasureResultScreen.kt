package com.calmmap.ui.measure

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.style.TextAlign
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.GhostButton
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.caption
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.AnnounceAnswer
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.sampleSoundBase
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BgSubtle
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
    // 소리 종류 점수 상위 4개를 칸으로. 가장 높은 칸은 파랗게 (주 요인)
    val top = result.sounds.take(4)
    var announceAnswer by remember { mutableStateOf<AnnounceAnswer?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "측정 완료", background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "이번 측정", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.weight(1f))
                    LevelBadge(level = result.level)
                }
                Text(text = whenLabel(result) + " · 평균 ${result.db} dB", style = MaterialTheme.typography.caption, color = TextTertiary)
                top.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { sound ->
                            ScoreTile(
                                label = sound.label,
                                value = (sound.score * 100).toInt(),
                                main = sound == top.first(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            AnnounceResult(
                taps = result.announceTaps,
                answer = announceAnswer,
                onAnswer = { announceAnswer = it },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalmIcon(CalmIcons.Check, size = 18.dp, tint = BrandPrimary)
                Text(
                    text = "이 장소 측정 ${result.countBefore}회 → ${result.countBefore + 1}회. 3회부터 '확정'으로 보여드려요",
                    style = MaterialTheme.typography.caption,
                    color = TextSecondary,
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "지도에는 잠시 후(약 20분 안) 반영돼요",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            PrimaryButton(
                text = "저장하기",
                onClick = { onSave(result.copy(announceAnswer = announceAnswer)) },
            )
            GhostButton(text = "버리기", onClick = onDiscard)
        }
    }
}

// 점수 칸. 주 요인은 연한 파랑 + 파란 글자
@Composable
private fun ScoreTile(label: String, value: Int, main: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Radius.tile)
            .background(if (main) BrandPrimarySoft else BgSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.caption, color = if (main) BrandPrimary else TextTertiary)
        Text(text = "$value", style = MaterialTheme.typography.titleLarge, color = if (main) BrandPrimary else TextPrimary)
    }
}

/**
 * 측정 중에 누른 안내방송 횟수.
 * 한 번도 안 눌렀으면 방송이 없었던 건지 못 누른 건지 물어본다. 답을 안 하면 미측정으로 둔다
 */
@Composable
private fun AnnounceResult(taps: Int, answer: AnnounceAnswer?, onAnswer: (AnnounceAnswer) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (taps > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CalmIcon(CalmIcons.Volume, size = 18.dp, tint = TextSecondary)
                Text(text = "안내방송 ${taps}번 (직접 표시)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            }
        } else {
            Text(text = "측정하는 동안 안내방송이 있었나요?", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnounceAnswer.entries.forEach {
                    FilterChip(label = it.label, selected = it == answer, onClick = { onAnswer(it) })
                }
            }
        }
    }
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
