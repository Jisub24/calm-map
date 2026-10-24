package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.FeelingFace
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.LevelBadge
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LevelMid
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import kotlinx.coroutines.launch

// 온보딩 한 장에 들어가는 내용
private data class OnboardingPage(
    val illustration: @Composable () -> Unit,
    val title: String,
    val description: String,
)

// 최종안은 1/3 장만 있다. 2 · 3장은 같은 그림 틀(연한 파랑 + 흰 카드)에 내용만 바꿨다
// TODO: 2 · 3장 시안이 나오면 그림과 문구를 맞춘다
private val pages = listOf(
    OnboardingPage(
        illustration = { GraphIllustration() },
        title = "같은 마트도\n시간마다 달라요",
        description = "소리·혼잡·밝기를 시간대별로 살펴보고\n아이에게 덜 힘든 시간을 알려드려요.",
    ),
    OnboardingPage(
        illustration = { DaysIllustration() },
        title = "언제 가면 무던한지\n미리 볼 수 있어요",
        description = "요일과 시간대별로 감각 부담이\n얼마나 되는지 보여드려요.",
    ),
    OnboardingPage(
        illustration = { FeedbackIllustration() },
        title = "다녀온 뒤 남기면\n우리 아이에게 맞춰져요",
        description = "아이 반응을 기록하면\n다음 추천이 더 정확해져요.",
    ),
)

// 최종안 02 온보딩: 옆으로 넘기는 소개 3장. 마지막 장이나 '건너뛰기'에서 onFinish 로 로그인으로 간다
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    // 버튼으로 장을 넘길 때 애니메이션을 돌릴 곳
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Text(
            text = "건너뛰기",
            style = MaterialTheme.typography.labelLarge,
            color = TextTertiary,
            modifier = Modifier
                .align(Alignment.End)
                .pressFade(role = Role.Button, onClick = onFinish)
                .padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(28.dp))

        HorizontalPager(state = pagerState) { index ->
            val page = pages[index]
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(264.dp)
                        .clip(Radius.illustration)
                        .background(BrandPrimarySoft),
                    contentAlignment = Alignment.Center,
                ) { page.illustration() }
                Spacer(Modifier.height(36.dp))
                Text(text = page.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Text(text = page.description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        PrivacyNotice()

        Spacer(Modifier.weight(1f))
        PageDots(pageCount = pages.size, currentPage = pagerState.currentPage)
        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "다음",
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }
}

// 원음을 저장하지 않는다는 안내. 모든 장에 고정
@Composable
private fun PrivacyNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSubtle)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(CalmIcons.Lock, size = 18.dp, tint = TextSecondary)
        Text(
            text = "원음은 저장하지 않아요. 소리 종류와 크기만 기록해요.",
            style = MaterialTheme.typography.caption,
            color = TextSecondary,
        )
    }
}

// 지금 장은 길쭉한 파란 막대, 나머지는 작은 회색 점
@Composable
private fun PageDots(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == currentPage) 24.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i == currentPage) BrandPrimary else BorderStrong),
            )
        }
    }
}

// 흰 카드 (일러스트 공통 틀)
@Composable
private fun IllustrationCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            // 최종안: 가운데보다 12 아래 (위 56 · 아래 32)
            .offset(y = 12.dp)
            .size(width = 252.dp, height = 176.dp)
            .calmShadow(Shadows.float, Radius.card)
            .clip(Radius.card)
            .background(BgSurface)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp),
    ) { content() }
}

// 1장: 시간대 막대 그래프 + 무던한 구간 점선 표시 (피그마 그대로)
@Composable
private fun GraphIllustration() {
    IllustrationCard {
        Column {
            Text(text = "OO마트 · 화요일", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Text(text = "14시 이후가 무던해요", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
        val bars = listOf(
            27f to LevelCalm, 49.5f to LevelMid, 79.2f to LevelHigh, 82.8f to LevelHigh,
            37.8f to LevelMid, 21.6f to LevelCalm, 27f to LevelCalm, 54f to LevelMid,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            bars.forEachIndexed { i, (h, c) ->
                Bar(h.dp, c, Modifier.align(Alignment.BottomStart).offset(x = (28 * i).dp, y = (-16).dp))
            }
            // 무던한 구간을 감싸는 점선 상자
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 108.dp, y = (-10).dp)
                    .size(width = 84.dp, height = 98.dp)
                    .outlineBorder(BrandPrimary, RoundedCornerShape(12.dp), width = 1.5.dp, dashed = true),
            )
        }
    }
}

@Composable
private fun Bar(height: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 20.dp, height = height)
            .clip(RoundedCornerShape(5.dp))
            .background(color),
    )
}

// 2장: 요일별 단계
@Composable
private fun DaysIllustration() {
    IllustrationCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "OO마트 · 이번 주", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            listOf(
                "화 14시" to SensoryLevel.CALM,
                "토 11시" to SensoryLevel.MID,
                "토 15시" to SensoryLevel.HIGH,
            ).forEach { (time, level) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.width(72.dp),
                    )
                    LevelBadge(level)
                }
            }
        }
    }
}

// 3장: 다녀온 뒤 반응 고르기
@Composable
private fun FeedbackIllustration() {
    IllustrationCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "OO마트 다녀왔어요", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("괜찮았어요", "힘들었어요").forEachIndexed { i, label ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(Radius.tile)
                            .background(if (i == 0) BrandPrimarySoft else BgSubtle)
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // 방문 후 기록 화면과 같은 얼굴. 고른 쪽(괜찮았어요)은 파랗게
                        val color = if (i == 0) BrandPrimary else TextPrimary
                        FeelingFace(good = i == 0, color = color, size = 32.dp)
                        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingScreenPreview() {
    CalmMapTheme { OnboardingScreen(onFinish = {}) }
}
