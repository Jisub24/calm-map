package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.TextTertiary

/** 하단 탭에 들어가는 칸 */
enum class MainTab(val label: String, val icon: CalmIcons) {
    MAP("지도", CalmIcons.Map),
    HISTORY("기록", CalmIcons.History),
    ME("내 정보", CalmIcons.User),
}

/**
 * 피그마 최종안 BottomNav (높이 64). 지도 · 기록 · 내 정보.
 * 고른 탭은 아이콘과 글자가 파랗고, 나머지는 회색이다.
 *
 * 장소 상세와 정보 부족 화면에는 붙이지 않는다. 뒤로 가기로 돌아오는 화면이라
 * 탭을 같이 두면 어디로 가는 건지 헷갈린다.
 * 측정은 탭이 아니라 지도 위 버튼으로 들어간다. 장소를 정한 뒤에 하는 일이라서다.
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
            .background(BgSurface)
            .topLine(BorderDefault)
            // 흰 바탕은 시스템 내비게이션 바 아래까지 깔고, 탭은 그 위에 놓는다
            .navigationBarsPadding()
            .height(Height.bottomNav)
            .padding(start = 36.dp, end = 36.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            val tint = if (tab == selected) BrandPrimary else TextTertiary
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .pressFade(role = Role.Tab) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                CalmIcon(tab.icon, tint = tint)
                Text(text = tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}
