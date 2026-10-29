package com.calmmap.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/**
 * 피그마 최종안의 작은 공통 요소들.
 *   InfoButton      ⓘ 20 — 누르면 파래지고 아래에 InfoNote(파란 설명 상자)를 펼친다
 *   ActionChip      회색 알약 + 아이콘 — "음악 끔" 같은 사업장 조치
 *   FrequencyMeter  소리 빈도 막대 4칸 (0~4)
 *   TooltipBubble   진한 말풍선 + 아래 꼬리 — 그래프 막대를 눌렀을 때
 *   BottomCta       화면 맨 아래 고정 버튼 줄 (흰 바탕 + 위 구분선)
 */

/** ⓘ. 설명이 펼쳐져 있으면([open]) 파랗게 칠해 눌린 상태를 알린다. 설명은 [InfoNote] 로 보여준다 */
@Composable
fun InfoButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "설명 보기") {
    // 아이콘은 20 이지만 누르는 영역은 32
    Box(
        modifier = modifier
            .size(32.dp)
            .pressHighlight(shape = Radius.pill, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CalmIcon(CalmIcons.Info, size = 20.dp, tint = if (open) BrandPrimary else TextTertiary, contentDescription = contentDescription)
    }
}

/** ⓘ 를 눌렀을 때 펼쳐지는 설명. 다른 글과 섞이지 않게 파란 상자에 담는다 */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = BrandPrimary,
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.tile)
            .background(BrandPrimarySoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
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
fun BottomCta(modifier: Modifier = Modifier, note: String? = null, content: @Composable RowScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface)
            .topLine(BorderDefault)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 버튼을 왜 못 누르는지 같은 한 줄
        if (note != null) {
            Text(text = note, style = MaterialTheme.typography.caption, color = TextTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
