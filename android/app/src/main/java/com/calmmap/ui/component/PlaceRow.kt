package com.calmmap.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.SensoryLevel

/**
 * 장소 한 줄. 이름 · 설명 · 단계 배지.
 *
 * 지도 홈 아래 목록과 검색 결과가 같은 모양을 쓴다.
 * 줄 사이 여백만 다르다 (지도 홈 9 / 검색 12).
 *
 * 설명 자리에 "도보 4분 · 14시부터 무던해요 · 실측 확인" 처럼
 * **근거까지** 적는 것이 중요하다. 단계만 보여주면 왜 그런지 알 수 없고,
 * 측정이 부족한 곳과 확인된 곳을 구분할 수 없다.
 */
@Composable
fun PlaceRow(
    name: String,
    description: String,
    level: SensoryLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 9.dp,
    showDivider: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .let { if (showDivider) it.bottomLine(LineFaint) else it }
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
        LevelBadge(
            level = level,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
