package com.calmmap.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.Line

/**
 * 지도 홈 위쪽의 검색창.
 *
 * 여기서 글자를 직접 받지 않는다. 누르면 검색 화면으로 넘어가고
 * 입력은 거기서 한다. 지도 위에서 글자판이 올라오면 지도가 반쯤 가려지고,
 * 그 상태로 결과를 고르기 어렵다.
 *
 * 그래서 입력칸처럼 보이지만 실제로는 버튼이다.
 * 읽어 주는 기능에도 버튼으로 알리려고 Role.Button 을 붙인다.
 */
@Composable
fun SearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "장소 검색",
) {
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(shape)
            .outlineBorder(color = Line, shape = shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon(size = 15.dp, color = InkMuted)
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
