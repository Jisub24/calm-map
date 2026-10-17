package com.calmmap.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmmap.ui.components.PrimaryButton
import com.calmmap.ui.components.SecondaryButton
import com.calmmap.ui.theme.CalmMapTheme

// A4 위치 권한: 왜 위치가 필요한지 설명하고 권한을 요청한다
// 허용하든 거절하든 나중에 하든 onDone 으로 다음 단계(아이 프로필)로 간다
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
            .background(Color.White)
            // 아래 내비게이션 바(◁ ○ □)에 버튼이 가리지 않게 그 높이만큼 띄운다
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 위아래 Spacer 로 설명 묶음을 남는 공간 가운데에 둔다
        Spacer(modifier = Modifier.weight(1f))

        // 위치 핀 아이콘 자리. 아이콘이 나오면 Icon 으로 바꾼다
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "아이콘: 위치 핀", fontSize = 12.sp, color = Color(0xFF808080))
        }
        Text(
            text = "주변의 무던한 곳을\n찾으려면 위치가 필요해요",
            fontSize = 20.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "위치는 지도 표시에만 써요. 언제든 설정에서 끌 수 있어요.",
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
            color = Color(0xFF737373),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "위치 허용하기",
            onClick = {
                // 대략 위치와 정확한 위치를 같이 물어본다. 사용자가 둘 중 하나를 고를 수 있다
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ),
                )
            },
        )
        SecondaryButton(text = "나중에 할게요", onClick = onDone)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LocationPermissionScreenPreview() {
    CalmMapTheme {
        LocationPermissionScreen(onDone = {})
    }
}
