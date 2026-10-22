package com.calmmap.ui.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.model.QuietSpot
import com.calmmap.ui.model.sampleQuietSpots
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.InkSoft
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.LineSoft
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.PaperBox

// C4 현장 모드 (P2): 도착해서 지금 부하와 쉴 곳을 보여준다
// TODO: 지금은 예시 값. 현장 측정(D)과 진동 알림을 붙이면 실제 값으로
@Composable
fun OnSiteScreen(
    placeName: String,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier,
    load: Int = 72,
    loadNote: String = "방금 안내방송이 많아졌어요",
    quietSpots: List<QuietSpot> = sampleQuietSpots,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        TopBar(title = "${placeName}에 있어요")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoadGauge(load = load, note = loadNote)
            RisingAlert()
            Text(
                text = "가까운 조용한 곳",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkSoft,
            )
            quietSpots.forEach { QuietSpotRow(it) }
        }

        SecondaryButton(
            text = "방문 끝내기",
            onClick = onEndClick,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )
    }
}

// 지금 감각부하 숫자를 크게
@Composable
private fun LoadGauge(load: Int, note: String) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(PaperBox)
            .border(1.dp, LineSoft, shape),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "지금 감각부하", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        Text(text = "$load", fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(text = note, style = MaterialTheme.typography.bodyLarge, color = InkSoft)
    }
}

// 부하가 빠르게 오를 때 뜨는 띠
@Composable
private fun RisingAlert() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LevelHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "📳 부하가 빠르게 오르고 있어요", style = MaterialTheme.typography.titleSmall, color = Paper)
        Text(text = "보호자 폰이 진동으로 알려요", style = MaterialTheme.typography.bodySmall, color = LineSoft)
    }
}

@Composable
private fun QuietSpotRow(spot: QuietSpot) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, LineSoft, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = spot.name, style = MaterialTheme.typography.bodyLarge, color = Ink)
        Text(text = spot.distance, style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnSiteScreenPreview() {
    CalmMapTheme {
        OnSiteScreen(placeName = "OO마트", onEndClick = {})
    }
}
