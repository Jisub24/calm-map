package com.calmmap.ui.map

import com.calmmap.R
import androidx.core.content.res.ResourcesCompat
import android.graphics.Typeface
import android.graphics.PointF
import com.calmmap.ui.component.CalmIcon
import com.calmmap.ui.component.CalmIcons
import android.os.Bundle
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.calmmap.ui.component.pressHighlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.calmmap.ui.component.GlyphStyle
import com.calmmap.ui.component.LevelGlyph
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.theme.BrandPrimary
import com.calmmap.ui.theme.LevelHigh
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextSecondary
import com.calmmap.ui.theme.caption
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.calmmap.ui.component.BottomTabBar
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.component.PlaceRow
import com.calmmap.ui.component.SearchBar
import com.calmmap.ui.component.outlineBorder
import com.calmmap.ui.component.pressFade
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.TimeSlot
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.samplePlaces
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.TextDisabled
import com.calmmap.ui.theme.BorderStrong
import com.calmmap.ui.theme.BgSurface
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

/** 접힌 시트 높이 (최종안 88) */
private val SheetPeek = 88.dp

/**
 * 최종안 06 지도 홈 · 06-1 방문 예정이 있을 때 · 07 장소 카드.
 *
 * 지도가 화면을 꽉 채우고, 그 위에 검색창 · 칩 · 방문 예정 띠 · 측정 · 내 위치 버튼 · 아래 시트가 뜬다.
 *   아래 시트(접힘)  '지금 무던한 곳 N' + 범례. '전체 보기' 를 누르면 목록이 펼쳐진다
 *   장소를 누르면    시트가 장소 카드(07)로 바뀌고 칩 · 띠는 접힌다
 *   '시간 선택' 칩    시트가 시간 선택으로 바뀐다. 평일 · 주말과 시각을 고르면 마커 · 목록 · 카드가 그 시각 기준이 된다.
 *                    '지금' 칩을 누르면 다시 지금 기준
 *
 * 장소는 아직 임시 데이터다. 답사에서 가게를 정하면 DB 에서 읽어온다.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    places: List<PlaceUi> = samplePlaces,
    onSearchClick: () -> Unit = {},
    onPlaceClick: (PlaceUi) -> Unit = {},
    onTabSelect: (MainTab) -> Unit = {},
    /** 정해 둔 방문. 있으면 위쪽에 '방문 예정' 띠가 뜬다 */
    visitPlan: VisitPlan? = null,
    onVisitClick: (PlaceUi) -> Unit = {},
    /** 방문 예정 띠를 눌렀을 때. 미리 보는 카드를 연다 */
    onVisitPlanClick: () -> Unit = {},
    onVisitDoneClick: () -> Unit = {},
    /** 띠에 안 보이는 나머지 계획 수. 0 보다 크면 둘째 줄이 생긴다 */
    otherVisitCount: Int = 0,
    onOtherVisitsClick: () -> Unit = {},
    /** 지도의 '측정' 버튼. 장소 고르기 → 측정 중 */
    onMeasureClick: () -> Unit = {},
    /** 시간 선택에서 고른 때. null 이면 지금. [places] 는 이미 이 때 기준으로 만든 것이다 */
    slot: TimeSlot? = null,
    onSlotChange: (TimeSlot?) -> Unit = {},
) {
    var personalized by remember { mutableStateOf(false) }
    // 장소를 고르면 아래 시트가 그 장소 카드로 바뀐다
    var selectedPlace by remember { mutableStateOf<PlaceUi?>(null) }
    var listOpen by remember { mutableStateOf(false) }
    var recenterKey by remember { mutableIntStateOf(0) }
    val nowHour = remember { LocalTime.now(ZoneId.of("Asia/Seoul")).hour }
    val todayWeekend = remember { LocalDate.now(ZoneId.of("Asia/Seoul")).dayOfWeek.value >= 6 }
    // 시간 선택 시트가 떠 있는지. 뒤로 가기를 누르면 앱이 아니라 시트가 닫힌다
    var timeOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = timeOpen) { timeOpen = false }

    // 어느 장소든 카드를 먼저 연다. 점수가 한 칸도 없어도 "평일에 잰 기록은 아직 없어요 · 주말에는 3번 쟀어요"
    // 처럼 알릴 말이 있고, 장소마다 누를 때 다른 화면이 뜨면 헷갈린다. 상세 보기에서 정보 부족 화면으로 간다
    fun pick(place: PlaceUi) {
        timeOpen = false
        selectedPlace = place
    }

    // '지금 무던한 곳' 은 무던함만 센다. 보통 · 후보는 '그 밖의 장소'.
    // 무던함은 실측으로 확인된 시간대에만 쓴다 (답변서2 질의1)
    val calmNow = places.filter { it.level == SensoryLevel.CALM }

    Column(modifier = modifier.fillMaxSize().background(BgSurface)) {
        Box(modifier = Modifier.weight(1f)) {
            NaverMapHost(
                places = places,
                selectedPlace = selectedPlace,
                onPlaceClick = { pick(it) },
                onMapClick = {
                    selectedPlace = null
                    listOpen = false
                    timeOpen = false
                },
                recenterKey = recenterKey,
                bottomInset = SheetPeek,
                modifier = Modifier.fillMaxSize(),
            )

            // 상태바 글자가 지도 위에서도 읽히도록 위쪽을 흰색으로 흐리게 덮는다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Brush.verticalGradient(listOf(BgSurface.copy(alpha = 0.92f), BgSurface.copy(alpha = 0f)))),
            )

            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchBar(onClick = onSearchClick)
                if (selectedPlace == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            label = "지금",
                            selected = slot == null,
                            onClick = {
                                onSlotChange(null)
                                timeOpen = false
                            },
                        )
                        // 고른 뒤에는 칩에 고른 때를 적는다. "주말 15시"
                        FilterChip(
                            label = slot?.label ?: "시간 선택",
                            selected = slot != null,
                            onClick = {
                                listOpen = false
                                timeOpen = true
                            },
                            trailingIcon = CalmIcons.ChevronDown,
                        )
                        FilterChip(
                            label = "우리 아이 맞춤",
                            selected = personalized,
                            onClick = { personalized = !personalized },
                        )
                    }
                    if (visitPlan != null) {
                        VisitBanner(
                            placeName = visitPlan.place.name,
                            whenText = "${visitPlan.day.label} ${visitPlan.hour}시",
                            otherCount = otherVisitCount,
                            onClick = onVisitPlanClick,
                            onDoneClick = onVisitDoneClick,
                            onOtherClick = onOtherVisitsClick,
                        )
                    }
                }
            }

            val picked = selectedPlace
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                if (picked == null && timeOpen) {
                    TimeSlotSheet(
                        initial = slot ?: TimeSlot(weekend = todayWeekend, hour = nowHour.coerceIn(8, 22)),
                        onChange = onSlotChange,
                        onClose = { timeOpen = false },
                    )
                } else if (picked == null) {
                    // 오른쪽 아래: 측정 · 내 위치. 둘 다 지도 위에 뜨는 흰 버튼
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 시간 선택 중에는 흐리게. 누르면 '지금' 으로 바꾸라고 알린다
                        MeasureButton(enabled = slot == null, onClick = onMeasureClick)
                        // TODO: 실제 내 위치를 붙이면 그 자리로 옮긴다. 지금은 처음 위치(홍대)로 돌아간다
                        LocateButton(onClick = { recenterKey++ })
                    }
                    NearbySheet(
                        whenLabel = slot?.label ?: "지금",
                        places = calmNow,
                        otherCount = places.size - calmNow.size,
                        open = listOpen,
                        onToggle = { listOpen = !listOpen },
                        onPlaceClick = { pick(it) },
                    )
                } else {
                    PlaceSheet(
                        place = picked,
                        nowHour = nowHour,
                        onDetailClick = { onPlaceClick(picked) },
                        onVisitClick = { onVisitClick(picked) },
                        onDismiss = { selectedPlace = null },
                    )
                }
            }
        }

        BottomTabBar(selected = MainTab.MAP, onSelect = onTabSelect)
    }
}

