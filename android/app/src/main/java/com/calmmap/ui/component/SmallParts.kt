package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안의 작은 공통 요소들.
 *   InfoButton      ⓘ 20 — 누르면 설명 시트
 *   ActionChip      회색 알약 + 아이콘 — "음악 끔" 같은 사업장 조치
 *   FrequencyMeter  소리 빈도 막대 4칸 (0~4)
 *   TooltipBubble   진한 말풍선 + 아래 꼬리 — 그래프 막대를 눌렀을 때
 *   BottomCta       화면 맨 아래 고정 버튼 줄 (흰 바탕 + 위 구분선)
 */

@Composable
fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "설명 보기") {
    // 보이는 원은 20 이지만 누르는 영역은 44 로 넓힌다
    Box(
        modifier = modifier
            .size(44.dp)
            .pressFade(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(Radius.pill)
                .background(BgSurface)
                .border(1.5.dp, BorderStrong, Radius.pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "i", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextTertiary)
        }
    }
}

@Composable
fun ActionChip(label: String, icon: CalmIcons, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(BgSubtle)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(icon, size = 14.dp)
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

/** level 0~4. 칸 높이 5 · 8 · 11 · 14, 켜진 칸은 진한 회색 */
@Composable
fun FrequencyMeter(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 26.dp, height = 14.dp)) {
        val w = 5.dp.toPx()
        val gap = 2.dp.toPx()
        val r = CornerRadius(1.5.dp.toPx())
        listOf(5, 8, 11, 14).forEachIndexed { i, h ->
            val hp = h.dp.toPx()
            drawRoundRect(
                color = if (i < level) TextSecondary else LevelCalm,
                topLeft = Offset(i * (w + gap), size.height - hp),
                size = Size(w, hp),
                cornerRadius = r,
            )
        }
    }
}

@Composable
fun TooltipBubble(title: String, body: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .calmShadow(Shadows.tooltip, Radius.tooltip)
                .clip(Radius.tooltip)
                .background(TextPrimary)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BgSurface)
            if (body != null) {
                Text(text = body, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = BgSurface)
            }
        }
        // 아래로 향한 꼬리 10×5
        Canvas(modifier = Modifier.size(width = 10.dp, height = 5.dp)) {
            drawPath(
                Path().apply {
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
                },
                color = TextPrimary,
            )
        }
    }
}

/** 화면 맨 아래 버튼 줄. 두 개를 나란히 둘 때는 각 버튼에 Modifier.weight(1f) */
@Composable
fun BottomCta(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface)
            .topLine(BorderDefault)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
