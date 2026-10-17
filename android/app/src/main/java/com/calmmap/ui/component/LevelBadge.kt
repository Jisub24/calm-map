package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.SensoryLevel

/**
 * 장소의 단계를 보여주는 알약 모양 배지.
 *
 * 지도 홈 목록 · 하단바 · 검색 결과 · 정보 부족 화면에서 모두 이것을 쓴다.
 * 네 곳에 따로 만들면 '무던함' 진하기를 바꿀 때 한 군데를 빠뜨리게 된다.
 *
 * 채움과 테두리가 단계마다 다른 것은 의도한 것이다.
 *   무던함 · 보통 · 부담 높음   채워서 그림     — 관측으로 확인된 값
 *   후보 · 정보 부족           채우지 않음     — 확인되지 않은 값
 */
@Composable
fun LevelBadge(
    level: SensoryLevel,
    modifier: Modifier = Modifier,
    /** '지금 보통' 처럼 앞에 말을 붙일 때 쓴다 */
    prefix: String? = null,
) {
    val shape = RoundedCornerShape(percent = 50)
    val text = if (prefix == null) level.label else "$prefix ${level.label}"

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 22.dp)
            .clip(shape)
            .let { base ->
                // 채우는 단계는 배경을, 채우지 않는 단계는 테두리를 그린다
                if (level.fill != null) base.background(level.fill, shape)
                else base.outlineBorder(color = InkMuted, shape = shape, dashed = level.dashed)
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = level.onFill,
        )
    }
}
