package com.calmmap.ui.map

import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.calmmap.R
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.SecondaryButton
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.SensoryLevel
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage

/** 가게 하나를 보여주는 확대. 길 이름까지 보여서 어느 지점인지 알 수 있다 */
private const val CONFIRM_ZOOM = 17.0

/**
 * 검색에서 고른 카카오 가게가 맞는지 지도에서 확인한다.
 * 같은 이름 지점이 많아서(스타벅스 홍대 근처만 여럿) 목록 글자만 보고 고르면 다른 지점에서 측정하게 된다.
 *
 *   측정하기   그 가게에서 측정을 시작한다
 *   아니에요   검색으로 돌아간다. 치던 검색어는 그대로 있다
 */
@Composable
fun PlaceConfirmScreen(
    place: PlaceUi,
    onMeasure: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val markerFont = remember { ResourcesCompat.getFont(context, R.font.noto_sans_kr) ?: Typeface.DEFAULT_BOLD }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    // 아래 카드 높이. 가게가 카드에 가리지 않게 지도 가운데를 그만큼 올린다
    var cardHeight by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        NaverMapView(modifier = Modifier.fillMaxSize(), onMapReady = { naverMap = it })

        LaunchedEffect(naverMap, cardHeight) {
            val map = naverMap ?: return@LaunchedEffect
            map.uiSettings.isZoomControlEnabled = false
            map.setContentPadding(0, 0, 0, cardHeight)
            map.cameraPosition = CameraPosition(place.position, CONFIRM_ZOOM)
        }
        LaunchedEffect(naverMap) {
            val map = naverMap ?: return@LaunchedEffect
            // 지도 마커와 같은 알약. 고른 것이라 진한 바탕
            val image = markerBitmap(SensoryLevel.NO_DATA, place.name, density, selected = true, typeface = markerFont)
            Marker().apply {
                position = place.position
                icon = OverlayImage.fromBitmap(image.bitmap)
                anchor = PointF(image.anchorX, image.anchorY)
                isForceShowIcon = true
                this.map = map
            }
        }

        // 왼쪽 위 뒤로 가기. 지도 위에 뜨는 흰 원 (내 위치 버튼과 같은 모양)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .calmShadow(Shadows.float, Radius.pill)
                .pressFade(role = Role.Button, onClick = onBack)
                .clip(Radius.pill)
                .background(BgSurface),
            contentAlignment = Alignment.Center,
        ) {
            CalmIcon(CalmIcons.ArrowLeft, size = 20.dp, contentDescription = "뒤로")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { cardHeight = it.height }
                .calmShadow(Shadows.sheet, Radius.sheet)
                .clip(Radius.sheet)
                .background(BgSurface)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "이 장소가 맞나요?", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(text = place.kakao?.summary ?: place.category, style = MaterialTheme.typography.caption, color = TextTertiary)
            Text(
                text = "측정을 저장하면 이 장소가 지도에 생겨요",
                style = MaterialTheme.typography.caption,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(text = "아니에요", onClick = onBack, modifier = Modifier.weight(1f))
                PrimaryButton(text = "측정하기", onClick = onMeasure, modifier = Modifier.weight(1f))
            }
        }
    }
}
