package com.calmmap.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 피그마 「★ 무던한 지도 · 최종안」 Foundations 의 컬러 토큰 (J안 팔레트).
 * 이름은 피그마 변수 이름을 그대로 옮겼다. brand/primary → BrandPrimary
 *
 * 단계(Level)를 빨강 · 초록으로 나누지 않는 이유는 두 가지다.
 *   1) 색각 이상이면 빨강과 초록이 같은 밝기로 보여 구분이 안 된다
 *   2) 빨강 · 초록은 "가면 안 된다 / 가도 된다" 는 판정으로 읽힌다. 우리는 권고만 한다
 * 그래서 한 색 계열의 연함 → 진함 + 글자 + 기호(○◐●)로 구분한다.
 *
 * 색을 바꿀 때는 이 파일만 고친다. 화면에서 Color(0x...) 를 직접 쓰지 않는다.
 */

// Brand — 주된 버튼, 선택된 탭, 강조
val BrandPrimary = Color(0xFF3A6EA5)
val BrandPrimaryPressed = Color(0xFF2F5A88)
val BrandPrimarySoft = Color(0xFFE6EEF7)   // 권고 상자, 로고 바탕
val BrandOnPrimary = Color(0xFFFFFFFF)

// Text
val TextPrimary = Color(0xFF1A1D24)        // 본문 · 제목 · 선택된 칩 바탕
val TextSecondary = Color(0xFF4B5261)      // 설명
val TextTertiary = Color(0xFF6E7685)       // 보조 · 단위 · 출처 · 꺼진 탭
val TextDisabled = Color(0xFFB8BEC8)

// Surface
val BgCanvas = Color(0xFFF3F4F6)           // 회색 바탕 화면
val BgSurface = Color(0xFFFFFFFF)          // 흰 바탕 · 카드
val BgSubtle = Color(0xFFF3F4F6)           // 상자 · 아이콘 바탕
val BgSunken = Color(0xFFEDEFF2)           // 세그먼트 바탕 같은 들어간 면
val BorderDefault = Color(0xFFE6E8EC)      // 구분선 · 카드 테두리
val BorderStrong = Color(0xFFC3C9D2)       // 보조 버튼 · 칩 · 입력 테두리

// Level — 감각부하 단계. 진하기 3단계 + 테두리 2종
val LevelCalm = Color(0xFFDCE4EE)          // 무던함 — 연함
val LevelCalmEdge = Color(0xFF7F8EA6)      // 무던함 마커 점의 테두리 (연한 바탕에서 점이 묻히지 않게)
val LevelMid = Color(0xFF9AAAC0)           // 보통 — 중간
val LevelHigh = Color(0xFF2E3B52)          // 부담 높음 — 진함
val LevelOnHigh = Color(0xFFFFFFFF)
val LevelOutline = Color(0xFF6E7685)       // 후보 — 실선 테두리
val LevelNoData = Color(0xFFB8BEC8)        // 정보 부족 — 점선 테두리

// Status — '매우 민감' 자극이 관측된 곳 같은 주의
val StatusCaution = Color(0xFF8A5300)
val StatusCautionSoft = Color(0xFFFFF6DB)

// Kakao — 카카오 로그인 버튼 공식 색
val KakaoBg = Color(0xFFFEE500)
val KakaoFg = Color(0xFF191919)

// Map — 지도 화면은 네이버 기본 지도를 그대로 쓴다. 이 색은 지도 밖 일러스트(온보딩 · 위치 권한)용
val MapLand = Color(0xFFEEF0EC)
val MapPark = Color(0xFFDDE8D8)
val MapWater = Color(0xFFD9E3EE)
