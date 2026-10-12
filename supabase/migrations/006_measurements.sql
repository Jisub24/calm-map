-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 6. measurements — 15초 측정 1건
--    사용자가 앱에서 [측정 시작]을 누를 때마다 한 줄씩 생긴다
--    먼저 필요: 002_places
--
--    여기서부터 사용자 데이터다. RLS 로 "자기 것만" 을 강제한다.

create table measurements (
  id           bigint generated always as identity primary key,
  place_id     bigint not null references places(id) on delete cascade,
  user_id      uuid   not null references auth.users(id) on delete cascade,
  device_model text   not null,
  measured_at  timestamptz not null,
  duration_sec smallint    not null default 15,
  avg_db       real        not null,
  max_db       real,
  created_at   timestamptz not null default now(),

  constraint measurements_duration_range
    check (duration_sec between 5 and 120),

  constraint measurements_avg_db_range
    check (avg_db between 0 and 140),

  constraint measurements_max_db_range
    check (max_db is null or max_db between 0 and 140),

  -- 앱이 두 값을 바꿔 보내는 버그를 잡는다
  constraint measurements_max_ge_avg
    check (max_db is null or max_db >= avg_db),

  -- 폰 시계가 틀어졌거나 누가 꾸며 넣은 미래 측정을 막는다.
  -- 폰과 서버 시계가 조금 어긋나는 것은 정상이라 10분 여유를 둔다.
  constraint measurements_not_future
    check (measured_at <= now() + interval '10 minutes')
);

create index measurements_place_time_idx on measurements (place_id, measured_at);
create index measurements_user_idx       on measurements (user_id);

comment on table  measurements is '15초 측정 1건. 원본 소리와 음성 임베딩을 담는 칸은 없다';
comment on column measurements.user_id      is '누가 쟀나. RLS 가 "자기 측정만"을 강제하는 기준';
comment on column measurements.device_model is '기종. 폰마다 마이크가 달라 보정에 쓴다';
comment on column measurements.measured_at  is '측정 시각. "토요일 14시대"로 분류되는 기준';
comment on column measurements.duration_sec is '실제 측정 길이. 나중에 길이를 바꿔도 과거와 섞이지 않게 남긴다';
comment on column measurements.avg_db       is '측정 구간 평균 소리 크기';
comment on column measurements.max_db       is '측정 구간 중 가장 컸던 순간. 갑작스러운 큰 소리 판단용';

alter table measurements enable row level security;

-- 읽기: 로그인한 사람은 자기가 잰 것만
create policy "measurements 는 자기 것만 읽기"
  on measurements
  for select
  to authenticated
  using (user_id = auth.uid());

-- 넣기: 자기 이름으로만. 남의 계정에 꾸며 넣는 것을 막는다
create policy "measurements 는 자기 것만 넣기"
  on measurements
  for insert
  to authenticated
  with check (user_id = auth.uid());

-- update / delete 정책은 일부러 만들지 않는다.
-- 측정은 사실 기록이라 올린 뒤에 고치거나 지울 수 없어야 한다.
-- 잘못 쟀을 때는 보내기 전에 [다시 측정] 으로 버린다.
