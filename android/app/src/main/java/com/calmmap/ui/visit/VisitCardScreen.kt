package com.calmmap.ui.visit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.R
import com.calmmap.ui.component.BottomCta
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.ConfirmDialog
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.CardStep
import com.calmmap.ui.model.sampleCardSteps
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimaryPressed
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.axis
import com.calmmap.ui.theme.caption

// 최종안 12 미리 보는 카드: 가기 전에 아이와 같이 볼 순서 안내
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
    /** 이 시간 예상 한 줄. "배경 음악, 안내방송 가끔" */
    expectation: String = "배경 음악, 안내방송 가끔",
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

    Column(modifier = modifier.fillMaxSize().background(BgCanvas)) {
        TopBar(title = "미리 보는 카드", onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = subtitle, style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.weight(1f))
                // TODO: 문장을 고쳐서 저장하는 화면
                Text(text = "문장 고치기", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)
            }

            // TODO: 입구 사진(직접 촬영 / Storage)이 있으면 사진으로
            Image(
                painter = painterResource(R.drawable.illust_storefront),
                contentDescription = "입구 그림",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(Radius.card),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.tile)
                    .background(BrandPrimarySoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalmIcon(CalmIcons.Clock, size = 16.dp, tint = BrandPrimaryPressed)
                Text(text = "이 시간 예상 · $expectation", style = MaterialTheme.typography.caption, color = BrandPrimaryPressed)
            }

            steps.forEachIndexed { i, step -> StepRow(index = i + 1, step = step) }

            Text(
                text = "예측이라 '~할 수 있어요'로 써요. 템플릿 문장이며 보호자가 고친 뒤 저장해요.",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
            )
            // 가끔 쓰는 동작이라 작고 연하게. 실수로 누르지 않게 확인 창을 거친다
            Text(
                text = "이 방문 취소하기",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressFade(role = Role.Button) { confirmingCancel = true }
                    .padding(vertical = 10.dp),
            )
        }

        BottomCta {
            SecondaryButton(text = "아이와 보기", onClick = onChildViewClick, modifier = Modifier.weight(1f))
            PrimaryButton(text = "출발", onClick = onDepartClick, modifier = Modifier.weight(1f))
        }
    }
}

// 단계 한 줄: 회색 원 아이콘 + "1단계" + 문장
@Composable
private fun StepRow(index: Int, step: CardStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BgSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(Radius.pill).background(BgSubtle),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(step.icon, size = 20.dp, tint = TextPrimary)
        }
        Column {
            Text(text = "${index}단계", style = MaterialTheme.typography.axis, color = TextTertiary)
            Text(text = step.text, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
    }
}

// 방문 취소 확인 창
@Composable
private fun CancelVisitDialog(subtitle: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "방문을 취소할까요?",
        body = "$subtitle 방문이 예정된 방문에서 빠져요.",
        confirmText = "방문 취소",
        dismissText = "그대로 두기",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VisitCardScreenPreview() {
    CalmMapTheme {
        VisitCardScreen(subtitle = "OO마트 · 오늘 15:00", onBack = {}, onChildViewClick = {}, onDepartClick = {}, onCancelVisit = {})
    }
}
