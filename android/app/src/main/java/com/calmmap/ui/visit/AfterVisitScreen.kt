package com.calmmap.ui.visit

import android.util.Log
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.data.brief
import com.calmmap.ui.component.FeelingFace
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.GhostButton
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.VisitFeeling
import com.calmmap.ui.model.HardFactor
import com.calmmap.ui.theme.BgSubtle
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.BrandPrimarySoft
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.StatusCaution
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import kotlinx.coroutines.launch

// 최종안 14 방문 후 기록: 괜찮았어요 / 힘들었어요 중 하나를 고르고 '기록하기'
// '힘들었어요' 를 고르면 무엇이 힘들었는지 칩이 나온다 (선택)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AfterVisitScreen(
    placeName: String,
    whenLabel: String,
    onSubmit: suspend (feeling: VisitFeeling, triggers: List<HardFactor>) -> Unit,
    onSkip: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 고른 반응. 고르기 전에는 null 이라 '기록하기' 를 누를 수 없다
    var feeling by remember { mutableStateOf<VisitFeeling?>(null) }
    val picked = remember { mutableStateListOf<HardFactor>() }
    // DB 에 쓰는 동안은 두 버튼을 막는다. 실패하면 이 화면에 남아서 다시 누를 수 있다
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun save(action: suspend () -> Unit) = scope.launch {
        saving = true
        saveError = null
        try {
            action()
        } catch (e: Exception) {
            Log.w("AfterVisit", "방문 기록 저장 실패: ${e.brief()}")
            saveError = "저장하지 못했어요. 인터넷 연결을 확인하고 다시 눌러 주세요"
        } finally {
            saving = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
    ) {
        Text(text = "$placeName\n다녀왔어요", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(text = "$whenLabel 방문 · 아이는 어땠나요?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeelingCard(
                good = true,
                label = "괜찮았어요",
                selected = feeling == VisitFeeling.OK,
                onClick = { feeling = VisitFeeling.OK },
                modifier = Modifier.weight(1f),
            )
            FeelingCard(
                good = false,
                label = "힘들었어요",
                selected = feeling == VisitFeeling.HARD,
                onClick = { feeling = VisitFeeling.HARD },
                modifier = Modifier.weight(1f),
            )
        }

        if (feeling == VisitFeeling.HARD) {
            Spacer(Modifier.height(24.dp))
            Text(text = "무엇이 힘들었나요? (선택)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HardFactor.entries.forEach { trigger ->
                    FilterChip(
                        label = trigger.label,
                        selected = trigger in picked,
                        onClick = {
                            when {
                                trigger in picked -> picked.remove(trigger)
                                // '잘 모르겠어요' 는 다른 요인과 같이 고를 수 없다
                                trigger == HardFactor.UNKNOWN -> { picked.clear(); picked.add(trigger) }
                                else -> { picked.remove(HardFactor.UNKNOWN); picked.add(trigger) }
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.banner)
                    .background(BgSubtle)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalmIcon(CalmIcons.Info, size = 16.dp, tint = TextTertiary)
                Text(
                    text = "고른 요인은 '우리 아이 맞춤'에 조금씩 반영돼요. 한 번에 크게 바뀌지 않아요.",
                    style = MaterialTheme.typography.caption,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        saveError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.caption,
                color = StatusCaution,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        PrimaryButton(
            text = if (saving) "저장하는 중…" else "기록하기",
            enabled = feeling != null && !saving,
            onClick = {
                val chosen = feeling ?: return@PrimaryButton
                save { onSubmit(chosen, if (chosen == VisitFeeling.HARD) picked.toList() else emptyList()) }
            },
        )
        Spacer(Modifier.height(4.dp))
        GhostButton(text = "건너뛰기", enabled = !saving, onClick = { save(onSkip) })
    }
}

// 반응 카드. 고른 카드는 연한 파랑 + 파란 테두리
@Composable
private fun FeelingCard(good: Boolean, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .pressFade(role = Role.RadioButton, onClick = onClick)
            .clip(Radius.card)
            .background(if (selected) BrandPrimarySoft else BgSubtle)
            .then(if (selected) Modifier.border(1.5.dp, BrandPrimary, Radius.card) else Modifier)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FeelingFace(good = good, color = if (selected) BrandPrimary else TextPrimary)
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = if (selected) BrandPrimary else TextPrimary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AfterVisitScreenPreview() {
    CalmMapTheme {
        AfterVisitScreen(placeName = "OO마트", whenLabel = "오늘 15:00", onSubmit = { _, _ -> }, onSkip = {})
    }
}
