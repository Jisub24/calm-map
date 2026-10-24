package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

/**
 * 피그마 최종안 Chip. 눌러서 켜고 끄는 알약 (높이 36).
 *
 *   꺼짐  흰 바탕 + 회색 테두리 + 회색 글자
 *   켜짐  진한 바탕 + 흰 글자 (굵게)
 *
 * 지도 위에 떠 있을 때는 floating = true 로 그림자를 넣는다. 지도 그림 위에서 칩이 묻히지 않게.
 * 읽어 주는 기능에도 켜짐이 전달되게 toggleableState 를 붙인다. 색만으로는 전달되지 않는다.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
    trailingIcon: CalmIcons? = null,
) {
    val fg = if (selected) BgSurface else TextSecondary
    Row(
        modifier = modifier
            .height(Height.chip)
            .then(if (floating) Modifier.calmShadow(Shadows.card, Radius.pill) else Modifier)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Radius.pill)
            .background(if (selected) TextPrimary else BgSurface)
            .then(if (selected || floating) Modifier else Modifier.border(1.dp, BorderStrong, Radius.pill))
            .padding(horizontal = 14.dp)
            .semantics { toggleableState = if (selected) ToggleableState.On else ToggleableState.Off },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = fg,
        )
        if (trailingIcon != null) CalmIcon(trailingIcon, size = 16.dp, tint = fg)
    }
}
