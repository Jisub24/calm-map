package com.calmmap.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.calmmap.data.AuthRepository
import com.calmmap.data.MeasurementRepository
import com.calmmap.data.PlaceDetailRepository
import com.calmmap.data.PlaceRepository
import com.calmmap.data.ProfileRepository
import com.calmmap.data.VisitRepository
import com.calmmap.data.brief
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
import com.calmmap.ui.map.PlaceConfirmScreen
import com.calmmap.ui.model.MEASURE_NOW_ONLY
import com.calmmap.ui.model.TimeSlot
import com.calmmap.ui.model.VisitPlan
import com.calmmap.ui.model.VisitUi
import com.calmmap.ui.model.visitDayOf
import com.calmmap.ui.model.PlaceDetailUi
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
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
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
    // 검색에서 고른 카카오 가게가 맞는지 지도에서 확인 → 측정 중
    PLACE_CONFIRM,
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
    // 지도 장소를 새로 읽을 때 같이 고친다. 앱을 켜 둔 채 시간이 지나도 '지금' 이 맞게
    var nowHour by remember { mutableIntStateOf(LocalTime.now(ZoneId.of("Asia/Seoul")).hour) }
    // 상세 탭(점수 · 소리 · 빛 · 사업장) 값. 장소를 고를 때마다 DB 에서 새로 읽는다
    var detail by remember { mutableStateOf<PlaceDetailUi?>(null) }
    // 상세 탭을 못 읽었는지. 그러면 '불러오는 중' 대신 실패 안내와 다시 시도 버튼을 띄운다
    var detailFailed by remember { mutableStateOf(false) }
    // 다시 시도를 누를 때마다 늘려서 아래 읽기를 다시 돌린다
    var detailRetry by remember { mutableIntStateOf(0) }
    // 상세 화면에 들어올 때마다 새로 읽는다. 장소가 바뀔 때만 읽으면, 같은 장소를 다시 열었을 때 예전 값이 남는다.
    // 지도에서 시간을 골랐으면 쌓인 평일/주말 기록의 그 시각 기준으로 읽는다
    // 같은 장소라도 고른 때(평일 · 주말)가 바뀌면 다시 읽는다 ("주말 기록 보기")
    LaunchedEffect(detailPlace?.id, detailPlace?.slot, screen == Screen.DETAIL, detailRetry) {
        if (screen != Screen.DETAIL) return@LaunchedEffect
        detail = null
        detailFailed = false
        val place = detailPlace ?: return@LaunchedEffect
        try {
            detail = PlaceDetailRepository.load(place, nowHour)
        } catch (e: CancellationException) {
            // 읽는 도중 상세를 떠난 것. 실패가 아니다
            throw e
        } catch (e: Exception) {
            Log.w("CalmMapApp", "장소 상세 불러오기 실패: ${e.brief()}")
            detailFailed = true
        }
    }

    // 방문 시간을 고를 장소
    var visitPlace by remember { mutableStateOf<PlaceUi?>(null) }
    // 정해 둔 방문들. 가까운 시간이 앞에 오게 정렬해 둔다
    // 정할 때 visits 에 저장하고, 앱을 켜면 visits 에서 다시 읽어 온다
    val visitPlans = remember { mutableStateListOf<VisitPlan>() }
    // 지금 카드 · 현장 모드 · 기록에서 다루는 방문
    var activePlan by remember { mutableStateOf<VisitPlan?>(null) }
    // 카드에서 뒤로 가면 돌아갈 곳 (지도 띠 또는 예정된 방문 목록)
    var cardFrom by remember { mutableStateOf(Screen.MAP) }
    // 시간 선택에서 뒤로 가면 돌아갈 곳 (지도 카드 또는 장소 상세)
    var visitFrom by remember { mutableStateOf(Screen.MAP) }
    // 예정된 방문 목록에서 뒤로 가면 돌아갈 곳 (지도 띠 또는 기록 탭)
    var plansFrom by remember { mutableStateOf(Screen.MAP) }
    // 다녀와서 기록한 방문. null 이면 아직 못 읽은 것
    var visits by remember(isGuest) { mutableStateOf<List<VisitUi>?>(if (isGuest) emptyList() else null) }
    var visitsFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 카카오 프로필 이름. 로그인이 바뀌면 다시 읽는다
    val nickname = remember(isGuest) { AuthRepository.displayName() ?: "보호자" }
    // 저장된 우리 아이 설정. null 이면 아직 못 읽은 것
    var sensitivities by remember { mutableStateOf<Map<SensoryFactor, Sensitivity>?>(null) }
    var savingChild by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // DB 에서 읽은 장소들. null 이면 아직 못 읽은 것
    var mapPlaces by remember { mutableStateOf<PlaceRepository.MapPlaces?>(null) }
    // 검색 · 측정 장소 고르기가 쓰는 지금 기준 목록
    val places = mapPlaces?.now
    // 지도 시간 선택에서 고른 때. null 이면 지금. 상세에 다녀와도 그대로 둔다
    var mapSlot by remember { mutableStateOf<TimeSlot?>(null) }
    // 지도로 올 때마다 새로 읽는다. 앱을 켜 둔 채 시간이 지나거나 batch 가 점수를 새로 써도 옛 값이 남지 않게.
    // 읽는 동안은 이전 목록을 그대로 보여주고, 실패하면 이전 목록을 둔 채 오래된 정보라고 알린다
    LaunchedEffect(screen == Screen.MAP) {
        if (screen != Screen.MAP) return@LaunchedEffect
        try {
            mapPlaces = PlaceRepository.loadAll()
            nowHour = LocalTime.now(ZoneId.of("Asia/Seoul")).hour
        } catch (e: CancellationException) {
            // 읽는 도중 지도를 떠난 것. 실패가 아니다
            throw e
        } catch (e: Exception) {
            Log.e("CalmMap", "장소 불러오기 실패: ${e.brief()}")
            val message = if (mapPlaces == null) "장소를 불러오지 못했어요" else "최신 정보를 불러오지 못했어요"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    // DB 의 방문 예정을 이번 로그인에서 읽었는지. 실패하면 false 로 남아 다음에 지도로 올 때 다시 읽는다
    var visitsLoaded by remember(isGuest) { mutableStateOf(false) }
    // 로그인한 상태로 지도에 오면 한 번 읽는다. 장소 정보(이름 · 그래프)를 붙여야 해서 장소를 읽은 뒤에
    LaunchedEffect(isGuest, places != null, screen == Screen.MAP) {
        if (isGuest || visitsLoaded || places == null || screen != Screen.MAP) return@LaunchedEffect
        try {
            val byId = places.associateBy { it.id }
            val loaded = VisitRepository.loadPlanned().mapNotNull { row ->
                // 지도에 없는 장소(지워졌거나 아직 못 읽은 곳)의 예정은 띠에 띄울 수 없어서 뺀다
                val place = byId[row.placeId] ?: return@mapNotNull null
                VisitPlan(place, visitDayOf(row.date), row.hour, row.id)
            }
            // 읽는 사이에 새로 정한 예정이 있으면 그것을 남긴다
            visitPlans.addAll(loaded.filter { new -> visitPlans.none { it.place.id == new.place.id } })
            visitPlans.sortWith(compareBy({ it.day.date }, { it.hour }))
            visitsLoaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("CalmMap", "방문 예정 불러오기 실패: ${e.brief()}")
        }
    }
    // 기록 탭 · 계정 관리에 들어올 때마다 새로 읽는다. 읽는 동안은 이전 목록을 그대로 둔다
    LaunchedEffect(isGuest, screen == Screen.HISTORY || screen == Screen.ACCOUNT) {
        if (isGuest || (screen != Screen.HISTORY && screen != Screen.ACCOUNT)) return@LaunchedEffect
        visitsFailed = false
        try {
            visits = VisitRepository.loadHistory()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("CalmMap", "방문 기록 불러오기 실패: ${e.brief()}")
            visitsFailed = true
        }
    }
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
            Log.e("CalmMap", "우리 아이 설정 불러오기 실패: ${e.brief()}")
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

    // 검색어. 고른 가게가 아니라서 돌아오면 치던 글자가 남아 있게 여기 둔다. 지도로 돌아가면 비운다
    var searchQuery by remember { mutableStateOf("") }
    var measureQuery by remember { mutableStateOf("") }
    LaunchedEffect(screen == Screen.MAP) {
        if (screen == Screen.MAP) {
            searchQuery = ""
            measureQuery = ""
        }
    }
    // 검색에서 고른 카카오 가게와, '아니에요' 를 누르면 돌아갈 곳 (검색 또는 측정 장소 고르기)
    var confirmPlace by remember { mutableStateOf<PlaceUi?>(null) }
    var confirmFrom by remember { mutableStateOf(Screen.SEARCH) }

    fun openConfirm(place: PlaceUi) {
        confirmFrom = screen
        confirmPlace = place
        screen = Screen.PLACE_CONFIRM
    }

    // 장소 상세에서는 장소가 정해져 있어서 바로 측정 중으로. [from] 은 측정을 끝내거나 그만두면 돌아갈 곳
    fun startMeasure(place: PlaceUi?, from: Screen = screen) {
        if (isGuest) {
            loginSheet = true
            return
        }
        // 시간 선택 중에는 측정하지 않는다. 상세는 그 장소를 연 때, 지도는 지금 고른 때를 본다
        if ((if (place != null) place.slot else mapSlot) != null) {
            Toast.makeText(context, "$MEASURE_NOW_ONLY. 위쪽 '지금'을 눌러 주세요", Toast.LENGTH_SHORT).show()
            return
        }
        measureFrom = from
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
            Screen.PLACE_CONFIRM -> confirmFrom
            Screen.VISIT_CHILD, Screen.VISIT_ONSITE -> Screen.VISIT_CARD
            else -> Screen.MAP
        }
    }

    // 화면 위에 로그인 안내 시트를 덮을 수 있게 Box 로 감싼다
    Box {
        when (screen) {
            Screen.MAP -> MapScreen(
                places = mapPlaces?.at(mapSlot).orEmpty(),
                slot = mapSlot,
                onSlotChange = { mapSlot = it },
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
                places = places.orEmpty(),
                onBack = { screen = Screen.MAP },
                onPlaceClick = { openDetail(it) },
                onMeasureClick = { openConfirm(it) },
                query = searchQuery,
                onQueryChange = { searchQuery = it },
            )

            Screen.HISTORY -> VisitHistoryScreen(
                onTabSelect = { selectTab(it) },
                visits = visits.orEmpty(),
                loading = visits == null && !visitsFailed,
                failed = visits == null && visitsFailed,
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
                            Log.e("CalmMap", "우리 아이 설정 저장 실패: ${e.brief()}")
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
                visitCount = visits?.size ?: 0,
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
                        // DB 에 들어간 뒤에야 지도로. 같은 장소의 예정이 있으면 새 줄 대신 시각만 바꾼다
                        onConfirm = { day, hour ->
                            val id = VisitRepository.plan(place.id, day.date, hour)
                            addPlan(VisitPlan(place, day, hour, id))
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
                // DB 에서 지운 뒤에 그 계획만 빼고 들어온 곳으로. 목록이 비면 지도로.
                // 못 지우면 카드에 남는다. 앱에서만 빼면 다시 켰을 때 취소한 예정이 돌아온다
                onCancelVisit = {
                    val plan = activePlan
                    if (plan != null) scope.launch {
                        try {
                            VisitRepository.cancel(plan.place.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w("CalmMap", "방문 취소 실패: ${e.brief()}")
                            Toast.makeText(context, "취소하지 못했어요. 인터넷 연결을 확인해 주세요", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        visitPlans.remove(plan)
                        activePlan = null
                        screen = if (cardFrom == Screen.VISIT_PLANS && visitPlans.isEmpty()) Screen.MAP else cardFrom
                    }
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
                        // DB 에 쓴 뒤에 기록 탭 맨 위에 새 기록을 넣는다. 기록 탭에 들어가면 DB 에서 다시 읽는다
                        onSubmit = { feeling, triggers ->
                            VisitRepository.record(plan.id ?: error("줄 번호 없는 방문 예정"), feeling, triggers)
                            visits = listOf(
                                VisitUi(
                                    placeName = plan.place.name,
                                    visitedAt = plan.recordLabel,
                                    feeling = feeling,
                                    trigger = triggers.joinToString(", ") { it.label }.ifEmpty { null },
                                    visitedOn = plan.day.date,
                                ),
                            ) + visits.orEmpty()
                            finishPlan(plan)
                        },
                        // 기록 없이 넘기면 예정을 지운다. 결과 없는 줄은 기록 탭 · 학습 어디에도 쓰이지 않는다
                        onSkip = {
                            VisitRepository.cancel(plan.place.id)
                            finishPlan(plan)
                        },
                    )
                }
            }

            Screen.MEASURE_PLACE -> MeasurePlaceScreen(
                places = places.orEmpty(),
                onBack = { screen = measureFrom },
                onPlaceClick = {
                    // 카카오 가게는 지도에서 맞는지 먼저 본다. 지도에 있는 장소는 바로 측정
                    if (it.kakao != null) {
                        openConfirm(it)
                    } else {
                        measurePlace = it
                        screen = Screen.MEASURE_RUN
                    }
                },
                query = measureQuery,
                onQueryChange = { measureQuery = it },
            )

            Screen.PLACE_CONFIRM -> {
                val place = confirmPlace
                if (place == null) {
                    screen = confirmFrom
                } else {
                    PlaceConfirmScreen(
                        place = place,
                        // 측정 장소 고르기에서 왔으면 측정을 그만둘 때 그 화면의 원래 규칙(지도에서 왔으면 장소 고르기)을 따른다
                        onMeasure = { startMeasure(place, from = if (confirmFrom == Screen.MEASURE_PLACE) measureFrom else confirmFrom) },
                        onBack = { screen = confirmFrom },
                    )
                }
            }

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
                        // DB 에 들어간 뒤에야 화면을 나간다. 실패하면 결과 화면이 알리고 그대로 남는다
                        onSave = { saved ->
                            MeasurementRepository.save(saved)
                            // 카카오 검색에서 고른 가게는 이번 저장으로 지도에 처음 생긴다. 지도로 가서 마커를 보여준다
                            val added = saved.place.kakao != null
                            Toast.makeText(
                                context,
                                if (added) "측정을 저장했어요. 지도에 장소가 생겼어요" else "측정을 저장했어요",
                                Toast.LENGTH_SHORT,
                            ).show()
                            measureResult = null
                            screen = if (added) Screen.MAP else measureFrom
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
                val noData = place?.let { sampleNoDataFor(it.id) }

                when {
                    place == null -> screen = Screen.MAP

                    // 그 날 점수가 한 칸도 없는 장소만 점수를 내지 않는 화면으로 보낸다.
                    // 고른 시각만 비어 있으면 상세를 연다. 그 날 다른 시각을 시간대별 탭에서 볼 수 있다
                    !place.hasForecast -> NoDataScreen(
                        place = place,
                        info = noData,
                        onBack = { screen = Screen.MAP },
                        onMeasureClick = { startMeasure(place) },
                        // 지도는 '지금' 그대로 두고 상세만 그 묶음 기록으로 연다
                        onOtherRecordClick = { slot ->
                            mapPlaces?.at(slot)?.firstOrNull { it.id == place.id }?.let { openDetail(it) }
                        },
                        detail = detail,
                        detailFailed = detailFailed,
                        onRetry = { detailRetry++ },
                    )

                    else -> PlaceDetailScreen(
                        place = place,
                        detail = detail,
                        detailFailed = detailFailed,
                        onRetry = { detailRetry++ },
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
