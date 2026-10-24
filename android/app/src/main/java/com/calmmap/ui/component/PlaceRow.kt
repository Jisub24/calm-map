package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/**
 * 피그마 최종안 PlaceRow. 아이콘 타일 · 이름 · 근거 설명 · 단계 배지.
 *
 * 지도 홈 목록과 검색 결과가 같은 모양을 쓴다.
 * 설명 자리에 "도보 4분 · 14시부터 무던해요" 처럼 **근거까지** 적는 것이 중요하다.
 * 단계만 보여주면 왜 그런지 알 수 없고, 측정이 부족한 곳과 확인된 곳을 구분할 수 없다.
 *
 * trailing 을 넘기면 배지 대신 그것을 둔다 (검색 결과의 '+ 측정 요청').
 */
@Composable
fun PlaceRow(
    name: String,
    description: String,
    level: SensoryLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 8.dp,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .let { if (showDivider) it.bottomLine(BorderDefault) else it }
            .padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Radius.tile)
                .background(BgSubtle),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(CalmIcons.Map, size = 20.dp, tint = TextTertiary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = name, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = description, style = MaterialTheme.typography.caption, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing() else LevelBadge(level = level)
    }
}
