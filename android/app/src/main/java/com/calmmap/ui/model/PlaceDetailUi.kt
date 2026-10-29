package com.calmmap.ui.model

import com.calmmap.ui.theme.SensoryLevel

/**
 * 장소 상세(B4)에서만 쓰는 정보.
 *
 * 지도나 목록에는 필요 없어서 PlaceUi 와 나눠 둔다.
 * 지도에 장소 15곳을 띄울 때 소리 구성이나 점수 내역까지 들고 있을 이유가 없다.
 */
data class PlaceDetailUi(
    /** 보호자가 '매우 민감'으로 고른 자극이 관측됐을 때 띄우는 한 줄. 없으면 null */
    val warning: String? = null,
    /** 지금 시각의 점수 내역. 오늘 점수가 한 칸도 없으면 null */
    val score: ScoreBreakdown?,
    val sounds: List<SoundRow> = emptyList(),
    /** 평일 · 주말 상관없는 장소 고유 소리 — 안내방송 빈도 · 배경 음악 (답변서2 질의1). 정보 부족 화면 소리 탭 */
    val fixedSounds: List<SoundRow> = emptyList(),
    /** 소리 탭 제목 옆 "최근 측정 N회". 모르면 null */
    val measureCount: Int? = null,
    val light: LightInfo? = null,
    val calmHours: CalmHours? = null,
)

/** 종합 지수를 어떻게 냈는지 */
data class ScoreBreakdown(
    val hour: Int,
    val total: Int,
    /** 하위 항목 4개. 혼잡 · 소리 크기 · 소리 종류/돌발성 · 밝기 */
    val factors: List<ScoreFactor>,
    /** 가장 높은 항목의 이름. 왜 이 점수인지를 한 마디로 말해 준다 */
    val mainFactor: String,
    /** "평일 12시 칸 · 실측 4회 · 중앙값 80 (72~86)" */
    val basis: String,
    /** 주변 지역 혼잡 단계 (서울시 · 여유 / 보통 / 약간 붐빔 / 붐빔). 장소가 아니라 동네 단위 */
    val areaCrowd: String,
    /** 주변 상권 단계 (서울시 카드 결제 · 한산한 / 보통 / 분주한 / 바쁜). 같은 업종 가게들 기준 */
    val commercialCrowd: String,
    val areaName: String = "홍대입구역 일대",
    val commercialName: String = "홍대 카페 상권",
)

data class ScoreFactor(
    val label: String,
    /** 0~100. 그 항목을 낼 재료가 없으면 null ("–" 로 보인다) */
    val score: Int?,
    /** 최댓값이라 종합 지수가 된 항목 */
    val isMain: Boolean = false,
    /** 줄 아래 작은 설명. "측정한 분들 답 · 붐빔" */
    val note: String? = null,
)

/** 소리 묶음 한 줄 */
data class SoundRow(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
    /** "그중 안내방송 추정 · 3회" 처럼 아래에 붙는 설명 */
    val subLabel: String? = null,
    val subValue: String? = null,
)

data class LightInfo(
    /** 밝기 (lux) */
    val lux: Int,
    /** 밝기만 본 부담 단계. 배지로 보여준다 */
    val luxLevel: SensoryLevel,
    val flicker: String,
    /** 깜빡임을 실제로 쟀는지. 안 쟀으면 점선으로 그린다 */
    val flickerMeasured: Boolean,
    val note: String,
    /** 보호자가 남긴 제보. 점수에는 넣지 않는다 */
    val report: String? = null,
)

/** 사업장이 등록한 조용한 시간. 점수에는 넣지 않고 따로 보여준다 */
data class CalmHours(
    val businessHours: String?,
    val businessActions: String?,
)

// ─────────────────────────────────────────────────────────────
// 10/18 답사 전까지 쓰는 임시 데이터
// ─────────────────────────────────────────────────────────────

