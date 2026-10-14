-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/12_답변서_반영.md 8번
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 15. commercial / commercial_categories — 상권 정보
--     먼저 필요: 001_congestion_areas
--
--     질의 답변서가 지목한 새 데이터다 (답변서2 질의2).
--     서울시가 신한카드 결제로 만든 "매장이 얼마나 바쁜가" 지표이고,
--     지금 수집 중인 인구 혼잡과는 다른 것이다.
--
--       인구 혼잡  통신사 기지국 신호 → 그 지역에 몇 명 있나
--       상권 정보  신한카드 결제      → 매장에서 얼마나 사나
--
--     답변서가 인구 혼잡의 한계를 짚는다.
--       "서울시 데이터는 통신사 기지국 신호로 추정한 '지역' 인구이며,
--        매장 실내 혼잡과 다를 수 있습니다."
--
--     홍대 관광특구에서 실제로 확인한 차이다.
--       지역 전체        "바쁜"
--         한식           분주한
--         제과/커피(카페)  보통
--         할인점/슈퍼(마트) 한산한
--
--     지역 혼잡 하나만 쓰면 마트에 "바쁨"을 붙이게 된다.


-- -----------------------------------------------------
-- 1. commercial — 지역 전체 상권
-- -----------------------------------------------------
--
-- 혼잡도(congestion)와 다른 점: 상권은 예측이 없다. 실측만 쌓인다.
-- 그래서 is_forecast 같은 칸이 없다.

create table commercial (
  id          bigint generated always as identity primary key,
  area_cd     text     not null references congestion_areas(area_cd) on delete cascade,
  base_at     timestamptz not null,
  level       smallint not null,
  level_text  text     not null,
  payment_cnt integer,
  payment_amt_min bigint,
  payment_amt_max bigint,

  constraint commercial_level_range
    check (level between 1 and 4),

  constraint commercial_payment_nonneg
    check (payment_cnt is null or payment_cnt >= 0),

  constraint commercial_amt_order
    check (payment_amt_min is null or payment_amt_max is null
           or payment_amt_max >= payment_amt_min),

  -- 서울시가 10분마다 갱신한다. 우리는 20분마다 받으므로 같은 시각을
  -- 두 번 받을 수 있다. 그때 쌓지 않고 덮어쓴다.
  constraint commercial_one_per_slot
    unique (area_cd, base_at)
);

create index commercial_area_base_idx on commercial (area_cd, base_at desc);

comment on table  commercial is
  '지역 상권 정보. 신한카드 결제 기반. 인구 혼잡과 다른 데이터다 (답변서2 질의2)';
comment on column commercial.base_at is
  '언제 기준인가 (API 의 CMRCL_TIME). 10분 단위로 갱신된다';
comment on column commercial.level is
  '1 한산한 / 2 보통 / 3 바쁜 / 4 분주한. 인구 혼잡의 단계와 이름이 다르다';
comment on column commercial.level_text is
  'API 원문. 단계 구분이 바뀌어도 다시 매길 수 있게 보관';
comment on column commercial.payment_cnt is
  '결제 건수. 단계만으로는 부족할 때 쓴다 (혼잡도에 인구수를 같이 저장한 것과 같은 이유)';

alter table commercial enable row level security;

-- 지도를 보려면 로그인 전에도 필요하다
create policy "commercial 은 누구나 읽기"
  on commercial for select using (true);

-- 쓰기 정책 없음. batch(Secret key) 만 쓴다.


-- -----------------------------------------------------
-- 2. commercial_categories — 업종별 상권
-- -----------------------------------------------------
--
-- 한 수집에 5~13줄이 딸린다. 이 표가 이번 작업의 핵심이다.
-- 지역 전체 단계만으로는 "마트는 한산한데 식당은 분주한" 을 구분할 수 없다.
--
-- measurements <-> measurement_labels 와 같은 구조로 나눈다.
-- 업종 수가 지역마다, 시각마다 달라서(결제가 없으면 아예 안 옴)
-- 칸을 고정으로 둘 수 없다.

create table commercial_categories (
  id            bigint generated always as identity primary key,
  commercial_id bigint not null references commercial(id) on delete cascade,
  lrg_ctgr      text   not null,
  mid_ctgr      text   not null,
  level         smallint not null,
  level_text    text   not null,
  payment_cnt   integer,

  constraint commercial_categories_level_range
    check (level between 1 and 4),

  constraint commercial_categories_payment_nonneg
    check (payment_cnt is null or payment_cnt >= 0),

  -- 같은 수집에 같은 업종이 두 번 들어오면 평균이 틀어진다
  constraint commercial_categories_no_duplicate
    unique (commercial_id, lrg_ctgr, mid_ctgr)
);

create index commercial_categories_commercial_idx
  on commercial_categories (commercial_id);
create index commercial_categories_mid_idx
  on commercial_categories (mid_ctgr);

comment on table  commercial_categories is
  '업종별 상권. 한 수집에 5~13줄. "마트는 한산한데 식당은 분주한"을 구분한다';
comment on column commercial_categories.lrg_ctgr is
  '대분류. 음식·음료 / 유통 / 패션·뷰티 / 여가·오락 / 의료';
comment on column commercial_categories.mid_ctgr is
  '중분류. 한식 / 제과·커피·패스트푸드 / 할인점·슈퍼마켓 / 미용서비스 …'
  ' places.category 와 이 값을 잇는 표는 settings.commercial_category_map 에 있다';
comment on column commercial_categories.level is
  '1 한산한 / 2 보통 / 3 바쁜 / 4 분주한';

alter table commercial_categories enable row level security;

create policy "commercial_categories 는 누구나 읽기"
  on commercial_categories for select using (true);

-- 쓰기 정책 없음. batch(Secret key) 만 쓴다.


-- -----------------------------------------------------
-- 3. settings — 업종 매핑표
-- -----------------------------------------------------
--
-- 우리 places.category 와 서울시 상권 중분류를 잇는 표.
--
-- 코드에 박지 않는 이유:
--   1) 서울시가 분류 이름을 바꿀 수 있다
--   2) "식당"에 기타요식을 넣을지 말지가 바뀔 수 있다
--   3) batch 와 앱이 둘 다 읽어야 해서 한 곳에 둬야 어긋나지 않는다
--
-- 병원이 빈 목록인 것은 홍대에서 '의료' 업종이 아예 오지 않았기 때문이다.
-- 카드 결제 집계에 안 잡히는 것으로 보인다. 병원은 인구 혼잡으로 대체한다.

insert into settings (key, value, description) values
  ('commercial_category_map',
   '{"마트": ["할인점/슈퍼마켓"],
     "카페": ["제과/커피/패스트푸드"],
     "식당": ["한식", "일식/중식/양식", "기타요식"],
     "미용실": ["미용서비스"],
     "병원": []}'::jsonb,
   'places.category → 서울시 상권 중분류. 병원은 상권 정보가 없어 비어 있다'),

  ('commercial_areas',
   '["POI007", "POI055", "POI073", "POI053"]'::jsonb,
   '상권을 수집할 지역. 홍대 관광특구 · 홍대입구역 · 연남동 · 합정역. '
   '82곳 전부 모으면 하루 70,000줄이라 권역만 모은다')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();
