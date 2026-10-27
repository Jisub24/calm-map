package com.calmmap.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmmap.R
import com.calmmap.ui.theme.TextPrimary

/**
 * 피그마 최종안 02 Icons. 24 크기 · 선 1.8 · 둥근 끝.
 * 파일은 res/drawable/ic_*.xml (피그마에서 SVG 로 내보내 변환). 이름은 피그마 Icon/이름 그대로.
 *
 * 화면 UI 아이콘만 여기 둔다. 지도 마커는 지도 SDK 에 비트맵으로 넘겨야 해서
 * map/MarkerIcon.kt 에서 따로 그린다.
 */
enum class CalmIcons(@DrawableRes val res: Int) {
    ArrowLeft(R.drawable.ic_arrow_left),
    Search(R.drawable.ic_search),
    Map(R.drawable.ic_map),
    History(R.drawable.ic_history),
    User(R.drawable.ic_user),
    Mic(R.drawable.ic_mic),
    ChevronRight(R.drawable.ic_chevron_right),
    ChevronDown(R.drawable.ic_chevron_down),
    Locate(R.drawable.ic_locate),
    Bell(R.drawable.ic_bell),
    Info(R.drawable.ic_info),
    Volume(R.drawable.ic_volume),
    Sun(R.drawable.ic_sun),
    Users(R.drawable.ic_users),
    Lock(R.drawable.ic_lock),
    Check(R.drawable.ic_check),
    X(R.drawable.ic_x),
    Store(R.drawable.ic_store),
    Headphones(R.drawable.ic_headphones),
    Alert(R.drawable.ic_alert),
    Calendar(R.drawable.ic_calendar),
    Plus(R.drawable.ic_plus),
    Clock(R.drawable.ic_clock),
    Pin(R.drawable.ic_pin),
    Music(R.drawable.ic_music),
    MusicOff(R.drawable.ic_music_off),
    VolumeOff(R.drawable.ic_volume_off),
    VolumeLow(R.drawable.ic_volume_low),
    Bolt(R.drawable.ic_bolt),
    Warning(R.drawable.ic_warning),
    Machine(R.drawable.ic_machine),
    LightDim(R.drawable.ic_light_dim),
    WifiOff(R.drawable.ic_wifi_off),

    /** 카카오 로고. 색을 바꾸지 않는다 */
    Kakao(R.drawable.ic_kakao),
}

/**
 * 아이콘 하나. 기본 24dp · 본문 색.
 * 장식용이면 contentDescription 을 비워 두고, 아이콘만 있는 버튼이면 꼭 적는다 (화면 읽기).
 */
@Composable
fun CalmIcon(
    icon: CalmIcons,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = TextPrimary,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(icon.res),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = if (icon == CalmIcons.Kakao) Color.Unspecified else tint,
    )
}
