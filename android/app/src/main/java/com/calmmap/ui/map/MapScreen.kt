package com.calmmap.ui.map

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalTime
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.MicIcon
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.SearchBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.Line
import com.calmmap.ui.theme.LineStrong
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.SensoryLevel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage

/** 홍대입구역. 측정 권역이 홍대 관광특구라 그 근처를 처음 화면으로 둔다. */
private val HONGDAE = LatLng(37.5566, 126.9237)
private const val DEFAULT_ZOOM = 15.0

/**
 * 와이어프레임 B1 지도 홈.
 *
 * 위에서부터 검색창 · 필터 칩 · 방문 예정 배너 · 지도 · 가까운 장소 목록 · 하단 탭.
 *
 * 장소는 아직 임시 데이터다. 10/18 답사에서 가게를 정하면 DB 에서 읽어온다.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
    onSearchClick: () -> Unit = {},
    onPlaceClick: (PlaceUi) -> Unit = {},
    onMeasureClick: () -> Unit = {},
    onTabSelect: (MainTab) -> Unit = {},
    /** 정해 둔 방문. 있으면 위쪽에 '방문 예정' 띠가 뜬다 */
    visitPlan: VisitPlan? = null,
    onVisitClick: (PlaceUi) -> Unit = {},
    /** 방문 예정 띠를 눌렀을 때. 미리 보는 카드를 연다 */
    onVisitPlanClick: () -> Unit = {},
    onVisitDoneClick: () -> Unit = {},
) {
    var personalized by remember { mutableStateOf(false) }
    // 장소를 고르면 아래 목록이 그 장소 카드(B2)로 바뀐다
    var selectedPlace by remember { mutableStateOf<PlaceUi?>(null) }
    val nowHour = remember { LocalTime.now().hour }

    // 소리 · 빛 정보가 없는 장소는 카드를 거치지 않고 바로 상세로 보낸다.
    // 카드의 알맹이가 권고 시간대와 그래프인데 둘 다 없어서
    // 이름과 버튼만 남은 빈 카드가 올라온다
    fun pick(place: PlaceUi) {
        if (place.level == SensoryLevel.NO_DATA) onPlaceClick(place)
        else selectedPlace = place
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding(),
    ) {
        // 장소를 고르면 아래 카드가 커진다. 칩과 배너를 접어 자리를 내준다
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchBar(onClick = onSearchClick)
            if (selectedPlace == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(label = "지금", selected = true, onClick = {})
                    FilterChip(label = "시간 선택 ▾", selected = false, onClick = {})
                    FilterChip(
                        label = "우리 아이 맞춤",
                        selected = personalized,
                        onClick = { personalized = !personalized },
                    )
                }
            }
        }

        if (selectedPlace == null && visitPlan != null) {
            VisitBanner(
                placeName = visitPlan.place.name,
                whenText = "${visitPlan.day.label} ${visitPlan.hour}시",
                onClick = onVisitPlanClick,
                onDoneClick = onVisitDoneClick,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            NaverMapHost(
                places = places,
                selectedPlace = selectedPlace,
                onPlaceClick = { pick(it) },
                onMapClick = { selectedPlace = null },
                modifier = Modifier.fillMaxSize(),
            )
            Legend(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            )
            MeasureButton(
                onClick = onMeasureClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )
        }

        val picked = selectedPlace
        if (picked == null) {
            NearbySheet(
                // 제목이 '무던한 시간이 있는 곳' 이므로 부담 높음과 정보 부족은 뺀다.
                // 지금은 현재 단계로만 거르지만, hourly_forecast 를 붙이면
                // '오늘 중에 무던한 시간대가 하나라도 있는가' 로 바꾼다
                places = places.filter {
                    it.level != SensoryLevel.NO_DATA && it.level != SensoryLevel.HIGH
                },
                onPlaceClick = { pick(it) },
            )
        } else {
            PlaceSheet(
                place = picked,
                nowHour = nowHour,
                onDetailClick = { onPlaceClick(picked) },
                onVisitClick = { onVisitClick(picked) },
            )
        }

        BottomTabBar(selected = MainTab.MAP, onSelect = onTabSelect)
    }
}

/**
 * 방문을 확정한 장소가 있을 때 위쪽에 뜨는 띠.
 * 띠를 누르면 미리 보는 카드(출발하러 가는 길), '다녀왔어요' 는 다녀온 뒤 기록으로 가는 길이다
 */
@Composable
private fun VisitBanner(
    placeName: String,
    whenText: String,
    onClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(LineStrong)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$placeName · $whenText 방문 예정 ›",
            style = MaterialTheme.typography.bodySmall,
            color = Paper,
        )
        Box(
            modifier = Modifier
                .pressFade(role = Role.Button, onClick = onDoneClick)
                .clip(RoundedCornerShape(6.dp))
                .background(Paper)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "다녀왔어요",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
        }
    }
}

