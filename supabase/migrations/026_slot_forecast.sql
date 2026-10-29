-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 26. slot_forecast — 장소 × 평일/주말 × 시각
--     먼저 필요: 002_places, 013(점수 4항목), 019(area_crowd_level)
--
--     지도 '시간 선택' 이 읽는 표. hourly_forecast 는 날짜별이라
--     '주말 15시' 를 고르면 특정 토요일 하나의 값을 보여주게 된다.
--     이 표는 날짜 없이, 지금까지 쌓인 기록으로 낸 칸 값이다 (답변서1 질의2: 장소 × 평일/주말 × 1시간).
--
--     점수     측정한 칸만. hourly_forecast 와 같은 판정 (3건부터 단계, 1~2건 후보, 부담 높음은 추정이어도 표시)
--              빈 칸은 채우지 않는다. 답변서는 '그날의 공공 혼잡 예측' 으로 채우라고 했는데 이 표에는 그날이 없다
--     주변 혼잡 congestion 실측(is_forecast = false)을 그 묶음 날들에서 같은 시각끼리 모은 중앙값.
--              하루에 한 값(같은 시각 두 값이면 더 붐빈 쪽), 두 단계 사이면 올림. 점수에는 넣지 않는다
--
--     batch/forecast.py 가 20분마다 다시 계산해 덮어쓴다.

create table slot_forecast (
  place_id           bigint   not null references places(id) on delete cascade,
  day_group          text     not null,
  hour               smallint not null,

  score_crowd        real,
  score_loudness     real,
  score_sound        real,
  score_light        real,
  total_score        real,
  main_factor        text,

  status             text     not null,
  level              text,
  is_estimated       boolean  not null default false,
  measurement_count  smallint not null default 0,
  last_measured_at   timestamptz,

  area_crowd_level   smallint,
  area_days          smallint not null default 0,
  computed_at        timestamptz not null default now(),

  primary key (place_id, day_group, hour),

  constraint slot_forecast_day_group_valid
    check (day_group in ('weekday', 'weekend')),

  constraint slot_forecast_hour_range
    check (hour between 0 and 23),

  constraint slot_forecast_status_valid
    check (status in ('level', 'estimate', 'insufficient')),

  constraint slot_forecast_level_valid
    check (level is null or level in ('calm', 'normal', 'high')),

  -- hourly_forecast 와 같은 짝 맞추기. "정보 부족인데 무던함" 을 막는다
  constraint slot_forecast_level_only_when_level
    check (
      (status = 'level'  and level is not null and total_score is not null)
      or
      (status <> 'level' and level is null)
    ),

  constraint slot_forecast_scores_range
    check (
      (score_crowd    is null or score_crowd    between 0 and 100) and
      (score_loudness is null or score_loudness between 0 and 100) and
      (score_sound    is null or score_sound    between 0 and 100) and
      (score_light    is null or score_light    between 0 and 100) and
      (total_score    is null or total_score    between 0 and 100)
    ),

  constraint slot_forecast_main_factor_valid
    check (main_factor is null or main_factor in ('crowd', 'loudness', 'sound', 'light')),

  constraint slot_forecast_factor_with_score
    check ((total_score is null) = (main_factor is null)),

  constraint slot_forecast_area_crowd_range
    check (area_crowd_level is null or area_crowd_level between 1 and 4),

  constraint slot_forecast_counts_nonneg
    check (measurement_count >= 0 and area_days >= 0)
);

comment on table  slot_forecast is '장소 × 평일/주말 × 시각. 지도 시간 선택용. 날짜 없이 쌓인 기록으로 낸 칸 값';
comment on column slot_forecast.day_group        is 'weekday 월~금 / weekend 토·일';
comment on column slot_forecast.is_estimated     is '측정 1~2건이라 확정 전인 칸. 보간한 칸은 이 표에 없다';
comment on column slot_forecast.area_crowd_level is '주변 지역 혼잡 1~4. 지난 실측의 시각별 중앙값(올림). 점수에 넣지 않는다';
comment on column slot_forecast.area_days        is 'area_crowd_level 을 낸 날 수. 화면에 "지난 주말 N일 기록" 으로 보인다';

alter table slot_forecast enable row level security;

-- 지도를 보려면 로그인 전에도 필요하다
create policy "slot_forecast 는 누구나 읽기"
  on slot_forecast
  for select
  using (true);

-- 쓰기 정책 없음. batch(Secret key) 만 쓴다.
