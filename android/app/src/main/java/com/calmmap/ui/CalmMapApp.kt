package com.calmmap.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.calmmap.data.AuthRepository
import com.calmmap.data.ProfileRepository
import com.calmmap.ui.component.LoginRequiredSheet
import com.calmmap.ui.component.MainTab
import com.calmmap.ui.detail.NoDataScreen
import com.calmmap.ui.detail.PlaceDetailScreen
import com.calmmap.ui.map.MapScreen
import com.calmmap.ui.measure.MeasurePlaceScreen
import com.calmmap.ui.measure.MeasureResultScreen
import com.calmmap.ui.measure.MeasuringScreen
import com.calmmap.ui.model.MeasureResult
import com.calmmap.ui.model.PlaceUi
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.VisitUi
import com.calmmap.ui.model.sampleVisits
import com.calmmap.ui.model.sampleDetailFor
import com.calmmap.ui.model.sampleNoDataFor
import com.calmmap.ui.mypage.AccountScreen
import com.calmmap.ui.mypage.DataInfoScreen
import com.calmmap.ui.onboarding.ChildProfileScreen
import com.calmmap.ui.onboarding.SensoryFactor
import com.calmmap.ui.onboarding.Sensitivity
import com.calmmap.ui.mypage.MyPageScreen
import com.calmmap.ui.mypage.VisitHistoryScreen
import com.calmmap.ui.search.SearchScreen
import com.calmmap.ui.visit.AfterVisitScreen
import com.calmmap.ui.visit.ChildViewScreen
import com.calmmap.ui.visit.OnSiteScreen
import com.calmmap.ui.visit.PlannedVisitsScreen
import com.calmmap.ui.visit.VisitCardScreen
import com.calmmap.ui.visit.VisitTimeScreen
import java.time.LocalTime
import kotlinx.coroutines.launch

