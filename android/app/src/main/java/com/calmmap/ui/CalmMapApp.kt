package com.calmmap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.detail.NoDataScreen
import com.calmmap.ui.detail.PlaceDetailScreen
import com.calmmap.ui.map.MapScreen
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.VisitUi
import com.calmmap.ui.model.sampleVisits
import com.calmmap.ui.model.sampleDetailFor
import com.calmmap.ui.model.sampleNoDataFor
import com.calmmap.ui.mypage.DataInfoScreen
import com.calmmap.ui.mypage.MyPageScreen
import com.calmmap.ui.mypage.VisitHistoryScreen
import com.calmmap.ui.search.SearchScreen
import com.calmmap.ui.visit.AfterVisitScreen
import com.calmmap.ui.visit.ChildViewScreen
import com.calmmap.ui.visit.OnSiteScreen
import com.calmmap.ui.visit.VisitCardScreen
import com.calmmap.ui.visit.VisitTimeScreen
import java.time.LocalTime

/** 지금 보여줄 화면 */
private enum class Screen {
    MAP, SEARCH, DETAIL, HISTORY, ME, DATA_INFO,
    // 방문 계획 흐름: 시간 선택 → 미리 보는 카드 → (아이와 보기) → 현장 모드 → 방문 후 기록
    VISIT_TIME, VISIT_CARD, VISIT_CHILD, VISIT_ONSITE, VISIT_AFTER,
}

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

    // 방문 시간을 고를 장소와, 시간까지 정한 방문
    var visitPlace by remember { mutableStateOf<PlaceUi?>(null) }
    var visitPlan by remember { mutableStateOf<VisitPlan?>(null) }
    // 시간 선택에서 뒤로 가면 돌아갈 곳 (지도 카드 또는 장소 상세)
    var visitFrom by remember { mutableStateOf(Screen.MAP) }
    // TODO: visits 테이블에 저장하면 지금은 앱을 끄면 사라진다
    val visits = remember { mutableStateListOf<VisitUi>().apply { addAll(sampleVisits) } }

    fun startVisit(place: PlaceUi) {
        visitFrom = screen
        visitPlace = place
        screen = Screen.VISIT_TIME
    }

    fun openDetail(place: PlaceUi) {
        detailPlace = place
        screen = Screen.DETAIL
    }

    // 하단 탭을 누르면 그 탭의 첫 화면으로
    fun selectTab(tab: MainTab) {
        screen = when (tab) {
            MainTab.MAP -> Screen.MAP
            MainTab.HISTORY -> Screen.HISTORY
            MainTab.ME -> Screen.ME
        }
    }

    // 기기 뒤로 가기를 누르면 지도로 돌아온다. 데이터 안내는 내 정보로 돌아온다.
    // 이게 없으면 앱이 그냥 꺼져서 사용자가 보다 말고 튕긴 것처럼 느낀다
    BackHandler(enabled = screen != Screen.MAP) {
        screen = when (screen) {
            Screen.DATA_INFO -> Screen.ME
            Screen.VISIT_TIME -> visitFrom
            Screen.VISIT_CARD -> Screen.VISIT_TIME
            Screen.VISIT_CHILD, Screen.VISIT_ONSITE -> Screen.VISIT_CARD
            else -> Screen.MAP
        }
    }

    when (screen) {
        Screen.MAP -> MapScreen(
            onSearchClick = { screen = Screen.SEARCH },
            onPlaceClick = { openDetail(it) },
            onTabSelect = { selectTab(it) },
            visitPlan = visitPlan,
            onVisitClick = { startVisit(it) },
            onVisitDoneClick = { screen = Screen.VISIT_AFTER },
        )

        Screen.SEARCH -> SearchScreen(
            onBack = { screen = Screen.MAP },
            onPlaceClick = { openDetail(it) },
            onTabSelect = { selectTab(it) },
        )

        Screen.HISTORY -> VisitHistoryScreen(onTabSelect = { selectTab(it) }, visits = visits)

        Screen.ME -> MyPageScreen(
            onTabSelect = { selectTab(it) },
            onVisitHistoryClick = { screen = Screen.HISTORY },
            onDataInfoClick = { screen = Screen.DATA_INFO },
        )

        // TODO: 삭제는 확인 창을 띄운 뒤 Supabase 데이터와 계정을 지운다
        Screen.DATA_INFO -> DataInfoScreen(
            onBack = { screen = Screen.ME },
            onDeleteAllClick = {},
        )

        Screen.VISIT_TIME -> {
            val place = visitPlace
            if (place == null) {
                screen = Screen.MAP
            } else {
                VisitTimeScreen(
                    place = place,
                    nowHour = nowHour,
                    onBack = { screen = visitFrom },
                    onConfirm = { day, hour ->
                        visitPlan = VisitPlan(place, day, hour)
                        screen = Screen.VISIT_CARD
                    },
                )
            }
        }

        Screen.VISIT_CARD -> VisitCardScreen(
            onBack = { screen = Screen.VISIT_TIME },
            onChildViewClick = { screen = Screen.VISIT_CHILD },
            onDepartClick = { screen = Screen.VISIT_ONSITE },
        )

        Screen.VISIT_CHILD -> ChildViewScreen(onClose = { screen = Screen.VISIT_CARD })

        Screen.VISIT_ONSITE -> OnSiteScreen(
            placeName = visitPlan?.place?.name.orEmpty(),
            onEndClick = { screen = Screen.VISIT_AFTER },
        )

        Screen.VISIT_AFTER -> {
            val plan = visitPlan
            if (plan == null) {
                screen = Screen.MAP
            } else {
                AfterVisitScreen(
                    placeName = plan.place.name,
                    whenLabel = plan.whenLabel,
                    // 맨 위에 새 기록을 넣고 방문 예정을 지운다
                    // TODO: visits 테이블에 저장
                    onSubmit = { feeling, triggers ->
                        visits.add(
                            0,
                            VisitUi(
                                placeName = plan.place.name,
                                visitedAt = plan.recordLabel,
                                feeling = feeling,
                                trigger = triggers.joinToString(", ").ifEmpty { null },
                            ),
                        )
                        visitPlan = null
                        screen = Screen.MAP
                    },
                    onSkip = {
                        visitPlan = null
                        screen = Screen.MAP
                    },
                )
            }
        }

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
                    onVisitClick = { startVisit(place) },
                )
            }
        }
    }
}
