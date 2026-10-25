package com.calmmap.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.calmmap.R
import com.calmmap.ui.theme.BrandPrimarySoft

/**
 * 최종안 로고. 둥근 타일 + 파란 핀 + 물결.
 * 스플래시(파란 바탕)에서는 흰 타일, 로그인에서는 연한 파란 타일을 쓴다.
 * 모서리는 피그마 그대로 한 변의 28% (80 → 22.5).
 */
@Composable
fun CalmLogo(size: Dp, modifier: Modifier = Modifier, tile: Color = BrandPrimarySoft) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 28))
            .background(tile),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = "무던한 지도",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
