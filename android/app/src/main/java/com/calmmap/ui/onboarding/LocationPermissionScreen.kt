package com.calmmap.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calmmap.R
import com.calmmap.ui.component.GhostButton
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.CalmMapTheme
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary

// 최종안 04 위치 권한: 왜 위치가 필요한지 설명하고 권한을 요청한다
// 허용하든 거절하든 나중에 하든 onDone 으로 다음 단계(우리 아이 설정)로 간다
@Composable
fun LocationPermissionScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Android 권한 팝업을 띄우는 도구. 사용자가 고르고 나면 { } 안이 실행된다
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        // 거절해도 지도는 기본 지역 중심으로 보여주면 되니 결과와 상관없이 넘어간다
        onDone()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        LocationIllustration()
        Spacer(Modifier.height(40.dp))
        Text(
            text = "주변의 무던한 곳을 찾으려면\n위치가 필요해요",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "위치는 가까운 장소를 보여줄 때만 써요.\n허용하지 않아도 지도를 둘러볼 수 있어요.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "위치 허용하기",
            onClick = {
                // 대략 위치와 정확한 위치를 같이 물어본다. 사용자가 둘 중 하나를 고를 수 있다
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                )
            },
        )
        Spacer(Modifier.height(6.dp))
        GhostButton(text = "나중에 할게요", onClick = onDone)
    }
}

// 지도 그림 위에 내 위치 점과 정확도 원
@Composable
private fun LocationIllustration() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(Radius.illustration),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.illust_location_map),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.size(120.dp)) {
            drawCircle(BrandPrimary.copy(alpha = 0.14f))
            drawCircle(BgSurface, radius = 12.dp.toPx())
            drawCircle(BrandPrimary, radius = 10.dp.toPx() - 2.dp.toPx())
            drawCircle(BgSurface, radius = 10.dp.toPx(), style = Stroke(4.dp.toPx()))
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LocationPermissionScreenPreview() {
    CalmMapTheme { LocationPermissionScreen(onDone = {}) }
}
