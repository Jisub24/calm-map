package com.calmmap.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.LineFaint

/** 하단 탭에 들어가는 칸 */
enum class MainTab(val label: String) {
    MAP("지도"),
    HISTORY("기록"),
    ME("내 정보"),
}

/**
 * 화면 맨 아래 탭 세 개.
 *
 * 지도 · 하단바 · 검색 화면에 붙는다.
 * 장소 상세와 정보 부족 화면에는 붙이지 않는다. 그쪽은 뒤로 가기로 돌아오는
 * 화면이라 탭을 같이 두면 어디로 가는 건지 헷갈린다.
 *
 * 측정은 탭이 아니라 지도 위 버튼으로 들어간다. 측정은 장소를 정한 뒤에
 * 하는 일이라 아무 때나 누르는 탭에 두면 "어디를 측정할지"부터 물어야 한다.
 */
@Composable
fun BottomTabBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topLine(LineFaint)
            .height(62.dp),
    ) {
        MainTab.entries.forEach { tab ->
            TabItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) Ink else InkMuted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressFade(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (tab) {
            MainTab.MAP -> MapTabIcon(size = 20.dp, color = tint)
            MainTab.HISTORY -> ListTabIcon(size = 20.dp, color = tint)
            MainTab.ME -> PersonTabIcon(size = 20.dp, color = tint)
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
            modifier = Modifier.height(16.dp),
        )
    }
}
