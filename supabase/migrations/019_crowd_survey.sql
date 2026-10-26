-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 19. 장소 혼잡 설문, 강한 돌발 횟수, 지역 혼잡 표시 칸, 소리 눈금 조정
--     먼저 필요: 018
--
--     10/18 답사 7건을 점수로 바꿔 느낌과 비교한 결과 (10/24):
--       - 사람이 많아 부담 높았던 카페가 "보통"으로 나왔다. 혼잡이 점수에 없고
--         소리 눈금이 느슨했다
--       - 컵 내려놓는 소리 3번(배경 +7dB)만으로 부담 높음이 나왔다
--
--     답변서 질의 1 의 하위 항목 "장소 혼잡" 은 서울시 지역 데이터로는 채울 수 없어
--     (가게가 아니라 동네 단위) 비워 두었는데, 측정자 설문으로 채운다.


-- -----------------------------------------------------
-- 1. measurements — 장소 혼잡 설문, 강한 돌발 횟수
-- -----------------------------------------------------
--
-- 측정 끝에 묻는다. "사람이 얼마나 있었나요?"
--   빈자리가 많았어요 0 / 반쯤 찼어요 1 / 거의 다 찼어요 2 / 잘 모르겠어요 비움
-- "한산 · 보통 · 붐빔" 대신 눈에 보이는 기준으로 묻는다. 사람마다 "보통" 이 다르다.

alter table measurements
  add column crowd_level smallint,
  add column sudden_strong_count smallint not null default 0;

comment on column measurements.crowd_level is
  '장소 혼잡 설문 답. 0 빈자리 많음 / 1 반쯤 참 / 2 거의 다 참 / 비어 있으면 잘 모르겠어요. 종합 점수의 "장소 혼잡" 항목';
comment on column measurements.sudden_strong_count is
  '돌발 중 배경(LA90)보다 10dB 이상 튄 횟수. 10dB 미만 돌발(컵 소리 등)은 0.5번으로 센다';

alter table measurements
  add constraint measurements_crowd_level_range
    check (crowd_level is null or crowd_level between 0 and 2),
  add constraint measurements_strong_within_sudden
    check (sudden_strong_count <= sudden_count);


-- -----------------------------------------------------
-- 2. hourly_forecast — score_crowd 는 장소 혼잡, 지역 혼잡은 따로
-- -----------------------------------------------------
--
-- score_crowd 는 013 에서 "주변 지역 혼잡에서 온다" 고 적었다. 이제 측정자 설문에서 온다.
-- 지역 혼잡(서울시)은 점수에 넣지 않고 "주변 지역 혼잡" 띠로만 보여주므로 칸을 따로 둔다.
-- 한 칸에 섞으면 나중에 설문인지 서울시인지 알 수 없다.

comment on column hourly_forecast.score_crowd is
  '장소 혼잡 0~100. 측정자 설문(crowd_level)에서 온다. 종합 최댓값에 들어간다';

alter table hourly_forecast
  add column area_crowd_level smallint;

comment on column hourly_forecast.area_crowd_level is
  '주변 지역 혼잡 1~4 (여유 · 보통 · 약간 붐빔 · 붐빔). 서울시 지역 데이터. 점수에 넣지 않고 그래프 아래 띠로만 보여준다';

alter table hourly_forecast
  add constraint hourly_forecast_area_crowd_range
    check (area_crowd_level is null or area_crowd_level between 1 and 4);


-- -----------------------------------------------------
-- 3. settings — 장소 혼잡 점수, 소리 눈금 조정
-- -----------------------------------------------------

insert into settings (key, value, description) values
  ('score_crowd_level', '[25, 60, 90]'::jsonb,
   '장소 혼잡 설문 답 0 · 1 · 2 → 25 · 60 · 90 점. 임시값 [10/24]')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();

-- 소리 크기 눈금. 80dB → 73dB.
-- 10/18 답사에서 부담 높았던 곳(Leq 66.3 이상)과 아니었던 곳(65.7 이하)의 경계가
-- 66dB 라서, 부담 높음 시작(70점)이 66dB 에 오도록 끝을 73 으로 당겼다.
-- 7건에 맞춘 값이라 새 녹음과 갤럭시 보정 뒤 다시 본다. 무던한 곳 녹음이 없어 0점 쪽은 근거가 없다.
update settings
   set value       = '{"db_zero": 50, "db_full": 73}'::jsonb,
       description = '소리 크기 → 점수. Leq(에너지 평균) 50dB 가 0점, 73dB 가 100점, 사이는 직선. 10/24 답사 느낌에 맞춘 임시값',
       updated_at  = now()
 where key = 'score_sound_level';