/**
 * 방문 예정 띠 (최종안 06-1). 진한 남색 바탕에 흰 글자.
 * 띠를 누르면 미리 보는 카드(출발하러 가는 길), '다녀왔어요' 는 다녀온 뒤 기록으로 가는 길이다.
 *
 * 계획이 여러 개여도 가장 가까운 하나만 보여준다. 띠를 여러 줄 쌓으면 지도가 가려진다.
 * 나머지가 있으면 시각 줄 끝 "N곳 더" 로 목록을 연다
 */
@Composable
private fun VisitBanner(
    placeName: String,
    whenText: String,
    otherCount: Int,
    onClick: () -> Unit,
    onDoneClick: () -> Unit,
    onOtherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calmShadow(Shadows.float, Radius.tile)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Radius.tile)
            .background(LevelHigh)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 장소 이름이 길어 한 줄에 시각까지 넣으면 잘려서, 이름과 시각을 두 줄로 나눈다.
        // 다른 예정이 있으면 셋째 줄을 만들지 않고 시각 줄 끝에 "1곳 더" 를 붙인다
        Column(
            modifier = Modifier
                .weight(1f)
                .pressFade(role = Role.Button, onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = placeName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = BgSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$whenText 방문 예정 ›",
                    style = MaterialTheme.typography.caption,
                    color = BgSurface,
                )
                if (otherCount > 0) {
                    Text(
                        text = "  ·  ${otherCount}곳 더 ›",
                        style = MaterialTheme.typography.caption,
                        color = BorderStrong,
                        // 띠 전체가 카드로 가는 버튼이라, 이 글자만 따로 눌러야 목록으로 간다
                        modifier = Modifier.pressFade(role = Role.Button, onClick = onOtherClick),
                    )
                }
            }
        }
        Text(
            text = "다녀왔어요",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier
                .pressFade(role = Role.Button, onClick = onDoneClick)
                .clip(Radius.pill)
                .background(BgSurface)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/** 지도 오른쪽 아래 측정 버튼. 내 위치 버튼과 같은 흰 바탕 · 높이 44 · Float 그림자 */
@Composable
private fun MeasureButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .calmShadow(Shadows.float, Radius.pill)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Radius.pill)
            .background(BgSurface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIcon(CalmIcons.Mic, size = 18.dp, tint = if (enabled) BrandPrimary else TextDisabled)
        Text(text = "측정", style = MaterialTheme.typography.labelLarge, color = if (enabled) TextPrimary else TextDisabled)
    }
}

