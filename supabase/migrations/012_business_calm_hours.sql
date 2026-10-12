-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 12. business_calm_hours — 사업장이 등록한 무던한 시간
--     사업장 웹에서 가게 주인이 직접 넣고 고치고 지운다
--     먼저 필요: 002_places
--
--     이 테이블만 로그인 없이 쓰기를 허용한다.
--     사업장 웹에 회원가입을 만들지 않기로 했기 때문이다 (docs/02_MVP_범위).
--     그래서 점수 계산에는 넣지 않고 "가게가 알려준 정보"로 따로 보여준다.

-- 시간대가 겹치는지 보려면 '구간' 끼리 비교해야 한다.
--   btree_gist : exclude 제약에서 = 비교(place_id, weekday)를 쓰려면 필요
--   timerange  : PostgreSQL 에 time 용 범위 타입이 기본으로 없어서 직접 만든다
create extension if not exists btree_gist;
create type timerange as range (subtype = time);

create table business_calm_hours (
  id            bigint generated always as identity primary key,
  place_id      bigint   not null references places(id) on delete cascade,
  weekday       smallint not null,
  start_time    time     not null,
  end_time      time     not null,
  actions       text[],
  custom_action text,
  updated_at    timestamptz not null default now(),

  constraint business_calm_hours_weekday_range
    check (weekday between 1 and 7),

  constraint business_calm_hours_time_order
    check (start_time < end_time),

  -- 화면의 시간 선택이 30분 단위다. 10:15 같은 값이 들어오면
  -- 그래프의 30분 칸과 맞지 않아 어디에 그릴지 정할 수 없다.
  constraint business_calm_hours_half_hour
    check (
      extract(minute from start_time) in (0, 30) and
      extract(second from start_time) = 0 and
      extract(minute from end_time)   in (0, 30) and
      extract(second from end_time)   = 0
    ),

  -- 웹이 오타(music_stop 등)를 보내면 앱이 조용히 무시하게 된다
  constraint business_calm_hours_actions_valid
    check (
      actions is null
      or actions <@ array['music_off', 'light_down', 'announcement_min']::text[]
    ),

  -- 시간만 등록하고 조치를 비우면 앱에 보여줄 내용이 없다
  constraint business_calm_hours_something_done
    check (
      (actions is not null and array_length(actions, 1) > 0)
      or
      (custom_action is not null and length(trim(custom_action)) > 0)
    ),

  -- 같은 가게 · 같은 요일에 시간이 겹치는 줄을 막는다.
  -- 겹치면 앱이 같은 시각에 조치 두 개를 보여줘야 해서 무엇을 쓸지 정할 수 없다.
  -- unique 로는 막을 수 없다. 10:00~12:00 과 11:00~13:00 은 값이 달라서
  -- unique 는 통과시키지만 실제로는 11~12시가 겹친다.
  constraint business_calm_hours_no_overlap
    exclude using gist (
      place_id with =,
      weekday  with =,
      timerange(start_time, end_time) with &&
    )
);

create index business_calm_hours_place_idx on business_calm_hours (place_id, weekday);

comment on table  business_calm_hours is '사업장이 등록한 무던한 시간. 점수 계산에 넣지 않고 따로 보여준다';
comment on column business_calm_hours.weekday       is '1=월 … 7=일. place_hours 와 같은 규칙';
comment on column business_calm_hours.start_time    is '시작 시각. 30분 단위';
comment on column business_calm_hours.end_time      is '종료 시각. 시작보다 뒤여야 한다';
comment on column business_calm_hours.actions       is 'music_off 음악 끔 / light_down 조명 낮춤 / announcement_min 안내방송 최소';
comment on column business_calm_hours.custom_action is '목록에 없는 조치를 직접 적는 칸';
comment on column business_calm_hours.updated_at    is '마지막으로 고친 시각';

alter table business_calm_hours enable row level security;

-- 이 테이블만 로그인 없이 쓰기를 허용한다.
-- 사업장 웹에 회원가입이 없어서, 웹은 Publishable key 로만 접근한다.
-- 대신 이 테이블은 점수 계산에 쓰지 않으므로, 장난 입력이 들어와도
-- 예측값이 틀어지지는 않는다.
create policy "business_calm_hours 는 누구나 읽기"
  on business_calm_hours for select using (true);

create policy "business_calm_hours 는 누구나 등록"
  on business_calm_hours for insert with check (true);

create policy "business_calm_hours 는 누구나 수정"
  on business_calm_hours for update using (true) with check (true);

create policy "business_calm_hours 는 누구나 삭제"
  on business_calm_hours for delete using (true);
