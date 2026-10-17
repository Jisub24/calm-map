package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper

/**
 * 눌러서 켜고 끄는 칩.
 *
 * 쓰는 곳
 *   지도 홈    지금 · 시간 선택 · 우리 아이 맞춤
 *   검색       가까운 순 · 무던한 시간 있는 곳만
 *   장소 상세   점수 구성 · 소리 · 빛 · 무던한 시간
 *
 * 켜진 것과 꺼진 것을 바탕 진하기로 구분한다.
 * 읽어 주는 기능(TalkBack)을 쓰는 사람에게도 켜짐 여부가 전달되도록
 * toggleableState 를 같이 붙인다. 색만으로는 전달되지 않는다.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .height(32.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(shape)
            .let { base ->
                if (selected) base.background(LineStrong, shape)
                else base.outlineBorder(color = Line, shape = shape)
            }
            .padding(horizontal = 12.dp)
            .semantics {
                toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Paper else Ink,
        )
    }
}
