package com.calmmap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CompactSegmented
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.bottomLine
import com.calmmap.ui.theme.BgCanvas
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

/**
 * 아이가 힘들어할 수 있는 감각 요인 (최종안 7가지).
 * group 은 화면에서 묶어 보여줄 제목, hint 는 이름 아래 작은 예시
 */
enum class SensoryFactor(val label: String, val group: String, val hint: String? = null) {
    SuddenSound("갑작스러운 소리", "소리", "안내방송·딩동 같은 돌발음"),
    Alarm("경보음·사이렌", "소리"),
    LoudVoice("사람의 큰 소리", "소리", "고함·울음"),
    Machine("기계음", "소리", "핸드드라이어·청소기"),
    SustainedLoud("계속되는 큰 소리", "소리", "음악·웅성거림"),
    Crowd("사람 많음", "환경"),
    BrightLight("밝은 조명", "환경"),
}

/** 요인마다 고르는 민감도 3단계. 처음엔 모두 보통 */
enum class Sensitivity(val label: String) {
    Normal("보통"),
    Sensitive("민감"),
    Very("매우"),
}

// 최종안 05 우리 아이 설정: 요인마다 보통 / 민감 / 매우 를 고른다
// '매우'로 고른 자극이 있는 시간은 다른 점수와 상관없이 '주의'로 알린다
@Composable
fun ChildProfileScreen(
    onStart: (Map<SensoryFactor, Sensitivity>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initial: Map<SensoryFactor, Sensitivity> = emptyMap(),
    // 내 정보에서 고칠 때는 "저장"
    confirmText: String = "저장하고 시작하기",
    // 저장하는 동안 버튼을 막아 두 번 저장되지 않게
    saving: Boolean = false,
) {
    val sensitivities = remember {
        mutableStateMapOf<SensoryFactor, Sensitivity>().apply {
            SensoryFactor.entries.forEach { put(it, initial[it] ?: Sensitivity.Normal) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        TopBar(title = "우리 아이 설정", onBack = onBack, background = BgCanvas)

        // 내용이 길어지면 이 부분만 스크롤된다. 버튼은 아래에 고정
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Text(
                text = "아이가 힘들어하는 것을\n알려주세요",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "'매우 민감'으로 고른 자극이 있는 시간은 다른 점수와 상관없이 '주의'로 알려드려요.",
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            SensoryFactor.entries.groupBy { it.group }.entries.forEachIndexed { i, (group, factors) ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                FactorGroup(
                    title = group,
                    factors = factors,
                    selected = { sensitivities.getValue(it) },
                    onSelect = { factor, level -> sensitivities[factor] = level },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCanvas)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "진단명·행동 기록은 묻지 않아요",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(text = confirmText, enabled = !saving, onClick = { onStart(sensitivities.toMap()) })
        }
    }
}

// 소리 · 환경 묶음 카드
@Composable
private fun FactorGroup(
    title: String,
    factors: List<SensoryFactor>,
    selected: (SensoryFactor) -> Sensitivity,
    onSelect: (SensoryFactor, Sensitivity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
        factors.forEachIndexed { i, factor ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (i < factors.lastIndex) it.bottomLine(BorderDefault) else it }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(text = factor.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    if (factor.hint != null) {
                        Text(text = factor.hint, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
                CompactSegmented(
                    options = Sensitivity.entries.map { it.label },
                    selectedIndex = selected(factor).ordinal,
                    onSelect = { onSelect(factor, Sensitivity.entries[it]) },
                    // '매우'는 주의 색으로 눈에 띄게
                    selectedColor = { if (it == Sensitivity.Very.ordinal) StatusCaution else BrandPrimary },
                    selectedBackground = { if (it == Sensitivity.Very.ordinal) StatusCautionSoft else BgSurface },
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChildProfileScreenPreview() {
    CalmMapTheme {
        ChildProfileScreen(
            onStart = {},
            onBack = {},
            initial = mapOf(SensoryFactor.SuddenSound to Sensitivity.Very, SensoryFactor.Alarm to Sensitivity.Sensitive),
        )
    }
}
