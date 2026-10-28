package com.calmmap.ui.measure

import android.util.Log
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.Role
import com.calmmap.ui.component.pressHighlight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.GhostButton
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
import com.calmmap.ui.model.AnnounceLevel
import com.calmmap.ui.model.CrowdAnswer
import com.calmmap.ui.model.ENOUGH_MEASURES
import com.calmmap.ui.model.MIN_SAVE_SECONDS
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.sampleSoundBase
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.StatusCaution
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

// D3 측정 결과: 이번 측정 요약과 서버로 보낼 값을 보여주고 저장 / 버리기
// 저장할 때는 결과 화면에서 고른 안내방송 · 혼잡 답까지 넣은 결과를 넘긴다.
// onSave 는 DB 에 쓰는 동안 기다린다. 실패하면 이 화면에 남아서 다시 누를 수 있다
@Composable
fun MeasureResultScreen(
    result: MeasureResult,
    onSave: suspend (MeasureResult) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 소리 종류 점수 상위 4개를 칸으로. 가장 높은 칸은 파랗게 (주 요인)
    val top = result.sounds.take(4)
    var announceLevel by remember { mutableStateOf<AnnounceLevel?>(null) }
    var crowd by remember { mutableStateOf<CrowdAnswer?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    // ⓘ 를 누르면 소리 칸 숫자가 무슨 뜻인지 보여준다
    var showHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                // 단계 · 점수는 여기서 안 보여준다. 소리 크기만으로 붙이면 틀린 안내가 된다. 점수는 저장 뒤 서버가 낸다
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "이번 측정", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.weight(1f))
                    // 아이콘은 20 이지만 누르는 영역은 32
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .pressHighlight(shape = Radius.pill, role = Role.Button, onClick = { showHint = !showHint }),
                        contentAlignment = Alignment.Center,
                    ) {
                        CalmIcon(CalmIcons.Info, size = 20.dp, tint = if (showHint) BrandPrimary else TextTertiary, contentDescription = "숫자 설명")
                    }
                }
                Text(text = whenLabel(result) + " · 소리 크기 ${result.db} dB", style = MaterialTheme.typography.caption, color = TextTertiary)
                if (showHint) {
                    // 칸의 숫자는 점수가 아니다. 그 소리가 얼마나 자주 · 또렷하게 들렸는지(YAMNet 확신도 평균)를 0~100 으로 보여준다
                    Text(
                        text = "숫자는 점수가 아니에요. 측정하는 동안 그 소리가 얼마나 자주, 또렷하게 들렸는지 나타내요. 100에 가까울수록 자주 들린 소리예요.",
                        style = MaterialTheme.typography.caption,
                        color = BrandPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.tile)
                            .background(BrandPrimarySoft)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
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

            AnnounceQuestion(answer = announceLevel, onAnswer = { announceLevel = it })

            CrowdQuestion(answer = crowd, onAnswer = { crowd = it })

            // 몇 번째 측정인지. 3회가 모여야 지도에 단계가 확정으로 바뀌니 그 전까지만 알린다
            val nth = result.countBefore + 1
            if (nth < ENOUGH_MEASURES) {
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
                        text = "이 장소의 ${nth}번째 측정이에요. ${ENOUGH_MEASURES}회가 모이면 지도에 확정 단계로 보여드려요",
                        style = MaterialTheme.typography.caption,
                        color = TextSecondary,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = when {
                    !result.canSave -> "${MIN_SAVE_SECONDS / 60}분 이상 측정한 것만 저장할 수 있어요"
                    saveError != null -> saveError!!
                    else -> "지도에는 잠시 후(약 20분 안) 반영돼요"
                },
                style = MaterialTheme.typography.caption,
                color = if (saveError != null) StatusCaution else TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            PrimaryButton(
                text = if (saving) "저장하는 중…" else "저장하기",
                enabled = result.canSave && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        saveError = null
                        try {
                            onSave(result.copy(announceLevel = announceLevel, crowd = crowd))
                        } catch (e: Exception) {
                            Log.w("MeasureResult", "측정 저장 실패", e)
                            saveError = "저장하지 못했어요. 인터넷 연결을 확인하고 다시 눌러 주세요"
                        } finally {
                            saving = false
                        }
                    }
                },
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
 * 안내방송이 몇 번쯤 나왔나. YAMNet 이 못 잡아서 사람이 답한다. 답을 안 하면 미측정(비움)으로 둔다.
 * 고른 칩을 다시 누르면 풀린다
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnounceQuestion(answer: AnnounceLevel?, onAnswer: (AnnounceLevel?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "측정하는 동안 안내방송이 몇 번 나왔나요?", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnnounceLevel.entries.forEach {
                FilterChip(label = it.label, selected = it == answer, onClick = { onAnswer(if (it == answer) null else it) })
            }
        }
    }
}

/** 사람이 얼마나 있었나. 장소 혼잡 점수의 재료. 답을 안 하면 비운다. 고른 칩을 다시 누르면 풀린다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CrowdQuestion(answer: CrowdAnswer?, onAnswer: (CrowdAnswer?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "사람이 얼마나 있었나요?", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CrowdAnswer.entries.forEach {
                FilterChip(label = it.label, selected = it == answer, onClick = { onAnswer(if (it == answer) null else it) })
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
                endedAt = LocalDateTime.now(),
                seconds = 60,
            ),
            onSave = {},
            onDiscard = {},
        )
    }
}
