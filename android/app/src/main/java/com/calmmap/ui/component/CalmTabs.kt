package com.calmmap.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
 *
 * 탭은 폭을 똑같이 나눠 쓴다. 폰 글자 크기를 키워 이름이 칸보다 길어지면 두 줄로 꺾지 않고
 * 그 탭만 넓어지고 탭 줄을 옆으로 넘긴다.
 */
@Composable
fun CalmTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(Height.tab)
            .bottomLine(BorderDefault),
    ) {
        val share = maxWidth / tabs.size
        Row(modifier = Modifier.fillMaxHeight().horizontalScroll(rememberScrollState())) {
            tabs.forEachIndexed { i, label ->
                val on = i == selectedIndex
                Box(
                    modifier = Modifier
                        .widthIn(min = share)
                        .fillMaxHeight()
                        .let { if (on) it.bottomLine(TextPrimary, 2.dp) else it }
                        .pressFade(role = Role.Tab) { onSelect(i) }
                        .semantics { selected = on }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) TextPrimary else TextTertiary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
