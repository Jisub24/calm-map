package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.calmmap.ui.theme.BorderDefault

/**
 * 눌렀다는 것을 눈으로 보여주는 두 가지 방법.
 *
 * Compose 기본 물결 효과(ripple)를 쓰지 않는 이유:
 *   물결은 퍼졌다 사라지는 애니메이션이라 **손가락에 가려 안 보이는** 경우가 많다.
 *   특히 목록 한 줄이나 작은 아이콘에서는 거의 눈치채지 못한다.
 *   누르는 동안 계속 유지되는 표시가 "눌렀다" 를 더 분명하게 전한다.
 *
 * 누름 표시가 없으면 사용자는 **눌린 건지 앱이 멈춘 건지 구분할 수 없어**
 * 같은 자리를 여러 번 누르게 된다.
 */

/**
 * 누르는 동안 살짝 흐려진다. 글자 · 버튼 · 목록 줄에 쓴다.
 *
 * 색을 바꾸지 않고 흐리게만 해서, 단계 표시에 쓰는 진하기와 섞이지 않는다.
 *
 * **붙이는 순서가 중요하다. background · outlineBorder 보다 앞에 둔다.**
 *
 *   맞음   .pressFade(...).clip(shape).background(색)      바탕과 테두리까지 흐려짐
 *   틀림   .clip(shape).background(색).pressFade(...)      안의 글자만 흐려짐
 *
 * Modifier 는 앞에 쓴 것이 바깥을 감싼다. 흐리게 하는 효과보다 **앞에서**
 * 그린 바탕과 테두리는 그 효과 밖에 있어서 그대로 남는다.
 * 그러면 글자만 흐려져 눌린 느낌이 안 난다.
 */
@Composable
fun Modifier.pressFade(
    role: Role? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    return this
        .alpha(if (pressed) 0.55f else 1f)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/**
 * 누르는 동안 뒤에 바탕이 깔린다. 아이콘만 있는 버튼에 쓴다.
 *
 * 아이콘은 선 몇 개뿐이라 흐려져도 티가 안 난다.
 * 뒤에 바탕이 생기는 편이 훨씬 분명하다.
 *
 * 바탕색은 BgSubtle(#F3F4F6)이 아니라 BorderDefault(#E6E8EC)를 쓴다.
 * 흰 바탕 위에서 F3F4F6 은 눈에 거의 안 띄어 눌린 줄 모른다 (10/26 확인).
 */
@Composable
fun Modifier.pressHighlight(
    shape: Shape,
    role: Role? = null,
    color: Color = BorderDefault,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    return this
        .clip(shape)
        .background(if (pressed) color else Color.Transparent)
        .clickable(
            interactionSource = interaction,
            indication = null,
            role = role,
            onClick = onClick,
        )
}
