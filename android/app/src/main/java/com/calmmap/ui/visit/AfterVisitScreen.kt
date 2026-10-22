package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.model.VisitFeeling
import com.calmmap.ui.model.visitTriggers
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperSoft

// C5 방문 후 기록: 괜찮았어요 / 힘들었어요 중 하나를 고르고 '기록하기'
// 원인 칩은 '힘들었어요' 일 때만 같이 저장된다
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AfterVisitScreen(
    placeName: String,
    whenLabel: String,
    onSubmit: (feeling: VisitFeeling, triggers: List<String>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 고른 반응. 고르기 전에는 null 이라 '기록하기' 를 누를 수 없다
    var feeling by remember { mutableStateOf<VisitFeeling?>(null) }
    val picked = remember { mutableStateListOf<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "$placeName 다녀왔어요",
            fontSize = 22.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "$whenLabel 방문",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeelingCard(
                emoji = "🙂",
                label = "괜찮았어요",
                selected = feeling == VisitFeeling.OK,
                onClick = { feeling = VisitFeeling.OK },
                modifier = Modifier.weight(1f),
            )
            FeelingCard(
                emoji = "😣",
                label = "힘들었어요",
                selected = feeling == VisitFeeling.HARD,
                onClick = { feeling = VisitFeeling.HARD },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "고르고 기록하면 다음 추천에 반영돼요.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // 선택 입력이라 점선 상자
        val shape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .outlineBorder(LineSoft, shape, dashed = true)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "(선택) 무엇이 힘들었나요?", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visitTriggers.forEach { trigger ->
                    FilterChip(
                        label = trigger,
                        selected = trigger in picked,
                        onClick = { if (trigger in picked) picked.remove(trigger) else picked.add(trigger) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "기록하기",
            enabled = feeling != null,
            onClick = {
                val chosen = feeling ?: return@PrimaryButton
                onSubmit(chosen, if (chosen == VisitFeeling.HARD) picked.toList() else emptyList())
            },
        )
        Text(
            text = "건너뛰기",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .pressFade(role = Role.Button, onClick = onSkip)
                .padding(vertical = 12.dp),
        )
    }
}

// 크게 누르는 반응 카드
@Composable
private fun FeelingCard(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .pressFade(role = Role.RadioButton, onClick = onClick)
            .clip(shape)
            .background(PaperSoft)
            // 고른 카드는 테두리를 굵고 진하게
            .border(if (selected) 2.dp else 1.dp, if (selected) LineStrong else Line, shape)
            .padding(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 36.sp)
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AfterVisitScreenPreview() {
    CalmMapTheme {
        AfterVisitScreen(placeName = "OO마트", whenLabel = "오늘 15:00", onSubmit = { _, _ -> }, onSkip = {})
    }
}
