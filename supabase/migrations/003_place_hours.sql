-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 3. place_hours — 장소별 요일 영업시간
--    그래프를 몇 시부터 몇 시까지 그릴지 정한다
--    먼저 필요: 002_places

create table place_hours (
  place_id   bigint   not null references places(id) on delete cascade,
  weekday    smallint not null,
  is_closed  boolean  not null default false,
  open_hour  smallint,
  close_hour smallint,

  primary key (place_id, weekday),

  constraint place_hours_weekday_range
    check (weekday between 1 and 7),

  constraint place_hours_open_range
    check (open_hour between 0 and 23),

  constraint place_hours_close_range
    check (close_hour between 1 and 24),

  constraint place_hours_open_before_close
    check (open_hour is null or close_hour is null or open_hour < close_hour),

  -- 휴무면 시간이 비어 있고, 영업이면 시간이 둘 다 있어야 한다
  constraint place_hours_closed_or_hours
    check (
      (is_closed = true  and open_hour is null     and close_hour is null)
      or
      (is_closed = false and open_hour is not null and close_hour is not null)
    )
);

comment on table  place_hours is '장소 하나당 7줄(월~일). 그래프의 가로 범위를 정한다';
comment on column place_hours.weekday    is '1=월 … 7=일. 언어마다 요일 번호가 달라 하나로 통일';
comment on column place_hours.is_closed  is '휴무일이면 참. "휴무"와 "입력 안 함"을 구분하려고 둔다';
comment on column place_hours.open_hour  is '여는 시각 0~23. 휴무면 비움';
comment on column place_hours.close_hour is '닫는 시각 1~24. 자정 넘기는 가게는 24로 자른다';

alter table place_hours enable row level security;

create policy "place_hours 는 누구나 읽기"
  on place_hours
  for select
  using (true);
