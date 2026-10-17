package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.components.SecondaryButton
import com.calmmap.ui.components.TopBar
import com.calmmap.ui.theme.CalmMapTheme

// 아이가 힘들어할 수 있는 감각 요인. group 은 화면에서 묶어 보여줄 제목
enum class SensoryFactor(val label: String, val group: String) {
    Announcement("안내방송", "소리"),
    BabyCrying("아기 울음", "소리"),
    Siren("사이렌·경보", "소리"),
    Speech("사람 말소리", "소리"),
    Music("음악", "소리"),
    Construction("공사 소음", "소리"),
    Crowd("혼잡", "환경"),
    FlickeringLight("깜빡이는 조명", "환경"),
    BrightLight("밝은 조명", "환경"),
}

// 민감도는 1~5 단계. 처음 고르면 가운데(3)에서 시작한다
private const val MAX_LEVEL = 5
private const val DEFAULT_LEVEL = 3

// A5 아이 프로필: 아이가 힘들어하는 요인과 민감도를 고른다
// 시작하기 → 고른 값(요인 → 민감도)을 넘김 / 건너뛰기 → 기본 가중치로 진행
@Composable
fun ChildProfileScreen(
    onStart: (Map<SensoryFactor, Int>) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 고른 요인과 그 민감도. 목록에 있으면 선택된 것, 값이 민감도
    val sensitivities = remember { mutableStateMapOf<SensoryFactor, Int>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        TopBar(title = "우리 아이 설정", onBack = onBack)

        // 내용이 길어지면 이 부분만 스크롤된다. 버튼은 아래에 고정
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "아이가 힘들어하는 것을 골라주세요",
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
            )
            Text(
                text = "고른 요인에 가중치를 줘서 '우리 아이 맞춤' 지수를 계산해요.",
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = Color(0xFF737373),
            )

            // "소리", "환경" 묶음마다 제목 + 칩 목록
            SensoryFactor.entries.groupBy { it.group }.forEach { (group, factors) ->
                SectionLabel(text = group)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    factors.forEach { factor ->
                        Chip(
                            label = factor.label,
                            selected = factor in sensitivities,
                            onClick = {
                                // 누를 때마다 선택 ↔ 해제
                                if (factor in sensitivities) {
                                    sensitivities.remove(factor)
                                } else {
                                    sensitivities[factor] = DEFAULT_LEVEL
                                }
                            },
                        )
                    }
                }
            }

            // 하나라도 고르면 민감도 상자가 나타난다
            if (sensitivities.isNotEmpty()) {
                SensitivityPanel(
                    sensitivities = sensitivities,
                    onLevelChange = { factor, level -> sensitivities[factor] = level },
                )
            }

            Text(
                text = "진단명·행동 기록은 묻지 않아요",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF808080),
            )
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // TODO: Supabase profiles 에 저장한 뒤 지도 홈으로
            PrimaryButton(text = "시작하기", onClick = { onStart(sensitivities.toMap()) })
            SecondaryButton(text = "건너뛰기", onClick = onSkip)
        }
    }
}

// "소리", "환경", "민감도" 같은 작은 소제목
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF4D4D4D),
    )
}

// 눌러서 고르는 둥근 버튼 (와이어프레임 Chip). 선택되면 진하게 칠해진다
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = label,
        fontSize = 13.sp,
        color = if (selected) Color.White else Color(0xFF333333),
        modifier = Modifier
            .background(if (selected) Color(0xFF333333) else Color.White, shape)
            .border(1.dp, if (selected) Color(0xFF333333) else Color(0xFF999999), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// 고른 요인마다 민감도를 ●●●○○ 로 보여주고, 점을 눌러 바꾼다
@Composable
private fun SensitivityPanel(
    sensitivities: Map<SensoryFactor, Int>,
    onLevelChange: (SensoryFactor, Int) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), shape)
            .border(1.dp, Color(0xFFCCCCCC), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(text = "민감도")
        // 칩 순서대로 보여주려고 enum 순서로 돈다
        SensoryFactor.entries.filter { it in sensitivities }.forEach { factor ->
            val level = sensitivities.getValue(factor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = factor.label, fontSize = 13.sp, color = Color(0xFF212121))
                Row {
                    // n 번째 점을 누르면 민감도 n
                    for (n in 1..MAX_LEVEL) {
                        Text(
                            text = if (n <= level) "●" else "○",
                            fontSize = 13.sp,
                            color = Color(0xFF4D4D4D),
                            modifier = Modifier
                                .clickable { onLevelChange(factor, n) }
                                .padding(horizontal = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChildProfileScreenPreview() {
    CalmMapTheme {
        ChildProfileScreen(onStart = {}, onSkip = {}, onBack = {})
    }
}
