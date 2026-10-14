-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/12_답변서_반영.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 13. 질의 답변서 반영 — 기존 테이블 수정
--
--     공모전 질의 답변서 2건을 받고 지수 계산 방식이 바뀌었다.
--     핵심은 세 가지다.
--       1) 상쇄하지 않는다 — 종합 지수는 하위 항목의 "최댓값"
--       2) 모르는 것은 0 이 아니라 "모름"
--       3) 근거가 엇갈리면 "무던함"으로 추천하지 않는다
--
--     가장 먼저 막아야 하는 것은 "부담을 낮게 잘못 안내하는 오류"다.
--
--     기존 데이터가 없어서(measurements · hourly_forecast 0줄) 자료 이전은 없다.


-- -----------------------------------------------------
-- 1. measurements — 빛과 돌발성 칸 추가
-- -----------------------------------------------------
--
-- 답변서의 하위 항목은 4개인데(혼잡 / 소리 크기 / 소리 종류·돌발성 / 밝기)
-- 그중 "밝기"를 담을 칸이 하나도 없었다.

alter table measurements
  add column avg_lux         real,
  add column lux_status      text not null default '미측정',
  add column flicker_status  text not null default '미측정',
  add column background_db   real,
  add column sudden_count    smallint not null default 0,
  add column sudden_max_rise real,
  add column announce_count  smallint not null default 0;

comment on column measurements.avg_lux is
  '평균 밝기(lux). 조도센서가 없는 기종은 비움';
comment on column measurements.lux_status is
  '미측정 / 측정유효 / 측정실패. 센서 없는 기종과 가려진 경우를 구분한다';
comment on column measurements.flicker_status is
  '조명 깜빡임. 기본 미측정. "미측정"과 "깜빡임 없음"은 다르다 (답변서 질의3)';
comment on column measurements.background_db is
  'LA90 - 측정 시간의 90%% 동안 넘는 소리 크기. 배경 소음 지표이자 돌발 판정 기준선';
comment on column measurements.sudden_count is
  '돌발 횟수. 배경(LA90) 대비 1초 안에 10dB 이상 오른 횟수';
comment on column measurements.sudden_max_rise is
  '돌발 중 가장 큰 상승폭(dB)';
comment on column measurements.announce_count is
  '안내방송 추정 횟수. 차임(Chime/Ding-dong) 직후의 말소리 또는 급상승한 말소리';

-- 상태 글자를 셋으로 묶는다. 오타가 들어오면 화면이 조용히 틀어진다.
alter table measurements
  add constraint measurements_lux_status_valid
    check (lux_status in ('미측정', '측정유효', '측정실패')),
  add constraint measurements_flicker_status_valid
    check (flicker_status in ('미측정', '측정유효', '측정실패'));

-- 밝기 값과 상태가 어긋나지 않게 한다.
-- 측정유효인데 값이 비어 있거나, 미측정인데 값이 있는 경우를 막는다.
alter table measurements
  add constraint measurements_lux_value_matches_status
    check (
      (lux_status = '측정유효' and avg_lux is not null and avg_lux >= 0)
      or
      (lux_status <> '측정유효' and avg_lux is null)
    );

-- 돌발이 있었다면 상승폭이 있어야 하고, 없었다면 상승폭도 없어야 한다
alter table measurements
  add constraint measurements_sudden_rise_matches_count
    check (
      (sudden_count = 0 and sudden_max_rise is null)
      or
      (sudden_count > 0 and sudden_max_rise is not null and sudden_max_rise > 0)
    );

alter table measurements
  add constraint measurements_announce_within_sudden
    check (announce_count <= sudden_count);

-- 측정 길이를 바꾼다.
--   15초 -> 180초(3분).
--   답변서는 "각 15분 이상"을 말하지만, 보호자가 15분을 들고 있기는 어렵다.
--   그래서 3분을 최소로 두고, 길이에 따라 반영 범위를 다르게 한다 (docs/12 9번).
--     3분 미만   저장만, 점수 미반영
--     3~10분     "부담 높음"만 판정 가능. "무던함" 불가
--     10분 이상  전부 반영
--
--   LA90(배경 소음)을 내려면 최소 1분이 필요해서 하한을 60초로 올린다.
alter table measurements alter column duration_sec set default 180;

