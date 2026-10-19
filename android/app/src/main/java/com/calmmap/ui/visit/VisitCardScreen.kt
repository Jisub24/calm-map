package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.CardStep
import com.calmmap.ui.model.sampleCardSteps
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LevelCalm
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox
import com.calmmap.ui.theme.PaperSoft

// C2 미리 보는 카드: 가기 전에 아이와 같이 볼 순서 안내
@Composable
fun VisitCardScreen(
    /** 어느 방문의 카드인지. "OO마트 · 내일 15:00" */
    subtitle: String,
    onBack: () -> Unit,
    onChildViewClick: () -> Unit,
    onDepartClick: () -> Unit,
    /** 확인 창에서 '방문 취소' 를 눌렀을 때 */
    onCancelVisit: () -> Unit,
    modifier: Modifier = Modifier,
    steps: List<CardStep> = sampleCardSteps,
) {
    var confirmingCancel by remember { mutableStateOf(false) }

    if (confirmingCancel) {
        CancelVisitDialog(
            subtitle = subtitle,
            onConfirm = {
                confirmingCancel = false
                onCancelVisit()
            },
            onDismiss = { confirmingCancel = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "미리 보는 카드", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted)

            // TODO: 입구 사진은 직접 찍거나 Storage 에서 받는다
            PhotoPlaceholder(text = "입구 사진", height = 150.dp)

            // TODO: AI 카드 생성을 붙이면 '다시 만들기' 를 누를 수 있게
            Text(
                text = "✨ AI가 만든 카드 · 다시 만들기 ↻",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )

            steps.forEachIndexed { index, step ->
                StepRow(text = "${index + 1}. ${step.text}")
            }
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(text = "아이와 보기", onClick = onChildViewClick, modifier = Modifier.weight(1f))
            PrimaryButton(text = "출발", onClick = onDepartClick, modifier = Modifier.weight(1f))
        }
        // 가끔 쓰는 동작이라 작고 연하게. 실수로 누르지 않게 확인 창을 거친다
        Text(
            text = "이 방문 취소하기",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
                .pressFade(role = Role.Button, onClick = { confirmingCancel = true })
                .padding(vertical = 12.dp),
        )
    }
}

// 방문 취소 확인 창
@Composable
private fun CancelVisitDialog(subtitle: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        title = { Text(text = "방문을 취소할까요?", style = MaterialTheme.typography.titleMedium, color = Ink) },
        text = {
            Text(
                text = "$subtitle 방문이 예정된 방문에서 빠져요.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "방문 취소", fontWeight = FontWeight.Bold, color = Ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소하지 않기", color = InkMuted)
            }
        },
    )
}

// 사진이 들어갈 자리. 점선은 '아직 없는 것' 표시
@Composable
internal fun PhotoPlaceholder(text: String, height: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(PaperBox)
            .outlineBorder(LineSoft, shape, dashed = true),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = InkMuted, textAlign = TextAlign.Center)
    }
}

// 카드 한 줄: 왼쪽 그림 자리 + 안내 문장
@Composable
private fun StepRow(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PaperSoft)
            .border(1.dp, LineSoft, shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: 단계별 그림이 생기면 Image 로
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LevelCalm),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Ink)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VisitCardScreenPreview() {
    CalmMapTheme {
        VisitCardScreen(
            subtitle = "OO마트 · 오늘 15:00",
            onBack = {},
            onChildViewClick = {},
            onDepartClick = {},
            onCancelVisit = {},
        )
    }
}
