package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextTertiary

/**
 * 피그마 최종안 SearchBar. 지도 위에 떠 있는 흰 알약 검색창 (높이 48 · Float 그림자).
 *
 * 여기서 글자를 직접 받지 않는다. 누르면 검색 화면으로 넘어가고 입력은 거기서 한다.
 * 지도 위에서 글자판이 올라오면 지도가 반쯤 가려지고, 그 상태로 결과를 고르기 어렵다.
 * 그래서 입력칸처럼 보이지만 실제로는 버튼이다 (Role.Button).
 */
@Composable
fun SearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "장소·주소 검색",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Height.search)
            .calmShadow(Shadows.float, Radius.pill)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Radius.pill)
            .background(BgSurface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(CalmIcons.Search, size = 20.dp, tint = TextTertiary)
        Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
    }
}
