package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper

/**
 * 화면 아래쪽 버튼 두 종류.
 *
 * 채운 것과 테두리만 있는 것으로 어느 쪽이 주된 동작인지 보여준다.
 * 색은 쓰지 않는다. 단계 표시에 쓰는 진하기와 섞이면 안 되기 때문이다.
 *
 * 높이를 48dp 로 둔 것은 손가락으로 누를 수 있는 최소 크기(44dp)보다
 * 크게 잡기 위해서다. 보호자가 아이를 안고 한 손으로 쓰는 상황을 생각하면
 * 더 작게 만들 수 없다.
 */

private val Shape = RoundedCornerShape(8.dp)

/** 주된 동작. 방문 계획 · 측정 요청 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Shape)
            .background(LineStrong),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Paper,
        )
    }
}

/** 곁들이는 동작. 상세 보기 · 이 장소 측정 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Shape)
            .outlineBorder(LineStrong, Shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
    }
}

/**
 * 목록 안에 들어가는 작은 버튼. 검색 결과의 '측정 요청' 에 쓴다.
 *
 * 높이 34dp 로 줄였지만 좌우 여백을 넉넉히 둬서 누를 면적은 확보했다.
 * 목록 한 줄 안에 들어가야 해서 48dp 를 쓸 수 없는 자리다.
 */
@Composable
fun SmallOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .height(34.dp)
            .pressFade(role = Role.Button, enabled = enabled, onClick = onClick)
            .clip(shape)
            .outlineBorder(if (enabled) LineStrong else Line, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Ink else InkMuted,
        )
    }
}
