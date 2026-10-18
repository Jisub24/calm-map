package com.calmmap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calmmap.ui.detail.NoDataScreen
import com.calmmap.ui.detail.PlaceDetailScreen
import com.calmmap.ui.map.MapScreen
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.sampleDetailFor
import com.calmmap.ui.model.sampleNoDataFor
import com.calmmap.ui.search.SearchScreen
import java.time.LocalTime

/** 지금 보여줄 화면 */
private enum class Screen { MAP, SEARCH, DETAIL }

/**
 * 어느 화면을 보여줄지 정하는 자리.
 *
 * 화면이 셋뿐이라 Navigation 라이브러리를 아직 쓰지 않는다.
 * 라이브러리를 넣으면 화면마다 주소를 붙이고 인자 전달 규칙을 정해야 하는데,
 * 지금은 그만한 복잡함이 필요 없다.
 *
 * 스플래시 · 온보딩(#19)이 병합되어 화면이 예닐곱 개가 되면 그때 바꾼다.
 * 그때도 MainActivity 는 손대지 않고 이 파일만 고치면 된다.
 */
@Composable
fun CalmMapApp() {
    var screen by remember { mutableStateOf(Screen.MAP) }
    // 상세로 들어갈 때 어느 장소인지 들고 간다
    var detailPlace by remember { mutableStateOf<PlaceUi?>(null) }
    val nowHour = remember { LocalTime.now().hour }

    fun openDetail(place: PlaceUi) {
        detailPlace = place
        screen = Screen.DETAIL
    }

    // 기기 뒤로 가기를 누르면 지도로 돌아온다.
    // 이게 없으면 앱이 그냥 꺼져서 사용자가 보다 말고 튕긴 것처럼 느낀다
    BackHandler(enabled = screen != Screen.MAP) { screen = Screen.MAP }

    when (screen) {
        Screen.MAP -> MapScreen(
            onSearchClick = { screen = Screen.SEARCH },
            onPlaceClick = { openDetail(it) },
        )

        Screen.SEARCH -> SearchScreen(
            onBack = { screen = Screen.MAP },
            onPlaceClick = { openDetail(it) },
        )

        Screen.DETAIL -> {
            val place = detailPlace
            val detail = place?.let { sampleDetailFor(it.id) }
            val noData = place?.let { sampleNoDataFor(it.id) }

            when {
                place == null -> screen = Screen.MAP

                // 소리 · 빛 측정이 없는 장소는 점수를 내지 않는 화면으로 보낸다.
                // 같은 상세로 보내면 탭이 통째로 비어서 고장 난 것처럼 보인다
                detail == null && noData != null -> NoDataScreen(
                    place = place,
                    info = noData,
                    onBack = { screen = Screen.MAP },
                    onMeasureClick = {},
                )

                else -> PlaceDetailScreen(
                    place = place,
                    detail = detail,
                    nowHour = nowHour,
                    onBack = { screen = Screen.MAP },
                    onMeasureClick = {},
                    onVisitClick = {},
                )
            }
        }
    }
}
