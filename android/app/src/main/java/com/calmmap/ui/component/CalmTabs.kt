package com.calmmap.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안 Tabs (높이 44). 밑줄로 고른 탭을 표시한다.
 * 장소 상세의 시간대별 · 점수 · 소리 · 빛 · 사업장.
 */
@Composable
fun CalmTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Height.tab)
            .bottomLine(BorderDefault),
    ) {
        tabs.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { if (on) it.bottomLine(TextPrimary, 2.dp) else it }
                    .pressFade(role = Role.Tab) { onSelect(i) }
                    .semantics { selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) TextPrimary else TextTertiary,
                )
            }
        }
    }
}