alter table measurements drop constraint if exists measurements_duration_range;
alter table measurements
  add constraint measurements_duration_range
    check (duration_sec between 60 and 7200);

comment on column measurements.duration_sec is
  '실제 측정 길이(초). 60~7200. 길이에 따라 점수 반영 범위가 다르다 (docs/12)';


-- -----------------------------------------------------
-- 2. sound_groups — 소리 종류 5종 -> 4종
-- -----------------------------------------------------
--
-- 답변서는 소리를 4묶음으로 나눈다.
--   1 경보음      Alarm, Fire alarm, Smoke detector, Siren, Buzzer, Beep
--   2 사람의 큰 소리 Screaming, Shout, Yell, Children shouting, Baby cry
--   3 기계음      Hair dryer, Vacuum cleaner, Blender, Drill
--   4 지속 소음    Crowd, Hubbub, Music, Television
--
-- announcement 와 sudden 이 사라진다. 둘 다 YAMNet 라벨이 아니라
-- dB 변화로 잡기 때문이다 (background_db, sudden_count, announce_count).
--
-- 지금 0줄이라 값 이전이 필요 없다.

alter table sound_groups drop constraint if exists sound_groups_type_valid;
alter table sound_groups
  add constraint sound_groups_type_valid
    check (sound_type in ('alarm', 'human_loud', 'machine', 'continuous'));

comment on column sound_groups.sound_type is
  'alarm 경보음 / human_loud 사람의 큰 소리 / machine 기계음 / continuous 지속 소음. '
  '안내방송과 돌발성은 라벨이 아니라 dB 변화로 잡는다';


-- -----------------------------------------------------
-- 3. hourly_forecast — 점수 체계 교체
-- -----------------------------------------------------
--
-- 점수 6개를 섞던 방식에서, 하위 항목 4개의 "최댓값"으로 바꾼다.
--
--   혼잡 20 · 소리 크기 35 · 소리 종류 80 · 밝기 30
--     평균 방식 41 (보통)       <- 특정 자극이 상쇄되어 사라진다
--     최댓값    80 (부담 높음)  <- 답변서 방식
--
-- 자폐의 감각 특성은 "특정 자극"에 대한 반응이라, 평균을 내면
-- 아이마다 다른 민감 영역이 사라진다. 통합대기환경지수(CAI)도 같은 구조다.

-- 칸을 지우면 그 칸을 쓰던 check 제약도 PostgreSQL 이 함께 지운다.
-- hourly_forecast_scores_range 와 hourly_forecast_level_only_when_level 이
-- 여기서 자동으로 사라지므로, 아래에서 drop 할 때 if exists 를 붙인다.
alter table hourly_forecast
  drop column score_announcement,
  drop column score_speech,
  drop column score_music,
  drop column score_machine,
  drop column score_sudden;

alter table hourly_forecast
  rename column base_score to total_score;

alter table hourly_forecast
  add column score_loudness real,
  add column score_sound    real,
  add column score_light    real,
  add column main_factor    text,
  add column is_estimated   boolean not null default false;

comment on column hourly_forecast.score_crowd is
  '장소 혼잡 0~100. 주변 지역 혼잡에서 온다';
comment on column hourly_forecast.score_loudness is
  '소리 크기 0~100';
comment on column hourly_forecast.score_sound is
  '소리 종류·돌발성 0~100';
comment on column hourly_forecast.score_light is
  '밝기 0~100. 조도센서가 없으면 비움';
comment on column hourly_forecast.total_score is
  '종합 지수 = 하위 항목 4개의 최댓값. 평균이 아니다 (답변서 질의1)';
comment on column hourly_forecast.main_factor is
  '최댓값이 나온 항목. crowd / loudness / sound / light. 화면에 "주 요인"으로 표시';
