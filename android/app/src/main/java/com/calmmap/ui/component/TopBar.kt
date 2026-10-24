package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary

/**
 * 피그마 최종안 TopBar (높이 56). ← 40dp 버튼 + Heading/18 Bold 제목.
 * onBack 을 넘기면 왼쪽에 뒤로가기 화살표가 생긴다. actions 는 오른쪽 끝에 붙는다.
 * 회색 바탕 화면(BgCanvas)에서는 background 를 넘겨 바탕과 맞춘다.
 */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    background: Color = BgSurface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            // 상태바(시계 · 배터리 줄) 밑으로 내려서 겹치지 않게 한다
            .statusBarsPadding()
            .height(Height.topBar)
            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // 화살표는 24 지만 누르는 영역은 40, 누르는 동안 동그랗게 진해진다
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pressHighlight(shape = Radius.pill, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                CalmIcon(CalmIcons.ArrowLeft, contentDescription = "뒤로")
            }
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        actions()
    }
}
