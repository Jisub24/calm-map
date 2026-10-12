-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 1. congestion_areas — 서울시 혼잡도 지역 121곳
--    batch 가 "어느 지역에 API 를 호출할지" 를 여기서 읽는다

create table congestion_areas (
  area_cd                text     primary key,
  area_nm                text     not null,
  eng_nm                 text,
  category               text     not null,
  collect_interval_hours smallint not null default 1,

  constraint congestion_areas_category_valid
    check (category in ('관광특구', '발달상권', '인구밀집지역', '공원', '고궁·문화유산')),

  constraint congestion_areas_interval_range
    check (collect_interval_hours between 0 and 24)
);

comment on table  congestion_areas is '서울시 실시간 인구데이터가 다루는 주요 장소 121곳';
comment on column congestion_areas.area_cd                is '지역 코드. API 호출에 그대로 쓴다 (예: POI007)';
comment on column congestion_areas.area_nm                is '지역명 (예: 홍대 관광특구). 화면에 "OO 기준"으로 표시';
comment on column congestion_areas.eng_nm                 is '영문명. 배포 엑셀에 있어 같이 보관';
comment on column congestion_areas.category               is '서울시 구분. 혼잡도 산출 방식이 구분마다 다르다';
comment on column congestion_areas.collect_interval_hours is '몇 시간마다 수집할지. 0이면 수집 안 함';

alter table congestion_areas enable row level security;

create policy "congestion_areas 는 누구나 읽기"
  on congestion_areas
  for select
  using (true);
