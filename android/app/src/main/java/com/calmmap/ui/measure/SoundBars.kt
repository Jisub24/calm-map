package com.calmmap.ui.measure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.calmmap.ui.model.SoundScore
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LineFaint
import java.util.Locale

// 소리 종류별 점수 막대. 측정 중과 측정 결과가 같이 쓴다
@Composable
internal fun SoundBars(sounds: List<SoundScore>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sounds.forEach { SoundBar(it) }
    }
}

// 한 줄: 이름 · 막대 · 점수
@Composable
private fun SoundBar(sound: SoundScore) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = sound.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
            modifier = Modifier.width(80.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(shape)
                .background(LineFaint),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sound.score.coerceIn(0f, 1f))
                    .clip(shape)
                    .background(LevelHigh),
            )
        }
        Text(
            text = String.format(Locale.US, "%.2f", sound.score),
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
    }
}
