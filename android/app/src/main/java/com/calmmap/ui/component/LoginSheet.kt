package com.calmmap.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

/**
 * 최종안 09 로그인 안내 시트. 로그인 없이 둘러보는 중에 측정 · 방문 계획 · 우리 아이 맞춤을 누르면 뜬다.
 * 화면 전체를 어둡게 덮고 아래에서 올라온다. 바깥을 누르거나 '다음에 할게요' 로 닫는다.
 */
@Composable
fun LoginRequiredSheet(
    onKakaoClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x731A1D24))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.sheet)
                .background(BgSurface)
                // 시트 안을 눌러도 닫히지 않게 클릭을 여기서 막는다
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(BorderStrong))
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(56.dp).clip(Radius.pill).background(BrandPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                CalmIcon(CalmIcons.Lock, size = 28.dp, tint = BrandPrimary)
            }
            Spacer(Modifier.height(16.dp))
            Text(text = "로그인하면 쓸 수 있어요", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "측정 기록과 방문 계획은 계정에 저장돼요.\n지도와 장소 정보는 로그인 없이 계속 볼 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            CalmButton(text = "카카오로 시작하기", onClick = onKakaoClick, variant = ButtonVariant.Kakao)
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "다음에 할게요", onClick = onDismiss)
            Spacer(Modifier.width(1.dp))
        }
    }
}
