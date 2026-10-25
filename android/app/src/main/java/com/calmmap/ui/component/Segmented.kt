package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSunken
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안 Segmented. 둘 이상 중 하나를 고르는 알약 (높이 40).
 * 들어간 회색 바탕 위에 고른 칸만 흰 알약으로 떠 있다 (Card 그림자).
 *
 * 쓰는 곳: 장소 상세 '모두 기준 / 우리 아이 맞춤', 우리 아이 설정 '보통 / 민감 / 매우'.
 * 작은 자리에서는 height 를 줄여 쓴다.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = Height.segmented,
    /** 고른 칸 글자색. 우리 아이 설정의 '매우' 처럼 강조할 때 바꾼다 */
    selectedColor: (Int) -> Color = { TextPrimary },
    selectedBackground: (Int) -> Color = { BgSurface },
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(Radius.pill)
            .background(BgSunken)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (on) Modifier.calmShadow(Shadows.card, Radius.pill) else Modifier)
                    .clip(Radius.pill)
                    .background(if (on) selectedBackground(i) else Color.Transparent)
                    .pressFade(role = Role.RadioButton) { onSelect(i) }
                    .semantics { selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (height < Height.segmented) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = if (on) selectedColor(i) else TextTertiary,
                )
            }
        }
    }
}

/** 가로를 채우는 Segmented */
@Composable
fun FullSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = Segmented(options, selectedIndex, onSelect, modifier.fillMaxWidth())

/**
 * 목록 한 줄 오른쪽에 들어가는 작은 Segmented (글자 11, 둥근 12).
 * 우리 아이 설정의 '보통 / 민감 / 매우'. 고른 칸은 흰 바탕 + 파란 글자,
 * 강조할 칸(매우)은 selectedBackground · selectedColor 로 주의 색을 준다.
 */
@Composable
fun CompactSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: (Int) -> Color = { BrandPrimary },
    selectedBackground: (Int) -> Color = { BgSurface },
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(BgSubtle)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                modifier = Modifier
                    .then(if (on) Modifier.calmShadow(Shadows.card, shape) else Modifier)
                    .clip(shape)
                    .background(if (on) selectedBackground(i) else Color.Transparent)
                    .pressFade(role = Role.RadioButton) { onSelect(i) }
                    .semantics { selected = on }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) selectedColor(i) else TextTertiary,
                )
            }
        }
    }
}
