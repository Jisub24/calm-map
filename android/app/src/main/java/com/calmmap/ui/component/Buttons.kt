package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSunken
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandOnPrimary
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.Height
import com.calmmap.ui.theme.KakaoBg
import com.calmmap.ui.theme.KakaoFg
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextDisabled
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

/**
 * 피그마 최종안 Button. Variant 4종 × Size 2종.
 *
 *   Primary    파란 바탕 — 화면의 주된 동작 하나 (다음 · 방문 계획 · 저장)
 *   Secondary  흰 바탕 + 회색 테두리 — 곁들이는 동작 (상세 보기 · 이 장소 측정)
 *   Ghost      글자만 파랗게 — 건너뛰기 · 나중에 할게요
 *   Kakao      카카오 공식 노란색 + 로고
 *   Danger     진한 회색 바탕 — 되돌릴 수 없는 동작 (탈퇴하기 · 방문 취소)
 *
 *   L 52dp (주요 버튼)   M 44dp (카드 안 · 나란히 둘 때)
 *
 * 모양은 모두 알약(999). 누르는 동안 Primary 는 한 단계 진해지고, 나머지는 옅은 바탕이 깔린다.
 * 누를 수 없을 때는 회색 바탕 + 흐린 글자.
 */
enum class ButtonVariant { Primary, Secondary, Ghost, Kakao, Danger }
enum class ButtonSize { L, M }

@Composable
fun CalmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.L,
    enabled: Boolean = true,
    leadingIcon: CalmIcons? = if (variant == ButtonVariant.Kakao) CalmIcons.Kakao else null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val (bg, fg) = when {
        !enabled && variant != ButtonVariant.Ghost -> BgSunken to TextDisabled
        !enabled -> Color.Transparent to TextDisabled
        variant == ButtonVariant.Primary -> (if (pressed) BrandPrimaryPressed else BrandPrimary) to BrandOnPrimary
        variant == ButtonVariant.Secondary -> (if (pressed) BgSubtle else BgSurface) to TextPrimary
        variant == ButtonVariant.Ghost -> (if (pressed) BrandPrimarySoft else Color.Transparent) to BrandPrimary
        variant == ButtonVariant.Danger -> (if (pressed) TextSecondary else TextPrimary) to BrandOnPrimary
        else -> KakaoBg to KakaoFg
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (size == ButtonSize.L) Height.buttonL else Height.buttonM)
            .clip(Radius.pill)
            .background(bg)
            .then(
                if (variant == ButtonVariant.Secondary) Modifier.border(1.dp, BorderStrong, Radius.pill) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) CalmIcon(leadingIcon, size = 20.dp, tint = fg)
        // 버튼 높이가 정해져 있어서 두 줄이 되면 잘린다. 폰 글자 크기를 키워 칸이 모자라면 한 줄 안에서 글자를 줄인다
        val style = if (size == ButtonSize.L) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge
        BasicText(
            text = text,
            style = style.copy(color = fg),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize),
        )
    }
}

/** 주된 동작. 방문 계획 · 저장 · 다음 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.L,
) = CalmButton(text, onClick, modifier, ButtonVariant.Primary, size, enabled)

/** 곁들이는 동작. 상세 보기 · 이 장소 측정 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.L,
) = CalmButton(text, onClick, modifier, ButtonVariant.Secondary, size, enabled)

/** 글자만 있는 버튼. 건너뛰기 · 나중에 할게요 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.L,
) = CalmButton(text, onClick, modifier, ButtonVariant.Ghost, size, enabled)

/**
 * 목록 한 줄 안에 들어가는 작은 알약 버튼. 검색 결과의 '+ 측정 요청 4'.
 * 줄 안에 들어가야 해서 32dp 지만 좌우 여백으로 누를 면적을 확보했다.
 */
@Composable
fun SmallOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: CalmIcons? = null,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .pressFade(role = Role.Button, enabled = enabled, onClick = onClick)
            .clip(Radius.pill)
            .background(BgSurface)
            .border(1.dp, if (enabled) TextPrimary else BorderStrong, Radius.pill)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) TextPrimary else TextDisabled
        if (leadingIcon != null) CalmIcon(leadingIcon, size = 14.dp, tint = fg)
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
