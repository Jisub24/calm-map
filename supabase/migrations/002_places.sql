-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 2. places — 지도에 마커로 찍히는 장소
--    이 테이블에 있는 곳만 지도에서 누를 수 있다
--    먼저 필요: 001_congestion_areas

create table places (
  id                 bigint generated always as identity primary key,
  name               text   not null,
  category           text   not null,
  address            text   not null,
  lat                double precision not null,
  lng                double precision not null,
  congestion_area_cd text   not null references congestion_areas(area_cd),
  source             text   not null default '직접입력',
  created_at         timestamptz not null default now(),

  constraint places_category_valid
    check (category in ('마트', '카페', '식당', '병원', '미용실')),

  constraint places_source_valid
    check (source in ('직접입력', '카카오수집', '제주수기지도')),

  constraint places_lat_range check (lat between 33 and 39),
  constraint places_lng_range check (lng between 124 and 132)
);

create index places_area_idx on places (congestion_area_cd);

comment on table  places is '지도에 마커로 찍히는 장소. 여기 있는 곳만 누를 수 있다';
comment on column places.id                 is '우리 DB 안에서만 쓰는 번호. 지도 서비스의 장소 ID 와 무관';
comment on column places.category           is '업종. 영업시간 기본값을 정하는 기준';
comment on column places.lat                is '위도. 지도에 마커를 찍는 위치';
comment on column places.lng                is '경도';
comment on column places.congestion_area_cd is '이 가게가 쓸 혼잡도 지역 (congestion_areas)';
comment on column places.source             is '출처. 직접입력 / 카카오수집 / 제주수기지도';

alter table places enable row level security;

create policy "places 는 누구나 읽기"
  on places
  for select
  using (true);