/** 지도 오른쪽 아래 내 위치 버튼 (흰 원 44 · Float 그림자) */
@Composable
private fun LocateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .calmShadow(Shadows.float, Radius.pill)
            .pressFade(role = Role.Button, onClick = onClick)
            .clip(Radius.pill)
            .background(BgSurface),
        contentAlignment = Alignment.Center,
    ) {
        CalmIcon(CalmIcons.Locate, size = 20.dp, contentDescription = "내 위치")
    }
}

/**
 * 아래 시트 (최종안 06). 접히면 '지금 무던한 곳 N · 그 밖의 장소 M' 과 범례만,
 * '전체 보기' 를 누르면 가까운 순 목록이 펼쳐진다. 시간을 골랐으면 '주말 15시 무던한 곳 N'.
 */
@Composable
private fun NearbySheet(
    /** "지금" 또는 고른 때 "주말 15시" */
    whenLabel: String,
    places: List<PlaceUi>,
    otherCount: Int,
    open: Boolean,
    onToggle: () -> Unit,
    onPlaceClick: (PlaceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .calmShadow(Shadows.sheet, Radius.sheet)
            .clip(Radius.sheet)
            .background(BgSurface)
            // 시트 위에서 끌면 시트가 움직여야지 뒤의 지도가 움직이면 안 된다.
            // 위로 끌면 펼치고 아래로 끌면 접는다. 빈 곳을 눌러도 지도로 넘어가지 않게 터치를 여기서 끝낸다
            .sheetDrag(open = open, onOpen = onToggle, onClose = onToggle)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "$whenLabel 무던한 곳 ", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(text = "${places.size}", style = MaterialTheme.typography.titleLarge, color = BrandPrimary)
            Text(
                text = " · 그 밖의 장소 $otherCount",
                style = MaterialTheme.typography.caption,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 글자 대신 화살표. 손잡이 바와 같은 뜻이라 "전체 보기 · 접기" 글자 없이도 읽힌다.
            // 열리면 180도 돌아 위를 가리킨다
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pressHighlight(shape = Radius.pill, role = Role.Button, onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                CalmIcon(
                    CalmIcons.ChevronDown,
                    size = 20.dp,
                    tint = TextSecondary,
                    contentDescription = if (open) "목록 접기" else "목록 펼치기",
                    modifier = Modifier.rotate(if (open) 180f else 0f),
                )
            }
        }
        LevelLegend(modifier = Modifier.padding(top = 8.dp))
        if (open) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                places.forEachIndexed { i, place ->
                    PlaceRow(
                        name = place.name,
                        description = place.description,
                        level = place.level,
                        onClick = { onPlaceClick(place) },
                        showDivider = i != places.lastIndex,
                    )
                }
            }
        }
    }
}

/**
 * 시트 위에서의 세로 끌기. 40dp 넘게 위로 끌면 onOpen, 아래로 끌면 onClose.
 *
 * 끌기 이벤트를 여기서 소비하지 않으면 그대로 뒤의 네이버 지도로 내려가서
 * 시트를 잡아 올리려는데 지도가 밀린다.
 */
