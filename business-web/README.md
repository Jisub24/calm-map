# business-web — 사업장 등록 웹

가게가 영업시간과 '무던한 시간대'를 등록하면 보호자 앱 지도 · 장소 카드 · 그래프에 바로 반영된다.
로그인 없이 가게를 찾아 바로 등록하는 시연용 (`docs/03_화면_구성.md` 8번).

주소: https://calm-map-business-web.vercel.app (가게 바로가기: `?place=장소번호`)

## 화면

1. 내 가게 찾기 — 무던한 지도에 있는 가게 + 카카오 장소 검색. 고르면 지도로 "이 가게가 맞나요?"
   - 이 브라우저에서 연 가게는 '내 가게' 목록으로 기억한다. 가게가 여러 개면 [가게 등록] 이나 왼쪽 위 로고 → 목록에서 바로 고른다
   - 지도에 없는 가게면 확인할 때 장소를 새로 넣는다 (`add_business_place`, 029)
2. 영업시간 — 요일별 여는 · 닫는 시각, 쉬는 날, 자정 넘김. 7일을 한 번에 저장 (`set_place_hours`, 029)
   - 앱 그래프에서 영업하지 않는 시각은 '영업 안 함' 으로 그려지고, batch 는 영업 중인 시각만 점수 칸을 만든다
3. 무던한 시간 — 요일 여러 개 · 30분 단위 · 하는 일(음악 끔 · 조명 낮춤 · 방송 최소 + 직접 입력). 등록 · 수정 · 삭제
   - 끝 시각, 같은 요일 겹침, 영업시간 밖이면 저장 전에 알림
   - 오른쪽에 "보호자 앱에서 이렇게 보여요" 미리보기 (지도 마커 · 장소 카드 · 상세 '사업장' 탭과 같은 글자)

## 파일

| 파일 | 내용 |
|---|---|
| `index.html` | 화면 구조 |
| `style.css` | 앱과 같은 색 · 글꼴(Noto Sans KR) · 모서리 |
| `app.js` | 검색 · 저장 · 미리보기 |
| `config.example.js` | 키 자리. 복사해서 `config.js` 로 (git 에 안 올라감) |
| `vercel.json` · `vercel-build.js` | Vercel 에 올릴 때 화면 파일을 `dist/` 로 모으고 `config.js` 를 만든다 |

## 내 PC 에서 열기

```bash
cp business-web/config.example.js business-web/config.js   # 값 채우기
cd business-web && python -m http.server 5500
# 브라우저에서 http://localhost:5500
```

카카오 검색 · 지도는 카카오 개발자 콘솔 → 플랫폼 키 → JavaScript 키 → JavaScript SDK 도메인에 `http://localhost:5500` 이 등록돼 있어야 뜬다.
같은 앱의 제품 설정 → 카카오맵 → 사용 설정이 ON 이어야 한다.
키는 대표 키(Default JS Key)가 아니라 사업장 웹용으로 따로 만든 JavaScript 키다. 도메인은 그 키에만 등록돼 있다.
JavaScript 키가 없으면 무던한 지도에 이미 있는 가게만 검색된다.

## 올리기

Vercel 프로젝트 `calm-map-business` (Root Directory `business-web`) 가 `main` 에 머지될 때마다 올린다.
올릴 때 `vercel-build.js` 가 화면 파일을 `dist/` 로 복사하고, Vercel 환경 변수(`SUPABASE_URL` · `SUPABASE_ANON_KEY` · `KAKAO_JS_KEY`)로 `config.js` 를 만든다.
Supabase 값이 비었거나 service key 가 들어 있으면 배포를 멈춘다.

PR 브랜치는 미리보기 주소로 올라간다. 미리보기 주소는 카카오에 등록돼 있지 않아서 카카오 검색 · 지도는 안 뜬다.

## 권한

웹은 Supabase anon key 로만 붙는다. service key 는 넣지 않는다.

| 하는 일 | 방법 |
|---|---|
| 장소 · 영업시간 · 무던한 시간 읽기 | 표 읽기 정책 (누구나) |
| 무던한 시간 등록 · 수정 · 삭제 | `business_calm_hours` 쓰기 정책 (012, 누구나) |
| 장소 추가 · 영업시간 저장 | DB 함수만 (029). 값을 확인하고 넣는다 |

사장 인증이 없어서 누구나 아무 가게의 시간을 바꿀 수 있다. 시연용이고, 사업장 인증을 붙이면 029 함수 권한을 거둔다.
