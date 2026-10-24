package com.calmmap.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.TopBar
import com.calmmap.ui.component.topLine
import com.calmmap.ui.theme.BgCanvas
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BorderDefault
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.KakaoBg
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/** 닉네임 최대 글자 수 */
private const val NICKNAME_MAX = 10

/** 최종안 18-1 프로필 수정: 사진 · 닉네임 · 로그인 정보 */
@Composable
fun ProfileEditScreen(
    nickname: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(nickname) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas)
            .navigationBarsPadding(),
    ) {
        TopBar(title = "프로필 수정", onBack = onBack, background = BgCanvas)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // TODO: 사진 고르기는 저장소(Storage)가 정해지면 붙인다
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(88.dp)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(Radius.pill)
                            .background(BorderDefault),
                        contentAlignment = Alignment.Center,
                    ) {
                        CalmIcon(CalmIcons.User, size = 40.dp, tint = TextPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .offset(62.dp, 62.dp)
                            .size(28.dp)
                            .clip(Radius.pill)
                            .background(BgSurface)
                            .border(1.dp, BorderStrong, Radius.pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        CalmIcon(CalmIcons.Plus, size = 16.dp, tint = TextPrimary)
                    }
                }
                Text(text = "사진 바꾸기", style = MaterialTheme.typography.bodySmall, color = BrandPrimary)
            }

            WhiteCard(spacing = 12.dp) {
                Row {
                    Text(text = "닉네임", style = MaterialTheme.typography.labelLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(text = "${text.length}/$NICKNAME_MAX", style = MaterialTheme.typography.caption, color = TextTertiary)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= NICKNAME_MAX) text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(BrandPrimary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(Radius.tile)
                                .background(BgSurface)
                                .border(1.5.dp, BrandPrimary, Radius.tile)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { inner() }
                    },
                )
                Text(text = "앱 안에서만 쓰여요. 다른 사용자에게는 보이지 않아요.", style = MaterialTheme.typography.caption, color = TextTertiary)
            }

            WhiteCard(spacing = 12.dp) {
                Text(text = "로그인 정보", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(Radius.tag)
                            .background(KakaoBg),
                    )
                    Text(text = "카카오 계정으로 로그인", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Text(
                    text = "카카오에서 받은 정보는 로그인에만 써요. 닉네임은 여기서 바꿀 수 있어요.",
                    style = MaterialTheme.typography.caption,
                    color = TextTertiary,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .topLine(BorderDefault)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            PrimaryButton(text = "저장", onClick = { onSave(text.trim()) }, enabled = text.isNotBlank())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ProfileEditScreenPreview() {
    CalmMapTheme {
        ProfileEditScreen(nickname = "OO 보호자", onBack = {}, onSave = {})
    }
}
