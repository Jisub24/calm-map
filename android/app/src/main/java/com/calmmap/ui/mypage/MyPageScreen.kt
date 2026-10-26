package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.ButtonSize
import com.calmmap.ui.component.ButtonVariant
import com.calmmap.ui.component.CalmButton
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.onboarding.SensoryFactor
import com.calmmap.ui.onboarding.Sensitivity
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.StatusCautionSoft
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

// 미리보기용
private val sampleSensitivities = linkedMapOf(
    SensoryFactor.SuddenSound to Sensitivity.Very,
    SensoryFactor.Alarm to Sensitivity.Sensitive,
    SensoryFactor.Crowd to Sensitivity.Sensitive,
)

/**
 * 최종안 18 내 정보.
 * 로그인했으면 계정 · 우리 아이 · 메뉴 5개, 둘러보는 중이면 카카오 시작 버튼과 메뉴 2개만.
 */
@Composable
fun MyPageScreen(
    onTabSelect: (MainTab) -> Unit,
    onDataInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = false,
    /** 카카오 프로필 이름 */
    nickname: String = "보호자",
    /** 저장된 우리 아이 설정. null 이면 아직 못 읽은 것 */
    sensitivities: Map<SensoryFactor, Sensitivity>? = null,
    onLoginClick: () -> Unit = {},
    onChildEditClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .statusBarsPadding(),
    ) {
        TopBar(title = "내 정보", background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard(spacing = 12.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(Radius.pill)
                            .background(BgSubtle),
                        contentAlignment = Alignment.Center,
                    ) {
                        CalmIcon(CalmIcons.User, size = 22.dp, tint = TextSecondary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isGuest) "로그인하지 않았어요" else "${nickname}님",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (isGuest) "지도와 장소 정보만 볼 수 있어요" else "카카오 계정으로 로그인했어요",
                            style = MaterialTheme.typography.caption,
                            color = TextTertiary,
                        )
                    }
                }
                // 이름은 카카오 것을 그대로 써서 '프로필 수정' 버튼은 두지 않는다
                if (isGuest) {
                    CalmButton(text = "카카오로 시작하기", onClick = onLoginClick, variant = ButtonVariant.Kakao, size = ButtonSize.M)
                }
            }

            ChildCard(isGuest = isGuest, sensitivities = sensitivities, onEditClick = onChildEditClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(BgSurface)
                    .padding(horizontal = 16.dp),
            ) {
                if (!isGuest) {
                    // TODO: 측정 기록이 저장되면 개수와 목록을 붙인다
                    MenuRow(CalmIcons.Pin, "내가 측정한 장소", trailing = "3곳", onClick = {})
                    // TODO: 알림 기능이 생기면 연결
                    MenuRow(CalmIcons.Bell, "알림 설정", onClick = {})
                }
                MenuRow(CalmIcons.Lock, "데이터·개인정보 안내", onClick = onDataInfoClick)
                // TODO: 사업장 웹 주소가 정해지면 브라우저로 연다
                MenuRow(CalmIcons.Store, "사업장이세요? 무던한 시간 등록", divider = !isGuest, onClick = {})
                if (!isGuest) {
                    MenuRow(CalmIcons.User, "계정 관리 (로그아웃 · 회원 탈퇴)", divider = false, onClick = onAccountClick)
                }
            }
        }

        BottomTabBar(selected = MainTab.ME, onSelect = onTabSelect)
    }
}

// 우리 아이: 보통이 아닌 요인만 태그로. '매우' 는 주의 색
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildCard(
    isGuest: Boolean,
    sensitivities: Map<SensoryFactor, Sensitivity>?,
    onEditClick: () -> Unit,
) {
    WhiteCard(spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "우리 아이", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            // 못 읽은 상태에서 고치면 저장된 설정을 처음 값으로 덮어써서 숨긴다
            if (!isGuest && sensitivities != null) {
                Text(
                    text = "수정하기",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandPrimary,
                    modifier = Modifier.pressFade(role = Role.Button, onClick = onEditClick),
                )
            }
        }
        val picked = sensitivities.orEmpty().filterValues { it != Sensitivity.Normal }
        when {
            isGuest -> Text(
                text = "로그인하면 아이가 힘들어하는 자극을 설정하고, 장소 점수를 우리 아이 기준으로 볼 수 있어요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
            sensitivities == null -> Text(
                text = "우리 아이 설정을 불러오는 중이에요",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
            picked.isEmpty() -> Text(
                text = "아직 민감하다고 고른 자극이 없어요",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
            // 가중치 자동 조절은 020 에서 빠져서 안내 문장 없이 태그만 보여준다
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                picked.forEach { (factor, level) -> SensitivityTag(factor.label, level) }
            }
        }
    }
}

// "갑작스러운 소리 매우 민감" 처럼 이름은 진하게, 정도는 옅게
@Composable
private fun SensitivityTag(name: String, level: Sensitivity) {
    val very = level == Sensitivity.Very
    Row(
        modifier = Modifier
            .clip(Radius.pill)
            .background(if (very) StatusCautionSoft else BgSubtle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = if (very) StatusCaution else TextSecondary)
        Text(
            text = if (very) "매우 민감" else "민감",
            style = MaterialTheme.typography.caption,
            color = if (very) StatusCaution else TextTertiary,
        )
    }
}

/** 흰 r20 카드. 내 정보 · 프로필 수정 · 계정 관리가 같이 쓴다 */
@Composable
internal fun WhiteCard(
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

// 메뉴 한 줄: 아이콘 · 이름 · (개수) · ›
@Composable
private fun MenuRow(
    icon: CalmIcons,
    text: String,
    onClick: () -> Unit,
    trailing: String? = null,
    divider: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .then(if (divider) Modifier.bottomLine(BorderDefault) else Modifier)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(icon, size = 20.dp, tint = TextSecondary)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) Text(text = trailing, style = MaterialTheme.typography.caption, color = TextTertiary)
        CalmIcon(CalmIcons.ChevronRight, size = 18.dp, tint = TextTertiary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MyPageScreenPreview() {
    CalmMapTheme {
        MyPageScreen(onTabSelect = {}, onDataInfoClick = {}, nickname = "OO 보호자", sensitivities = sampleSensitivities)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MyPageGuestPreview() {
    CalmMapTheme {
        MyPageScreen(onTabSelect = {}, onDataInfoClick = {}, isGuest = true)
    }
}

