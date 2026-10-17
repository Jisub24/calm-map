package com.calmmap.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 와이어프레임의 무채색을 그대로 옮긴 값이다.
 *
 * 단계를 색으로 나누지 않는 이유는 두 가지다.
 *   1) 색각 이상이면 빨강과 초록이 같은 밝기로 보여 구분이 안 된다
 *   2) 빨강·초록은 "가면 안 된다 / 가도 된다" 는 판정으로 읽힌다.
 *      우리는 권고만 한다
 *
 * 그래서 진하기와 글자로만 구분한다.
 * 최종 색은 아직 정하지 않았다. 정해지면 이 파일만 고치면 된다.
 */

// 글자
val Ink = Color(0xFF222222)        // 본문
val InkSoft = Color(0xFF4A4A4A)    // 설명
val InkMuted = Color(0xFF6B6B6B)   // 보조 · 단위 · 출처

// 바탕
val Paper = Color(0xFFFFFFFF)      // 화면
val PaperBox = Color(0xFFF0F0F0)   // 권고처럼 눈에 띄어야 하는 상자
val PaperSoft = Color(0xFFF7F7F7)  // 그 외 상자

// 선 · 버튼
val LineStrong = Color(0xFF333333) // 진한 버튼 바탕, 선택된 칩
val LineMid = Color(0xFF8A8A8A)    // 마커 테두리
val Line = Color(0xFFBDBDBD)       // 보통 테두리
val LineSoft = Color(0xFFD9D9D9)   // 옅은 테두리
val LineFaint = Color(0xFFEEEEEE)  // 목록 구분선

/**
 * 감각부하 단계. 진하기 3단계로만 구분한다.
 *
 * '후보'와 '정보 부족'은 채우지 않고 테두리로만 그린다.
 * 확인된 값이 아니라는 것을 채움 없음으로 보여주기 위해서다.
 */
val LevelCalm = Color(0xFFE6E6E6)  // 무던함 — 연함
val LevelMid = Color(0xFFA8A8A8)   // 보통 — 중간
val LevelHigh = Color(0xFF4A4A4A)  // 부담 높음 — 진함
