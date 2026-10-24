package com.calmmap.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel

/**
 * 피그마 최종안 LevelBadge. 장소의 단계를 보여주는 알약 배지 (높이 23).
 *
 * 지도 홈 목록 · 장소 카드 · 검색 결과 · 정보 부족 화면에서 모두 이것을 쓴다.
 * 네 곳에 따로 만들면 단계 색을 바꿀 때 한 군데를 빠뜨리게 된다.
 *
 *   무던함 · 보통 · 부담 높음   채워서 그림     — 관측으로 확인된 값
 *   후보                      흰 바탕 + 실선  — 추정만 있는 값
 *   정보 부족                  흰 바탕 + 점선  — 측정이 없음
 */
@Composable
fun LevelBadge(
    level: SensoryLevel,
    modifier: Modifier = Modifier,
    /** '지금 보통' 처럼 앞에 말을 붙일 때 쓴다 */
    prefix: String? = null,
) {
    val text = if (prefix == null) level.label else "$prefix ${level.label}"
    Row(
        modifier = modifier
            .height(Height.badge)
            .clip(Radius.pill)
            .then(
                if (level.fill != null) Modifier.background(level.fill)
                else Modifier
                    .background(BgSurface)
                    .outlineBorder(level.edge!!, Radius.pill, dashed = level.dashed),
            )
            .padding(start = 9.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelGlyph(level)
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = level.onFill)
    }
}