/** 지금 보여줄 화면 */
private enum class Screen {
    MAP, SEARCH, DETAIL, HISTORY, ME, DATA_INFO,
    // 내 정보 안: 프로필 수정 · 우리 아이 수정 · 계정 관리
    CHILD_EDIT, ACCOUNT,
    // 방문 계획 흐름
    //   계획할 때: 시간 선택 → 지도 (방문 예정 띠가 생김)
    //   갈 때: 방문 예정 띠 → 미리 보는 카드 → (아이와 보기) → 현장 모드 → 방문 후 기록
    VISIT_TIME, VISIT_CARD, VISIT_CHILD, VISIT_ONSITE, VISIT_AFTER, VISIT_PLANS,
    // 측정 흐름: (장소 선택) → 측정 중 → 측정 결과
    MEASURE_PLACE, MEASURE_RUN, MEASURE_RESULT,
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
fun CalmMapApp(
    /** 로그인 없이 둘러보는 중인지. 그러면 측정 · 방문 계획 · 우리 아이 맞춤 앞에서 로그인 안내 시트를 띄운다 */
    isGuest: Boolean = false,
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var loginSheet by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.MAP) }
    // 상세로 들어갈 때 어느 장소인지 들고 간다
    var detailPlace by remember { mutableStateOf<PlaceUi?>(null) }
    val nowHour = remember { LocalTime.now().hour }

    // 방문 시간을 고를 장소
    var visitPlace by remember { mutableStateOf<PlaceUi?>(null) }
    // 정해 둔 방문들. 가까운 시간이 앞에 오게 정렬해 둔다
    // TODO: 저장하지 않아서 앱을 끄면 사라진다
    val visitPlans = remember { mutableStateListOf<VisitPlan>() }
    // 지금 카드 · 현장 모드 · 기록에서 다루는 방문
    var activePlan by remember { mutableStateOf<VisitPlan?>(null) }
    // 카드에서 뒤로 가면 돌아갈 곳 (지도 띠 또는 예정된 방문 목록)
    var cardFrom by remember { mutableStateOf(Screen.MAP) }
    // 시간 선택에서 뒤로 가면 돌아갈 곳 (지도 카드 또는 장소 상세)
    var visitFrom by remember { mutableStateOf(Screen.MAP) }
    // 예정된 방문 목록에서 뒤로 가면 돌아갈 곳 (지도 띠 또는 기록 탭)
    var plansFrom by remember { mutableStateOf(Screen.MAP) }
    // TODO: visits 테이블에 저장하면 지금은 앱을 끄면 사라진다
    val visits = remember { mutableStateListOf<VisitUi>().apply { addAll(sampleVisits) } }
    val scope = rememberCoroutineScope()
    // 카카오 프로필 이름. 로그인이 바뀌면 다시 읽는다
    val nickname = remember(isGuest) { AuthRepository.displayName() ?: "보호자" }
    // 저장된 우리 아이 설정. null 이면 아직 못 읽은 것
    var sensitivities by remember { mutableStateOf<Map<SensoryFactor, Sensitivity>?>(null) }
    var savingChild by remember { mutableStateOf(false) }
    // 설정을 읽다가 실패했는지. '불러오는 중' 대신 실패 안내를 띄운다
    var childLoadFailed by remember { mutableStateOf(false) }
    // 내 정보에 들어왔는데 아직 못 읽었으면 읽는다. 실패하면 다음에 들어올 때 다시 읽는다
    LaunchedEffect(isGuest, screen == Screen.ME) {
        if (isGuest || screen != Screen.ME || sensitivities != null) return@LaunchedEffect
        childLoadFailed = false
        sensitivities = try {
            // 줄이 없으면 아무것도 고르지 않은 것과 같다
            ProfileRepository.load() ?: emptyMap()
        } catch (e: Exception) {
            Log.e("CalmMap", "우리 아이 설정 불러오기 실패", e)
            childLoadFailed = true
            null
        }
    }

    fun startVisit(place: PlaceUi) {
        if (isGuest) {
            loginSheet = true
            return
        }
        visitFrom = screen
        visitPlace = place
        screen = Screen.VISIT_TIME
    }

    // 같은 장소를 다시 정하면 이전 계획을 바꾼다
    fun addPlan(plan: VisitPlan) {
        visitPlans.removeAll { it.place.id == plan.place.id }
        visitPlans.add(plan)
        visitPlans.sortWith(compareBy({ it.day.date }, { it.hour }))
    }

    fun openPlan(plan: VisitPlan, to: Screen) {
        activePlan = plan
        cardFrom = screen
        screen = to
    }

    // 다녀왔으면 그 계획만 빼고, 다음 계획이 띠에 올라온다
    fun finishPlan(plan: VisitPlan) {
        visitPlans.remove(plan)
        activePlan = null
        screen = Screen.MAP
    }

    // 측정할 장소, 끝난 측정, 측정을 시작한 곳 (끝나면 여기로 돌아간다)
    var measurePlace by remember { mutableStateOf<PlaceUi?>(null) }
    var measureResult by remember { mutableStateOf<MeasureResult?>(null) }
    var measureFrom by remember { mutableStateOf(Screen.MAP) }
    val context = LocalContext.current

    // 장소 상세에서는 장소가 정해져 있어서 바로 측정 중으로
    fun startMeasure(place: PlaceUi?) {
        if (isGuest) {
            loginSheet = true
            return
        }
        measureFrom = screen
        measurePlace = place
        screen = if (place == null) Screen.MEASURE_PLACE else Screen.MEASURE_RUN
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
            Screen.DATA_INFO, Screen.CHILD_EDIT, Screen.ACCOUNT -> Screen.ME
            Screen.VISIT_PLANS -> plansFrom
            Screen.VISIT_TIME -> visitFrom
            Screen.VISIT_CARD -> cardFrom
            // 측정 중에 뒤로 가면 측정을 버린다. 지도에서 시작했으면 장소 선택으로
            Screen.MEASURE_RUN -> if (measureFrom == Screen.MAP) Screen.MEASURE_PLACE else measureFrom
            Screen.MEASURE_PLACE, Screen.MEASURE_RESULT -> measureFrom
            Screen.VISIT_CHILD, Screen.VISIT_ONSITE -> Screen.VISIT_CARD
            else -> Screen.MAP
        }
    }

    // 화면 위에 로그인 안내 시트를 덮을 수 있게 Box 로 감싼다
    Box {
        when (screen) {
            Screen.MAP -> MapScreen(
                onSearchClick = { screen = Screen.SEARCH },
                onPlaceClick = { openDetail(it) },
                onTabSelect = { selectTab(it) },
                visitPlan = visitPlans.firstOrNull(),
                otherVisitCount = (visitPlans.size - 1).coerceAtLeast(0),
                onVisitClick = { startVisit(it) },
                onVisitPlanClick = { visitPlans.firstOrNull()?.let { openPlan(it, Screen.VISIT_CARD) } },
                onVisitDoneClick = { visitPlans.firstOrNull()?.let { openPlan(it, Screen.VISIT_AFTER) } },
                onOtherVisitsClick = {
                    plansFrom = Screen.MAP
                    screen = Screen.VISIT_PLANS
                },
                onMeasureClick = { startMeasure(null) },
            )

            Screen.SEARCH -> SearchScreen(
                onBack = { screen = Screen.MAP },
                onPlaceClick = { openDetail(it) },
            )

            Screen.HISTORY -> VisitHistoryScreen(
                onTabSelect = { selectTab(it) },
                visits = visits,
                plans = visitPlans,
                onPlanClick = { openPlan(it, Screen.VISIT_CARD) },
                onAllPlansClick = {
                    plansFrom = Screen.HISTORY
                    screen = Screen.VISIT_PLANS
                },
            )

            Screen.VISIT_PLANS -> PlannedVisitsScreen(
                plans = visitPlans,
                onBack = { screen = plansFrom },
                onPlanClick = { openPlan(it, Screen.VISIT_CARD) },
            )

            Screen.ME -> MyPageScreen(
                onTabSelect = { selectTab(it) },
                onDataInfoClick = { screen = Screen.DATA_INFO },
                isGuest = isGuest,
                nickname = nickname,
                sensitivities = sensitivities,
                sensitivitiesFailed = childLoadFailed,
                onLoginClick = onLoginClick,
                onChildEditClick = { screen = Screen.CHILD_EDIT },
                onAccountClick = { screen = Screen.ACCOUNT },
            )

            Screen.CHILD_EDIT -> ChildProfileScreen(
                initial = sensitivities.orEmpty(),
                confirmText = "저장",
                onBack = { screen = Screen.ME },
                // 저장이 끝나야 내 정보로. 실패하면 이 화면에 남는다
                onStart = { values ->
                    scope.launch {
                        savingChild = true
                        try {
                            ProfileRepository.save(values)
                            sensitivities = values
                            screen = Screen.ME
                        } catch (e: Exception) {
                            Log.e("CalmMap", "우리 아이 설정 저장 실패", e)
                            Toast.makeText(context, "저장하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                        }
                        savingChild = false
                    }
                },
                saving = savingChild,
            )

            Screen.ACCOUNT -> AccountScreen(
                onBack = { screen = Screen.ME },
                onLogout = onLogout,
                // TODO: 서버에서 계정과 데이터를 지우는 함수(Edge Function)가 생기면 붙인다
                onWithdraw = {
                    Toast.makeText(context, "회원 탈퇴는 아직 준비 중이에요", Toast.LENGTH_SHORT).show()
                },
                joinedAt = AuthRepository.joinedAt() ?: "-",
                visitCount = visits.size,
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
                            addPlan(VisitPlan(place, day, hour))
                            // 계획만 잡고 지도로. 갈 때 '방문 예정' 띠를 눌러 카드를 연다
                            screen = Screen.MAP
                        },
                    )
                }
            }

            Screen.VISIT_CARD -> VisitCardScreen(
                subtitle = activePlan?.let { "${it.place.name} · ${it.whenLabel}" }.orEmpty(),
                onBack = { screen = cardFrom },
                onChildViewClick = { screen = Screen.VISIT_CHILD },
                onDepartClick = { screen = Screen.VISIT_ONSITE },
                // 그 계획만 빼고 들어온 곳으로. 목록이 비면 지도로
                onCancelVisit = {
                    activePlan?.let { visitPlans.remove(it) }
                    activePlan = null
                    screen = if (cardFrom == Screen.VISIT_PLANS && visitPlans.isEmpty()) Screen.MAP else cardFrom
                },
            )

            Screen.VISIT_CHILD -> ChildViewScreen(onClose = { screen = Screen.VISIT_CARD })

            Screen.VISIT_ONSITE -> OnSiteScreen(
                placeName = activePlan?.place?.name.orEmpty(),
                onEndClick = { screen = Screen.VISIT_AFTER },
            )

            Screen.VISIT_AFTER -> {
                val plan = activePlan
                if (plan == null) {
                    screen = Screen.MAP
                } else {
                    AfterVisitScreen(
                        placeName = plan.place.name,
                        whenLabel = plan.whenLabel,
                        // 기록 탭 맨 위에 새 기록을 넣는다
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
                            finishPlan(plan)
                        },
                        onSkip = { finishPlan(plan) },
                    )
                }
            }

            Screen.MEASURE_PLACE -> MeasurePlaceScreen(
                onBack = { screen = measureFrom },
                onPlaceClick = {
                    measurePlace = it
                    screen = Screen.MEASURE_RUN
                },
            )

            Screen.MEASURE_RUN -> {
                val place = measurePlace
                if (place == null) {
                    screen = measureFrom
                } else {
                    MeasuringScreen(
                        place = place,
                        onBack = {
                            screen = if (measureFrom == Screen.MAP) Screen.MEASURE_PLACE else measureFrom
                        },
                        onFinish = {
                            measureResult = it
                            screen = Screen.MEASURE_RESULT
                        },
                    )
                }
            }

            Screen.MEASURE_RESULT -> {
                val result = measureResult
                if (result == null) {
                    screen = measureFrom
                } else {
                    MeasureResultScreen(
                        result = result,
                        // TODO: measurements 테이블에 저장
                        onSave = {
                            Toast.makeText(context, "측정을 저장했어요", Toast.LENGTH_SHORT).show()
                            measureResult = null
                            screen = measureFrom
                        },
                        onDiscard = {
                            measureResult = null
                            screen = measureFrom
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
                        onMeasureClick = { startMeasure(place) },
                    )

                    else -> PlaceDetailScreen(
                        place = place,
                        detail = detail,
                        nowHour = nowHour,
                        onBack = { screen = Screen.MAP },
                        onMeasureClick = { startMeasure(place) },
                        onVisitClick = { startVisit(place) },
                        canPersonalize = !isGuest,
                        onLoginNeeded = { loginSheet = true },
                    )
                }
            }
        }

        if (loginSheet) {
            LoginRequiredSheet(
                onKakaoClick = {
                    loginSheet = false
                    onLoginClick()
                },
                onDismiss = { loginSheet = false },
            )
        }
    }
}