val sampleMartDetail = PlaceDetailUi(
    warning = "'매우'로 고른 갑작스러운 소리(안내방송 추정)가 관측된 곳이에요.",
    score = ScoreBreakdown(
        hour = 12,
        total = 80,
        factors = listOf(
            ScoreFactor("장소 혼잡", 20),
            ScoreFactor("소리 크기", 35),
            ScoreFactor("소리 종류 · 돌발성", 80, isMain = true),
            ScoreFactor("밝기", 30),
        ),
        mainFactor = "갑작스러운 소리",
        basis = "평일 12시 칸 · 실측 4회 · 중앙값 80 (72~86)",
        areaCrowd = "약간 붐빔",
        commercialCrowd = "보통",
    ),
    sounds = listOf(
        SoundRow(
            label = "갑작스러운 소리",
            value = "1시간에 4회",
            emphasized = true,
            subLabel = "그중 안내방송 추정",
            subValue = "3회",
        ),
        SoundRow("웅성임 · 음악", "계속 들림"),
        SoundRow("기계음", "가끔"),
        SoundRow("경보음 · 사람의 큰 소리", "관측 없음"),
    ),
    light = LightInfo(
        lux = 420,
        luxLevel = SensoryLevel.MID,
        flicker = "미측정",
        flickerMeasured = false,
        note = "미측정은 '깜빡임 없음'이 아니에요. 깜빡이는 조명을 '매우'로 고른 아이에게는 이 장소를 '무던함'으로 추천하지 않아요.",
        report = "\"형광등이 깜빡였어요\" · 보호자 제보 · 10/11 · 점수에는 넣지 않아요",
    ),
    calmHours = CalmHours(
        businessHours = "화 10~12시",
        businessActions = "음악 끔 · 조명 낮춤 · 방송 줄임",
    ),
)

val sampleLibraryDetail = PlaceDetailUi(
    score = ScoreBreakdown(
        hour = 15,
        total = 28,
        factors = listOf(
            ScoreFactor("장소 혼잡", 18),
            ScoreFactor("소리 크기", 28, isMain = true),
            ScoreFactor("소리 종류 · 돌발성", 15),
            ScoreFactor("밝기", 22),
        ),
        mainFactor = "소리 크기",
        basis = "평일 15시 칸 · 실측 5회 · 중앙값 28 (24~33)",
        areaCrowd = "보통",
        commercialCrowd = "보통",
    ),
    sounds = listOf(
        SoundRow("웅성임 · 음악", "가끔"),
        SoundRow("기계음", "가끔"),
        SoundRow("갑작스러운 소리", "관측 없음"),
        SoundRow("경보음 · 사람의 큰 소리", "관측 없음"),
    ),
    light = LightInfo(
        lux = 350,
        luxLevel = SensoryLevel.MID,
        flicker = "없음",
        flickerMeasured = true,
        note = "깜빡임을 측정해서 확인했어요.",
    ),
)

val sampleCafeDetail = PlaceDetailUi(
    warning = "'매우'로 고른 갑작스러운 소리가 관측된 곳이에요.",
    score = ScoreBreakdown(
        hour = 15,
        total = 76,
        factors = listOf(
            ScoreFactor("장소 혼잡", 58),
            ScoreFactor("소리 크기", 62),
            ScoreFactor("소리 종류 · 돌발성", 76, isMain = true),
            ScoreFactor("밝기", 41),
        ),
        mainFactor = "갑작스러운 소리",
        basis = "평일 15시 칸 · 실측 6회 · 중앙값 76 (70~84)",
        areaCrowd = "붐빔",
        commercialCrowd = "바쁜",
    ),
    sounds = listOf(
        SoundRow("갑작스러운 소리", "1시간에 6회", emphasized = true),
        SoundRow("웅성임 · 음악", "계속 들림"),
        SoundRow("기계음", "자주"),
        SoundRow("경보음 · 사람의 큰 소리", "관측 없음"),
    ),
    light = LightInfo(
        lux = 280,
        luxLevel = SensoryLevel.CALM,
        flicker = "미측정",
        flickerMeasured = false,
        note = "미측정은 '깜빡임 없음'이 아니에요.",
    ),
)

val sampleBakeryDetail = PlaceDetailUi(
    score = ScoreBreakdown(
        hour = 12,
        total = 60,
        factors = listOf(
            ScoreFactor("장소 혼잡", 45),
            ScoreFactor("소리 크기", 60, isMain = true),
            ScoreFactor("소리 종류 · 돌발성", 38),
            ScoreFactor("밝기", 33),
        ),
        mainFactor = "소리 크기",
        basis = "평일 12시 칸 · 실측 1회 · 다른 시간은 추정",
        areaCrowd = "보통",
        commercialCrowd = "보통",
    ),
    sounds = listOf(
        SoundRow("웅성임 · 음악", "계속 들림"),
        SoundRow("기계음", "가끔"),
        SoundRow("갑작스러운 소리", "관측 없음"),
        SoundRow("경보음 · 사람의 큰 소리", "관측 없음"),
    ),
)

/** 장소 번호로 상세 정보를 찾는다. 없으면 null */
fun sampleDetailFor(placeId: Long): PlaceDetailUi? = when (placeId) {
    1L -> sampleLibraryDetail
    2L -> sampleMartDetail
    3L -> sampleCafeDetail
    4L -> sampleBakeryDetail
    else -> null
}
