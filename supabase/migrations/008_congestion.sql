-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 8. congestion — 지역별 혼잡도 (실측 · 예측)
--    batch 가 1시간마다 서울시 API 에서 받아 쌓는다
--    한 번 호출하면 실측 1줄 + 예측 12줄이 들어온다
--    먼저 필요: 001_congestion_areas
--
--    예측은 쌓지 않고 덮어쓴다. unique 제약이 있어야
--    batch 가 on conflict ... do update 를 쓸 수 있다.

create table congestion (
  id          bigint generated always as identity primary key,
  area_cd     text   not null references congestion_areas(area_cd) on delete cascade,
  base_at     timestamptz not null,
  target_at   timestamptz not null,
  level       smallint    not null,
  level_text  text        not null,
  ppltn_min   integer,
  ppltn_max   integer,
  is_forecast boolean     not null,

  constraint congestion_level_range
    check (level between 1 and 4),

  constraint congestion_ppltn_nonneg
    check (ppltn_min is null or ppltn_min >= 0),

  constraint congestion_ppltn_order
    check (ppltn_min is null or ppltn_max is null or ppltn_max >= ppltn_min),

  -- 예측인데 과거 시각을 가리키는 줄을 막는다.
  -- 실측은 base_at = target_at 이라 검사를 건너뛴다.
  constraint congestion_forecast_is_later
    check (is_forecast = false or target_at > base_at),

  -- 같은 지역 · 같은 대상 시각 · 같은 종류는 한 줄만.
  -- batch 는 on conflict 로 덮어쓴다.
  constraint congestion_one_per_slot
    unique (area_cd, target_at, is_forecast)
);

create index congestion_area_target_idx on congestion (area_cd, target_at);

comment on table  congestion is '지역별 혼잡도. 한 번 호출하면 실측 1줄 + 예측 12줄이 생긴다';
comment on column congestion.base_at     is '언제 받았나 (API 의 PPLTN_TIME)';
comment on column congestion.target_at   is '어느 시각에 대한 값인가';
comment on column congestion.level       is '1 여유 / 2 보통 / 3 약간 붐빔 / 4 붐빔';
comment on column congestion.level_text  is 'API 원문. 단계 구분이 바뀌어도 다시 매길 수 있게 보관';
comment on column congestion.ppltn_min   is '추정 인구 하한(명). 혼잡도가 상대값이라 절대 인구도 저장한다';
comment on column congestion.ppltn_max   is '추정 인구 상한(명)';
comment on column congestion.is_forecast is '예측이면 참, 실측이면 거짓';

alter table congestion enable row level security;

-- 지도를 보려면 로그인 전에도 혼잡도가 필요하다
create policy "congestion 은 누구나 읽기"
  on congestion
  for select
  using (true);

-- 쓰기 정책 없음. batch(Secret key) 만 쓴다.