/** 지도 위 왼쪽 위 범례. 색이 아니라 진하기로 읽는 법을 알려준다 */
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Text(
        text = "연함=무던함 · 중간=보통 · 진함=부담 높음 · 점선=정보 부족 · 테두리=후보",
        style = MaterialTheme.typography.labelSmall,
        color = Ink,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Paper.copy(alpha = 0.85f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun MeasureButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(34.dp)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(shape)
            .background(Paper)
            .outlineBorder(LineStrong, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MicIcon(size = 14.dp, color = Ink)
        Text(
            text = "측정",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
    }
}

/**
 * 아래에서 올라오는 가까운 장소 목록.
 *
 * 지도에서 마커를 하나씩 눌러보지 않아도 가까운 곳을 훑을 수 있게 한다.
 * 아직 끌어올리는 동작은 없고 접힌 모습만 그린다.
 */
@Composable
private fun NearbySheet(
    places: List<PlaceUi>,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Paper)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Line),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "오늘 무던한 시간이 있는 곳 ${places.size}",
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
            )
            Text(
                text = "가까운 순",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
        places.forEachIndexed { index, place ->
            PlaceRow(
                name = place.name,
                description = place.description,
                level = place.level,
                onClick = { onPlaceClick(place) },
                showDivider = index != places.lastIndex,
            )
        }
    }
}

/**
 * 네이버 지도와 그 위의 장소 마커.
 *
 * 마커는 지도가 준비된 뒤에 붙인다. 지도 엔진이 뜨는 데 시간이 걸려서
 * 화면이 그려지는 시점에는 아직 붙일 대상이 없다.
 */
@Composable
private fun NaverMapHost(
    places: List<PlaceUi>,
    selectedPlace: PlaceUi?,
    onPlaceClick: (PlaceUi) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val markers = remember { mutableStateListOf<Marker>() }

    // 단계는 다섯 가지뿐이라 그림도 다섯 장만 만들어 두고 돌려 쓴다
    val icons = remember(density) {
        SensoryLevel.entries.associateWith { OverlayImage.fromBitmap(markerBitmap(it, density)) }
    }
    // 고른 장소는 크고 두꺼운 그림으로 바꿔 어느 것을 눌렀는지 보이게 한다
    val selectedIcons = remember(density) {
        SensoryLevel.entries.associateWith {
            OverlayImage.fromBitmap(markerBitmap(it, density, selected = true))
        }
    }

    NaverMapView(
        modifier = modifier,
        onMapReady = { naverMap = it },
    )

    LaunchedEffect(naverMap, places, selectedPlace) {
        val map = naverMap ?: return@LaunchedEffect
        if (markers.isEmpty()) {
            // 처음 한 번만 홍대로 맞춘다. 매번 하면 사용자가 옮긴 지도가 되돌아간다
            map.cameraPosition = CameraPosition(HONGDAE, DEFAULT_ZOOM)
        }
        map.setOnMapClickListener { _, _ -> onMapClick() }

        // 장소 목록이 바뀌면 옛 마커를 먼저 떼어낸다. 안 떼면 겹쳐서 쌓인다
        markers.forEach { it.map = null }
        markers.clear()

        places.forEach { place ->
            markers += Marker().apply {
                position = place.position
                val isPicked = place.id == selectedPlace?.id
                icon = if (isPicked) selectedIcons.getValue(place.level)
                else icons.getValue(place.level)
                zIndex = if (isPicked) 1 else 0
                captionText = place.name
                captionTextSize = 12f
                captionColor = Ink.toArgb()
                captionHaloColor = Paper.toArgb()
                // 지도가 원래 그리는 글자(역 이름 · 거리 이름)와 자리가 겹치면
                // 우리 장소 이름이 안 보인다. 우리 것을 우선한다
                isHideCollidedSymbols = true
                isHideCollidedCaptions = false
                isForceShowIcon = true
                setOnClickListener {
                    onPlaceClick(place)
                    true
                }
                this.map = map
            }
        }
    }
}

/**
 * MapView 를 Compose 에서 쓰기 위한 감싸개.
 *
 * MapView 는 Compose 가 나오기 전에 만들어진 뷰라서, 화면이 켜지고 꺼질 때마다
 * 생명주기를 직접 알려줘야 한다. 안 알려주면 지도가 안 그려지거나,
 * 화면을 벗어나도 메모리를 붙잡고 있는다.
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
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                mapView.onDestroy()
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view -> view.getMapAsync { map -> onMapReady(map) } },
    )
}
