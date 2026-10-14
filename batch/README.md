# batch — 예약 계산 (Python)

GitHub Actions(`.github/workflows/batch.yml`)로 정해진 간격마다 실행한다.

| 작업 | 파일 | 주기 | 내용 |
|---|---|---|---|
| 혼잡도 수집 | `collect_congestion.py` | 20분 | 서울시 실시간 인구데이터에서 현재 혼잡도 + 향후 12시간 예측 저장 |
| 상권 수집 | `collect_commercial.py` | 20분 | 서울시 상권 정보(결제 기반)를 지역·업종별로 저장 |
| 소리 묶음 · 점수 계산 | `calculate_score.py` | 10~15분 | 측정 라벨을 소리 종류 5개로 묶고 요소별 점수 계산 |
| 시간대별 예측 · 상태 판정 | `forecast.py` | 10~15분 | 오늘 1시간 단위 예측, 단계 / 추정 / 정보 부족 판정 |

계산 규칙은 `docs/04_구현_할일_데이터.md`, 테이블 구조는 `docs/08_DB_설계.md`.

## 처음 받았을 때

```bash
# 1. 라이브러리 설치
pip install -r batch/requirements.txt

# 2. 키 파일 만들기
cp batch/.env.example batch/.env      # Windows PowerShell: Copy-Item batch\.env.example batch\.env
#   .env 를 열어 SEOUL_API_KEY, SUPABASE_URL, SUPABASE_SERVICE_KEY 를 채운다
#   .env 는 git 에 올라가지 않는다. 키를 코드나 커밋에 넣지 않는다.
```

`SEOUL_API_KEY` 는 [서울 열린데이터광장](https://data.seoul.go.kr)에서 발급한다.
`SUPABASE_SERVICE_KEY` 는 Supabase 대시보드 Settings → API Keys 의 **Secret key** 다.
이 키는 접근 권한(RLS)을 통과하므로 앱이나 웹에는 절대 넣지 않는다.

## 실행

```bash
python batch/collect_congestion.py
```

저장소 루트에서 실행해도 되고 `batch/` 안에서 실행해도 된다.

출력 예시:

```
수집 대상 121곳 · 현재 congestion 0줄
진행 (. 성공 / X 실패)
........................................  40/121
........................................  80/121
........................................  120/121
.  121/121

성공 121곳 / 실패 0곳
보낸 줄 1,573개 · congestion 0 → 1,573줄 (+1,573)
```

121곳에 2~3분 걸린다. 호출 사이에 0.3초씩 쉬기 때문이다.

## 파일 구성

| 파일 | 하는 일 | 실행 |
|---|---|---|
| `common.py` | `.env` 읽기, Supabase 읽고 쓰기, `settings` 읽기. 다른 파일들이 함께 쓴다 | — |
| `collect_congestion.py` | 서울시 인구 혼잡 121곳 → `congestion` | **자동** 20분 |
| `collect_commercial.py` | 서울시 상권 4곳 → `commercial` · `commercial_categories` | **자동** 20분 |
| `calculate_score.py` | 아직 안 만듦 | — |
| `forecast.py` | 아직 안 만듦 | — |

### 혼잡도와 상권은 다른 데이터다

```
혼잡도  통신사 기지국 신호 → 그 지역에 몇 명 "있나"
상권    신한카드 결제      → 그 지역 매장에서 몇 번 "샀나"
```

길에 있는 사람과 가게에 들어간 사람은 다르다. 실제로 확인한 차이다.

```
홍대 관광특구 지역 전체   보통
  한식                  바쁜      <- 지역보다 높다
  할인점/슈퍼마켓          한산한    <- 지역보다 낮다
```

지역 값만 쓰면 식당은 낮게, 마트는 높게 안내하게 된다.
그래서 업종별 상권을 따로 모은다 (`docs/12_답변서_반영.md` 8번).

수집할 지역은 `settings.commercial_areas` 에 있어서 **코드를 고치지 않고 바꾼다.**
82곳 전부 모으면 하루 70,000줄이라 권역만 모은다.

## 알아둘 것

**Supabase 는 한 번에 1,000줄까지만 돌려준다.** 그보다 많이 읽어야 하면
`limit` 과 `offset` 으로 나눠 받아야 한다. 안 그러면 1,000줄까지만 계산하고
조용히 틀린 결과가 나온다.

**실측은 쌓이고 예측은 덮어쓴다.** `congestion` 에 `unique (area_cd, target_at,
is_forecast)` 가 있어서, 같은 지역·같은 대상 시각·같은 종류면 새 줄을 만들지
않고 덮어쓴다. 그래서 같은 스크립트를 여러 번 돌려도 예측이 중복되지 않는다.

**서울시 API 는 5분마다 갱신된다.** 1시간에 한 번 돌리면 지역당 실측 1줄이
쌓인다 (하루 121 × 24 = 2,904줄).

**서울시 API 는 지역을 이름으로 받는다.** `congestion_areas.area_nm` 을 그대로
주소에 넣는다. `area_cd` 는 우리 DB 안에서 테이블을 연결하는 용도다.

## 자동 실행 (GitHub Actions)

`.github/workflows/batch.yml` 의 `schedule` 이 주석 처리돼 있다. 켜기 전에:

1. 저장소 Settings → Secrets and variables → Actions 에 값 3개를 넣는다
   (`SEOUL_API_KEY`, `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`)
2. Actions 탭에서 `batch` → Run workflow 로 손으로 한 번 돌려 성공을 확인한다
3. 그 뒤에 `schedule` 주석을 지운다

손으로 확인하지 않고 예약을 켜면, 실패할 때 매시간 실패 알림이 온다.
