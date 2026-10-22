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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.model.CardStep
import com.calmmap.ui.model.sampleCardSteps
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LineFaint
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper

// C3 아이와 보기: 카드를 한 장씩 크게. 아이가 보기 쉽게 글자와 그림만 둔다
// 처음에서 '이전', 끝에서 '다 봤어요' 를 누르면 onClose 로 카드 화면에 돌아간다
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
            .background(Paper)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Progress(count = steps.size, current = index)

        // TODO: 단계별 큰 그림이 생기면 Image 로
        PhotoPlaceholder(text = "큰 그림", height = 300.dp)

        Text(
            text = step.childText,
            fontSize = 28.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // TODO: TextToSpeech 로 읽어주기
        Text(
            text = "🔊 읽어주기",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

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
    }
}

// 위쪽 진행 막대. 본 장까지 진하게
@Composable
private fun Progress(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= current) LineStrong else LineSoft),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChildViewScreenPreview() {
    CalmMapTheme {
        ChildViewScreen(onClose = {})
    }
}
