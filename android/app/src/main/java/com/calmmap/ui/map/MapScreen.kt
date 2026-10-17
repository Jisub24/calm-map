package com.calmmap.ui.map

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

/**
 * 와이어프레임 B1 지도 홈의 바탕.
 *
 * 지금은 지도만 띄운다. 마커 · 검색창 · 바텀시트 · 하단 탭은 아직 없다.
 * 이 화면이 뜨는 것으로 NCP Client ID 와 패키지 이름이 맞는지 확인한다.
 * 둘 중 하나라도 틀리면 지도가 회색으로만 나온다.
 */

/** 홍대입구역. 측정 권역이 홍대 관광특구라 그 근처를 처음 화면으로 둔다. */
private val HONGDAE = LatLng(37.5566, 126.9237)
private const val DEFAULT_ZOOM = 15.0

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    NaverMapView(
        modifier = modifier.fillMaxSize(),
        onMapReady = { map ->
            map.cameraPosition = CameraPosition(HONGDAE, DEFAULT_ZOOM)
        },
    )
}

/**
 * 네이버 지도의 MapView 를 Compose 에서 쓰기 위한 감싸개.
 *
 * MapView 는 Compose 가 나오기 전에 만들어진 뷰라서, 화면이 켜지고 꺼질 때마다
 * 생명주기를 **직접 알려줘야 한다.** 안 알려주면 지도가 안 그려지거나,
 * 화면을 벗어나도 메모리를 붙잡고 있는다.
 *
 * 공식 SDK 를 그대로 쓴다. Compose 전용 커뮤니티 라이브러리도 있지만
 * 버전이 SDK 를 따라가지 못할 수 있어서 쓰지 않았다.
 */
@Composable
private fun NaverMapView(
    modifier: Modifier = Modifier,
    onMapReady: (NaverMap) -> Unit = {},
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        // MapView 가 요구하는 생명주기 호출을 Compose 쪽 생명주기에 연결한다
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 액티비티가 죽는 중이면 위의 ON_DESTROY 가 이미 지도를 정리했다.
            // 여기서 또 부르면 같은 것을 두 번 반납하는 꼴이라 건너뛴다.
            // 액티비티는 살아있는데 이 화면만 빠지는 경우에만 여기서 정리한다.
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                mapView.onDestroy()
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // 지도가 준비되면 불린다. 지도 엔진이 뜨는 데 시간이 걸려서 콜백으로 받는다
            view.getMapAsync { map -> onMapReady(map) }
        },
    )
}
