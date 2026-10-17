package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.theme.CalmMapTheme
import kotlinx.coroutines.launch

// 온보딩 한 장에 들어가는 내용
private data class OnboardingPage(
    val illustration: String,
    val title: String,
    val description: String,
)

// TODO: 2, 3장은 시안이 없어서 임시 문구. 확정되면 바꾼다
private val pages = listOf(
    OnboardingPage(
        illustration = "일러스트: 지도 위 시간대 그래프",
        title = "같은 마트도\n시간마다 달라요",
        description = "소리·혼잡을 예측해서 아이가 덜 힘든 시간을 알려드려요.",
    ),
    OnboardingPage(
        illustration = "일러스트: 요일·시간대 점수",
        title = "언제 가면 무던한지\n미리 볼 수 있어요",
        description = "요일과 시간대별로 감각 부담이 얼마나 되는지 보여드려요.",
    ),
    OnboardingPage(
        illustration = "일러스트: 방문 후 기록",
        title = "다녀온 뒤 남기면\n우리 아이에게 맞춰져요",
        description = "아이 반응을 기록하면 다음 추천이 더 정확해져요.",
    ),
)

// A2 온보딩: 옆으로 넘기는 소개 2~3장. 마지막 장에서 onFinish 로 다음 단계(로그인)로 간다
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 지금 몇 번째 장인지 기억. 바뀌면 점 표시도 다시 그려진다
    val pagerState = rememberPagerState(pageCount = { pages.size })
    // 버튼으로 장을 넘길 때 애니메이션을 돌릴 곳
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 위아래 Spacer 가 남는 공간을 반씩 나눠 내용을 가운데로 민다
        Spacer(modifier = Modifier.weight(1f))

        // 옆으로 넘기는 부분. 장마다 pages[index] 내용만 바뀐다
        HorizontalPager(state = pagerState) { index ->
            OnboardingPageContent(page = pages[index])
        }

        PrivacyNotice()

        PageDots(pageCount = pages.size, currentPage = pagerState.currentPage)

        Spacer(modifier = Modifier.weight(1f))

        // 마지막 장이면 온보딩 끝, 아니면 다음 장으로
        PrimaryButton(
            text = "다음",
            onClick = {
                val isLastPage = pagerState.currentPage == pages.lastIndex
                if (isLastPage) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        )
    } 
}

// 온보딩 한 장: 일러스트 + 제목 + 설명
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 일러스트가 들어갈 자리. 그림이 나오면 Image 로 바꾼다
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = page.illustration, fontSize = 12.sp, color = Color(0xFF808080))
        }
        Text(
            text = page.title,
            fontSize = 22.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
        )
        Text(
            text = page.description,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Color(0xFF666666),
        )
    }
}

// 원음을 저장하지 않는다는 안내. 모든 장에 고정으로 보인다
@Composable
private fun PrivacyNotice() {
    Text(
        text = "🔒 원음은 저장하지 않아요. 소리 종류와 크기만 기록해요.",
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = Color(0xFF4D4D4D),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(12.dp))
            .padding(14.dp),
    )
}

// 지금 몇 번째 장인지 보여주는 점
@Composable
private fun PageDots(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            // 지금 장은 진하게, 나머지는 연하게
            val color = if (index == currentPage) Color(0xFF333333) else Color(0xFFCCCCCC)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingScreenPreview() {
    CalmMapTheme {
        OnboardingScreen(onFinish = {})
    }
}
