-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 9. hourly_forecast — 장소 × 날짜 × 시각 예측
--    batch 가 모든 재료를 버무려 만든 최종 답. 앱이 읽는 건 거의 이 테이블뿐이다
--    한 줄이 그래프 막대 하나
--    먼저 필요: 002_places
--
--    점수 칸에 not null 을 붙이지 않았다.
--    측정이 없는 시간은 점수를 "지어내지 않고 비워둬야" 하기 때문이다.
--    score = 0 은 "소리가 없었다"이지 "모른다"가 아니다.

create table hourly_forecast (
  id                 bigint generated always as identity primary key,
  place_id           bigint   not null references places(id) on delete cascade,
  date               date     not null,
  hour               smallint not null,

  score_announcement real,
  score_speech       real,
  score_music        real,
  score_machine      real,
  score_sudden       real,
  score_crowd        real,
  base_score         real,

  status             text     not null,
  level              text,
  measurement_count  smallint not null default 0,
  last_measured_at   timestamptz,
  computed_at        timestamptz not null default now(),

  constraint hourly_forecast_hour_range
    check (hour between 0 and 23),

  constraint hourly_forecast_status_valid
    check (status in ('level', 'estimate', 'insufficient')),

  constraint hourly_forecast_level_valid
    check (level is null or level in ('calm', 'normal', 'crowded')),

  -- 상태와 단계의 짝을 맞춘다.
  -- "정보 부족인데 무던함" 이나 "단계인데 단계가 비어 있음" 을 막는다.
  constraint hourly_forecast_level_only_when_level
    check (
      (status = 'level'  and level is not null and base_score is not null)
      or
      (status <> 'level' and level is null)
    ),

  constraint hourly_forecast_scores_range
    check (
      (score_announcement is null or score_announcement between 0 and 100) and
      (score_speech       is null or score_speech       between 0 and 100) and
      (score_music        is null or score_music        between 0 and 100) and
      (score_machine      is null or score_machine      between 0 and 100) and
      (score_sudden       is null or score_sudden       between 0 and 100) and
      (score_crowd        is null or score_crowd        between 0 and 100) and
      (base_score         is null or base_score         between 0 and 100)
    ),

  constraint hourly_forecast_count_nonneg
    check (measurement_count >= 0),

  -- batch 가 주기적으로 다시 계산한다. on conflict 로 덮어쓴다.
  constraint hourly_forecast_one_per_slot
    unique (place_id, date, hour)
);

create index hourly_forecast_place_date_idx on hourly_forecast (place_id, date);

comment on table  hourly_forecast is '장소 × 날짜 × 시각 예측. 한 줄이 그래프 막대 하나';
comment on column hourly_forecast.score_crowd       is '혼잡 점수. congestion 에서 온다';
comment on column hourly_forecast.base_score        is '가중치 없는 기본 점수. 맞춤을 끈 상태';
comment on column hourly_forecast.status            is 'level 단계 / estimate 추정 / insufficient 정보 부족';
comment on column hourly_forecast.level             is 'calm 무던 / normal 보통 / crowded 붐빔. status=level 일 때만';
comment on column hourly_forecast.measurement_count is '근거가 된 측정 수. "이 시간대 측정 5회" 문구에 쓴다';
comment on column hourly_forecast.last_measured_at  is '"마지막 측정 10/12 15:40" 문구에 쓴다';
comment on column hourly_forecast.computed_at       is 'batch 가 계산한 시각';

alter table hourly_forecast enable row level security;

-- 지도를 보려면 로그인 전에도 예측이 필요하다
create policy "hourly_forecast 는 누구나 읽기"
  on hourly_forecast
  for select
  using (true);

-- 쓰기 정책 없음. batch(Secret key) 만 쓴다.