comment on column hourly_forecast.is_estimated is
  '참이면 보간으로 채운 시간대. 그래프에 점선으로 그리고 "무던함"을 추천하지 않는다';

-- 단계 이름을 바꾼다. 제안서의 "붐빔"은 혼잡 외 자극도 포함하므로
-- "부담 높음"으로 바꾸라는 권고를 받았다 (답변서 질의1).
alter table hourly_forecast drop constraint if exists hourly_forecast_level_valid;
alter table hourly_forecast
  add constraint hourly_forecast_level_valid
    check (level is null or level in ('calm', 'normal', 'high'));

comment on column hourly_forecast.level is
  'calm 무던함(0~39) / normal 보통(40~69) / high 부담 높음(70~100). status=level 일 때만';

-- 점수 범위 제약을 새 칸에 맞춰 다시 건다
alter table hourly_forecast drop constraint if exists hourly_forecast_scores_range;
alter table hourly_forecast
  add constraint hourly_forecast_scores_range
    check (
      (score_crowd    is null or score_crowd    between 0 and 100) and
      (score_loudness is null or score_loudness between 0 and 100) and
      (score_sound    is null or score_sound    between 0 and 100) and
      (score_light    is null or score_light    between 0 and 100) and
      (total_score    is null or total_score    between 0 and 100)
    );

alter table hourly_forecast drop constraint if exists hourly_forecast_level_only_when_level;
alter table hourly_forecast
  add constraint hourly_forecast_level_only_when_level
    check (
      (status = 'level'  and level is not null and total_score is not null)
      or
      (status <> 'level' and level is null)
    );

alter table hourly_forecast
  add constraint hourly_forecast_main_factor_valid
    check (main_factor is null
           or main_factor in ('crowd', 'loudness', 'sound', 'light'));

-- 종합 지수가 있으면 주 요인도 있어야 한다.
-- 화면에 "부담 높음 · 주 요인: 안내방송"을 쓰려면 둘이 짝이어야 한다.
alter table hourly_forecast
  add constraint hourly_forecast_factor_with_score
    check ((total_score is null) = (main_factor is null));

-- 소리·빛이 하나도 없으면 종합 지수를 보류한다 (답변서 질의4).
-- 최댓값 방식에서 소리·빛이 비면 실제 최댓값을 알 수 없어서,
-- 혼잡만으로 지수를 내면 부담을 낮게 잘못 안내하게 된다.
alter table hourly_forecast
  add constraint hourly_forecast_total_needs_sound_or_light
    check (
      total_score is null
      or score_loudness is not null
      or score_sound is not null
      or score_light is not null
    );


-- -----------------------------------------------------
-- 4. settings — 측정 길이와 돌발 기준 추가
-- -----------------------------------------------------

insert into settings (key, value, description) values
  ('measure_duration_sec', '180'::jsonb,
   '기본 측정 길이(초). 3분'),

  ('measure_min_sec', '180'::jsonb,
   '점수에 반영하는 최소 측정 길이(초). 이보다 짧으면 저장만 하고 계산에 넣지 않는다'),

  ('measure_full_sec', '600'::jsonb,
   '"무던함" 판정이 가능한 최소 측정 길이(초). 10분. '
   '짧은 측정은 안내방송을 놓칠 수 있어 "부담 높음"만 판정한다'),

  ('sudden_rise_db', '10'::jsonb,
   '돌발 판정 기준. 배경(LA90) 대비 1초 안에 이만큼 오르면 돌발'),

  ('weekday_grouping', '"weekday_weekend"'::jsonb,
   '요일 묶는 방식. 평일/주말 2분류. 장소당 측정이 2회라 요일별로는 칸을 못 채운다')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();

-- level_thresholds 는 그대로 둔다.
-- 답변서의 0~39 무던함 / 40~69 보통 / 70~100 부담 높음 과 이미 일치한다.
--
-- lux_thresholds 는 아직 넣지 않는다.
-- 몇 lux 부터 부담인지 답변서에 없어서, 녹음 실험 때 조도를 같이 재서 정한다.
