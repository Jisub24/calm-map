package com.calmmap.ui.visit

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.CardStep
import com.calmmap.ui.model.sampleCardSteps
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

// 최종안 13 아이와 보기: 카드를 한 장씩 크게. 아이가 보기 쉽게 큰 그림과 글자만 둔다
// 처음에서 '이전', 끝에서 '다 봤어요', 위의 '닫기' 를 누르면 onClose 로 카드 화면에 돌아간다
@Composable
fun ChildViewScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    steps: List<CardStep> = sampleCardSteps,
) {
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val isLast = index == steps.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandPrimarySoft)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "닫기",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.pressFade(role = Role.Button, onClick = onClose).padding(vertical = 8.dp),
            )
            // 진행 막대: 본 장까지 파랗게
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(steps.size) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= index) BrandPrimary else BgSurface),
                    )
                }
            }
            Text(text = "${index + 1}/${steps.size}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }

        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(196.dp)
                .calmShadow(Shadows.card, Radius.pill)
                .clip(Radius.pill)
                .background(BgSurface),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(step.icon, size = 88.dp, tint = BrandPrimary)
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = step.childText,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        // TODO: TextToSpeech 로 읽어주기
        Row(
            modifier = Modifier
                .clip(Radius.pill)
                .background(BgSurface)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalmIcon(CalmIcons.Volume, size = 18.dp, tint = BrandPrimary)
            Text(text = "읽어주기", style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = "이전",
                onClick = { if (index == 0) onClose() else index-- },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = if (isLast) "다 봤어요" else "다음",
                onClick = { if (isLast) onClose() else index++ },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp).fillMaxWidth())
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChildViewScreenPreview() {
    CalmMapTheme { ChildViewScreen(onClose = {}) }
}