internal fun Modifier.sheetDrag(open: Boolean, onOpen: () -> Unit, onClose: () -> Unit): Modifier =
    pointerInput(open) {
        var dragged = 0f
        val threshold = 40.dp.toPx()
        detectVerticalDragGestures(
            onDragStart = { dragged = 0f },
            onVerticalDrag = { change, dy ->
                change.consume()
                dragged += dy
            },
            onDragEnd = {
                if (dragged < -threshold && !open) onOpen()
                if (dragged > threshold && open) onClose()
            },
        )
    }

/** 시트 맨 위 손잡이 36×4 */
@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(BorderStrong),
    )
}

/** 지도 범례. 색이 아니라 진하기 · 테두리로 읽는 법을 알려준다 */
@Composable
private fun LevelLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SensoryLevel.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LevelGlyph(level, style = GlyphStyle.Dot, size = 10.dp)
                Text(text = level.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
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
    /** 값이 바뀔 때마다 지도를 처음 위치로 옮긴다 (내 위치 버튼) */
    recenterKey: Int = 0,
    /** 지도 아래쪽을 가리는 시트 높이. 네이버 로고 · 축척이 그 위로 올라오게 한다 */
    bottomInset: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    // 마커 글자도 앱 글꼴로
    val markerFont = remember { ResourcesCompat.getFont(context, R.font.noto_sans_kr) ?: Typeface.DEFAULT_BOLD }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val markers = remember { mutableStateListOf<Marker>() }
    // 방금 누른 마커. 정보 부족 장소는 카드 없이 바로 상세로 가서 눌린 티가 안 났다.
    // 잠깐 진한 알약으로 바꿔 보여준 뒤 넘어간다
    var pressedId by remember { mutableStateOf<Any?>(null) }
    val scope = rememberCoroutineScope()

    // 마커에 장소 이름이 들어가서 장소마다 한 장씩 만든다. 목록이 바뀔 때만 다시 만든다
    val icons = remember(places, density) {
        places.associate { it.id to markerBitmap(it.level, it.name, density, typeface = markerFont) }
    }
    // 고른 장소는 진한 바탕으로 바꿔 어느 것을 눌렀는지 보이게 한다
    val selectedIcons = remember(places, density) {
        places.associate { it.id to markerBitmap(it.level, it.name, density, selected = true, typeface = markerFont) }
    }

    NaverMapView(
        modifier = modifier,
        onMapReady = { naverMap = it },
    )

    LaunchedEffect(naverMap, recenterKey) {
        val map = naverMap ?: return@LaunchedEffect
        if (recenterKey > 0) map.moveCamera(CameraUpdate.scrollTo(HONGDAE).animate(CameraAnimation.Easing))
    }

    LaunchedEffect(naverMap, bottomInset) {
        val map = naverMap ?: return@LaunchedEffect
        // 최종안에는 +/- 확대 버튼이 없다. 손가락으로 확대한다
        map.uiSettings.isZoomControlEnabled = false
        map.setContentPadding(0, 0, 0, with(density) { bottomInset.roundToPx() })
    }

    LaunchedEffect(naverMap, places, selectedPlace, pressedId) {
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
                val isPicked = place.id == selectedPlace?.id || place.id == pressedId
                val image = if (isPicked) selectedIcons.getValue(place.id) else icons.getValue(place.id)
                icon = OverlayImage.fromBitmap(image.bitmap)
                // 알약의 단계 점이 장소 위치에 오게 한다 (기본은 아래 끝 가운데)
                anchor = PointF(image.anchorX, image.anchorY)
                // 겹침 숨기기는 쓰지 않는다. 기본 화면에서 5곳이 다 보여야 한다.
                // 위에 오는 순서: 고른 것 > 단계 > 후보 > 정보 부족
                zIndex = when {
                    isPicked -> 10
                    place.level == SensoryLevel.NO_DATA -> 1
                    place.level == SensoryLevel.CANDIDATE -> 2
                    else -> 3
                }
                // 지도가 원래 그리는 글자(역 이름 · 거리 이름)와 겹치면 우리 마커를 우선한다
                isHideCollidedSymbols = true
                isForceShowIcon = true
                setOnClickListener {
                    pressedId = place.id
                    scope.launch {
                        delay(120)
                        onPlaceClick(place)
                        delay(300)
                        pressedId = null
                    }
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
internal fun NaverMapView(
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
