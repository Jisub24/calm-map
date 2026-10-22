package com.calmmap.ui.measure

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.content.ContextCompat
import com.calmmap.data.sound.SoundGroup
import com.calmmap.data.sound.SoundMeter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.FlickerAnswer
import com.calmmap.ui.model.MEASURE_SECONDS
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.SoundScore
import com.calmmap.ui.model.sampleMeasureCount
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlinx.coroutines.launch

// D2 측정 중: 1분 동안 마이크로 듣고 소리 크기와 소리 종류(YAMNet)를 보여준다
// 1분이 되면 저절로 끝난다. 원래 소리는 분석만 하고 바로 버린다
@Composable
fun MeasuringScreen(
    place: PlaceUi,
    onBack: () -> Unit,
    onFinish: (MeasureResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    // 권한 창에 답하기 전에는 안내 상자를 띄우지 않는다
    var micAsked by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMic = it
        micAsked = true
    }

    var seconds by remember { mutableIntStateOf(0) }
    // 첫 1초가 분석되기 전에는 null
    var db by remember { mutableStateOf<Int?>(null) }
    var sounds by remember { mutableStateOf(SoundGroup.entries.map { SoundScore(it.label, 0f) }) }
    var flicker by remember { mutableStateOf<FlickerAnswer?>(null) }
    var announceTaps by remember { mutableIntStateOf(0) }
    // 마지막으로 센 때. 방송 한 번에 여러 번 눌러도 한 번으로 센다
    var lastAnnounceAt by remember { mutableIntStateOf(-ANNOUNCE_MERGE_SEC) }
    // 끝날 때 평균을 내려고 매초 값을 더해 둔다
    val dbHistory = remember { mutableListOf<Int>() }
    val labelSums = remember { mutableMapOf<String, Float>() }
    val groupSums = remember { mutableMapOf<SoundGroup, Float>() }

    fun finish() {
        val n = dbHistory.size.coerceAtLeast(1)
        onFinish(
            MeasureResult(
                place = place,
                countBefore = sampleMeasureCount(place.id),
                db = if (dbHistory.isEmpty()) 0 else dbHistory.average().toInt(),
                sounds = SoundGroup.entries
                    .map { SoundScore(it.label, (groupSums[it] ?: 0f) / n) }
                    .sortedByDescending { it.score },
                // 거의 안 들린 라벨(0.01 미만)은 뺀다
                rawLabels = labelSums.mapValues { it.value / n }.filterValues { it >= 0.01f },
                flicker = flicker,
                endedAt = LocalDateTime.now(),
                seconds = seconds,
                announceTaps = announceTaps,
            ),
        )
    }

    // 설정 화면에서 권한을 켜고 돌아왔을 때 알아채게 다시 확인한다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    // 권한이 없으면 들어오자마자 묻는다
    LaunchedEffect(Unit) {
        if (!hasMic) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 권한이 생기면 듣기 시작. 화면을 나가면 마이크도 꺼진다
    LaunchedEffect(hasMic) {
        if (!hasMic) return@LaunchedEffect
        launch {
            @SuppressLint("MissingPermission") // 바로 위에서 권한을 확인했다
            val readings = SoundMeter(context).readings()
            readings.collect { reading ->
                db = reading.db
                dbHistory.add(reading.db)
                reading.labelScores.forEach { (label, score) ->
                    labelSums[label] = (labelSums[label] ?: 0f) + score
                }
                val groups = SoundGroup.scoresOf(reading.labelScores)
                groups.forEach { (group, score) -> groupSums[group] = (groupSums[group] ?: 0f) + score }
                sounds = SoundGroup.entries.map { SoundScore(it.label, groups[it] ?: 0f) }
            }
        }
        while (seconds < MEASURE_SECONDS) {
            delay(1000)
            seconds++
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
            if (micAsked && !hasMic) {
                MicNeeded(
                    onRetry = {
                        // 한 번 거절하면 안드로이드가 권한 창을 더 띄워주지 않을 수 있다.
                        // 그럴 때는 버튼이 아무 일도 안 하는 것처럼 보이니 앱 설정 화면을 연다
                        val activity = context.findActivity()
                        val canAsk = activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
                        if (canAsk) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                            )
                        }
                    },
                )
            }
            LoudnessBox(db = db)
            Text(
                text = "지금 들리는 소리 (YAMNet)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkSoft,
            )
            SoundBars(sounds = sounds)
            AnnounceBox(
                taps = announceTaps,
                onTap = {
                    if (seconds - lastAnnounceAt >= ANNOUNCE_MERGE_SEC) {
                        announceTaps++
                        lastAnnounceAt = seconds
                    }
                },
            )
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

// 마이크 권한을 거절했을 때
@Composable
private fun MicNeeded(onRetry: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PaperBox)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "마이크 권한이 있어야 측정할 수 있어요", style = MaterialTheme.typography.titleSmall, color = Ink)
        Text(
            text = "소리는 폰 안에서 분석만 하고 저장하지 않아요. 다시 물어보지 않으면 설정 > 앱 > 무던한 지도에서 켤 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
        )
        SecondaryButton(text = "마이크 허용하기", onClick = onRetry)
    }
}

// 상대 소리 크기를 크게
@Composable
private fun LoudnessBox(db: Int?) {
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
        Text(text = if (db == null) "– dB" else "$db dB", fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(text = "보정 전 값 · 같은 기기 비교용", style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}

/** 이 안에 또 누르면 같은 방송으로 본다. 안내 한 번이 보통 5초쯤이다 */
private const val ANNOUNCE_MERGE_SEC = 5

// 안내방송은 YAMNet 이 대화와 구분하지 못해서 직접 누른다
@Composable
private fun AnnounceBox(taps: Int, onTap: () -> Unit) {
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
            text = "안내방송이 나오면 눌러주세요 (수동 입력)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        SecondaryButton(
            text = if (taps == 0) "방송 나왔어요" else "방송 나왔어요 · ${taps}번",
            onClick = onTap,
        )
        Text(
            text = "방송 한 번에 한 번만 세요. 5초 안에 다시 누르면 같은 방송으로 봐요.",
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
        )
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

// Compose 의 context 에서 화면(Activity)을 꺼낸다
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
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
