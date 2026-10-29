-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 27. 카카오 장소 검색으로 고른 가게를 앱에서 places 에 넣는다
--
--     지금까지는 places 에 넣는 사람이 우리뿐이라 홍대 5곳만 있었다.
--     앱 검색에서 카카오 장소를 골라 측정하면, 저장할 때 그 가게를 places 에 새로 넣는다.
--
--     새 장소에 필요한 것
--       지역 코드    가게 좌표에서 가장 가까운 서울시 혼잡 지역 (1km 안). 그래서 지역 표에 중심 좌표를 둔다
--       업종        카카오 업종을 우리 업종으로 바꾼다. 맞는 게 없으면 '기타'
--       중복 방지    같은 가게를 두 번 넣지 않게 카카오 장소 id 를 둔다
--       영업시간     카카오가 주지 않는다. place_hours 를 비워 두고 batch 가 8~22시로 그린다
--
--     앱은 places 에 바로 쓰지 못한다. 아래 add_kakao_place 함수로만 넣는다.
--     값 검사와 지역 배정을 DB 한 곳에서 하려는 것


-- -----------------------------------------------------
-- 1. congestion_areas — 중심 좌표
-- -----------------------------------------------------
--
-- 서울시가 지역 경계를 주지 않아 지역 이름을 카카오 키워드 검색한 첫 결과 좌표를 쓴다.
-- 엉뚱한 곳이 나온 19곳(예: '강남 MICE 관광특구' → 다른 회사, '시의회 앞' → 고양시청)은
-- 지역을 대표하는 곳 이름으로 다시 찾았다 (코엑스, 서울특별시의회 등).
-- 기존 5곳을 이 좌표로 가장 가까운 지역에 배정해 보면 지금 값(POI055 4곳, POI007 1곳)과 같다.

alter table congestion_areas
  add column if not exists lat double precision,
  add column if not exists lng double precision;

update congestion_areas a
   set lat = v.lat, lng = v.lng
  from (values
  ('POI001', 37.511824, 127.059159),
  ('POI002', 37.569015, 127.008739),
  ('POI003', 37.564009, 126.984213),
  ('POI004', 37.534822, 126.993558),
  ('POI005', 37.507681, 127.099113),
  ('POI006', 37.569138, 126.995237),
  ('POI007', 37.555941, 126.924264),
  ('POI008', 37.577613, 126.976898),
  ('POI009', 37.567211, 126.973604),
  ('POI010', 37.569768, 126.983646),
  ('POI011', 37.560546, 127.130258),
  ('POI012', 37.579647, 126.991),
  ('POI013', 37.480396, 126.882662),
  ('POI014', 37.498086, 127.028001),
  ('POI015', 37.540408, 127.069203),
  ('POI016', 37.555048, 127.154165),
  ('POI017', 37.504549, 127.005121),
  ('POI018', 37.492743, 127.013868),
  ('POI019', 37.485261, 126.901473),
  ('POI020', 37.503342, 126.882308),
  ('POI021', 37.557151, 127.079511),
  ('POI023', 37.49331, 126.894931),
  ('POI024', 37.571669, 127.010634),
  ('POI025', 37.547242, 127.047387),
  ('POI026', 37.613278, 127.030087),
  ('POI027', 37.558678, 126.837736),
  ('POI029', 37.476562, 126.981559),
  ('POI030', 37.534439, 126.972923),
  ('POI031', 37.481285, 126.952713),
  ('POI032', 37.569396, 126.835037),
  ('POI033', 37.554069, 126.970703),
  ('POI034', 37.505168, 127.04871),
  ('POI035', 37.592968, 127.017126),
  ('POI036', 37.637885, 127.025509),
  ('POI037', 37.504811, 127.025492),
  ('POI038', 37.508908, 126.891313),
  ('POI039', 37.484267, 126.929745),
  ('POI040', 37.55693, 126.945529),
  ('POI041', 37.484577, 127.034164),
  ('POI042', 37.500674, 127.036469),
  ('POI043', 37.619204, 126.921103),
  ('POI044', 37.530729, 126.882099),
  ('POI045', 37.561268, 127.037103),
  ('POI046', 37.530076, 126.964824),
  ('POI047', 37.534525, 126.994334),
  ('POI048', 37.478632, 127.126177),
  ('POI049', 37.561604, 127.063704),
  ('POI050', 37.538511, 127.123928),
  ('POI051', 37.485289, 126.981676),
  ('POI052', 37.559763, 126.964492),
  ('POI053', 37.549913, 126.914454),
  ('POI054', 37.582044, 127.001945),
  ('POI055', 37.556871, 126.923779),
  ('POI056', 37.589796, 127.058048),
  ('POI058', 37.493888, 127.110927),
  ('POI059', 37.52082, 127.022729),
  ('POI060', 37.570055, 126.998947),
  ('POI061', 37.558677, 126.802781),
  ('POI063', 37.514055, 126.94211),
  ('POI064', 37.565859, 126.972776),
  ('POI066', 37.581768, 126.984812),
  ('POI067', 37.578729, 126.973111),
  ('POI068', 37.546444, 127.043029),
  ('POI070', 37.648543, 127.034656),
  ('POI071', 37.526756, 127.039152),
  ('POI072', 37.521775, 126.924398),
  ('POI073', 37.564477, 126.922021),
  ('POI074', 37.517295, 126.903931),
  ('POI076', 37.531451, 126.972204),
  ('POI077', 37.531739, 126.993409),
  ('POI078', 37.574547, 126.985592),
  ('POI079', 37.653339, 127.047645),
  ('POI080', 37.526126, 127.046048),
  ('POI081', 37.57904, 127.039434),
  ('POI082', 37.539758, 126.991722),
  ('POI083', 37.567184, 127.009911),
  ('POI084', 37.576014, 126.901278),
  ('POI085', 37.588374, 126.813125),
  ('POI086', 37.498212, 126.867089),
  ('POI087', 37.544134, 127.118316),
  ('POI088', 37.572603, 126.976916),
  ('POI089', 37.521643, 126.978511),
  ('POI090', 37.56704, 126.875608),
  ('POI091', 37.552498, 126.989317),
  ('POI092', 37.51766, 126.958034),
  ('POI093', 37.529297, 127.068921),
  ('POI094', 37.551725, 126.900028),
  ('POI095', 37.51082, 126.997161),
  ('POI096', 37.623607, 127.041573),
  ('POI098', 37.495371, 127.003858),
  ('POI100', 37.436452, 127.013898),
  ('POI101', 37.544322, 127.037618),
  ('POI102', 37.566844, 127.102742),
  ('POI103', 37.537808, 126.902938),
  ('POI104', 37.55147, 127.083789),
  ('POI105', 37.526389, 126.935129),
  ('POI106', 37.563194, 126.897635),
  ('POI107', 37.548253, 127.029834),
  ('POI108', 37.51724, 126.970527),
  ('POI109', 37.514591, 127.073025),
  ('POI110', 37.517799, 127.082358),
  ('POI111', 37.527334, 127.019047),
  ('POI112', 37.422636, 127.041509),
  ('POI114', 37.561481, 126.978006),
  ('POI115', 37.559182, 126.977627),
  ('POI116', 37.573437, 126.989776),
  ('POI117', 37.520223, 126.85289),
  ('POI118', 37.511626, 127.086314),
  ('POI119', 37.513311, 127.100231),
  ('POI120', 37.511621, 127.1059),
  ('POI121', 37.509325, 127.110416),
  ('POI122', 37.556979, 126.936671),
  ('POI123', 37.493283, 126.920518),
  ('POI124', 37.574406, 126.955398),
  ('POI125', 37.527186, 126.881665),
  ('POI126', 37.532356, 126.923745),
  ('POI127', 37.520534, 127.120813),
  ('POI128', 37.580787, 126.937551),
  ('POI129', 37.576568, 126.981715),
  ('POI130', 37.567527, 126.976535),
  ('POI131', 37.560003, 126.975313)
  ) as v(area_cd, lat, lng)
 where a.area_cd = v.area_cd;

comment on column congestion_areas.lat is '지역 중심 위도. 이름을 카카오 검색한 좌표라 대략적인 값. 새 장소의 지역을 고를 때 쓴다';
comment on column congestion_areas.lng is '지역 중심 경도';


-- -----------------------------------------------------
-- 2. places — 카카오 id · 업종 '기타' · 지역 비움 허용
-- -----------------------------------------------------

-- 누가 넣었는지는 두지 않는다. places 는 누구나 읽는 표라 사용자 번호가 공개된다
alter table places
  add column if not exists kakao_id text unique;

-- 가장 가까운 지역도 1km 넘게 떨어진 가게는 지역 혼잡을 붙이지 않는다.
-- 먼 지역 값을 그 가게 혼잡처럼 보여주면 틀린 안내가 된다
alter table places alter column congestion_area_cd drop not null;

-- 키즈카페 · 도서관처럼 다섯 업종에 안 맞는 곳도 아이와 가는 곳이라 받는다
alter table places drop constraint if exists places_category_valid;
alter table places
  add constraint places_category_valid
    check (category in ('마트', '카페', '식당', '병원', '미용실', '백화점', '기타'));

alter table places drop constraint if exists places_source_valid;
alter table places
  add constraint places_source_valid
    check (source in ('직접입력', '카카오수집', '제주수기지도', '앱검색'));

comment on column places.kakao_id           is '카카오 장소 id. 앱 검색에서 같은 가게를 두 번 넣지 않게 한다';
comment on column places.congestion_area_cd is '이 가게가 쓸 혼잡도 지역 (congestion_areas). 1km 안에 지역이 없으면 비어 있다';

-- 홍대 5곳도 카카오 id 를 채워 둔다. 검색 결과에 같은 가게가 한 번 더 나오지 않게
update places p
   set kakao_id = v.kakao_id
  from (values
    ('홍대마트24시',                   '1479308772'),
    ('스타벅스 홍대역8번출구점',         '1485627824'),
    ('AK플라자 홍대',                  '1156421273'),
    ('투썸플레이스 홍대걷고싶은거리점',    '1351720419'),
    ('하모니마트 홍대입구점',            '564321063')
  ) as v(name, kakao_id)
 where p.name = v.name and p.kakao_id is null;


-- -----------------------------------------------------
-- 3. settings — 상권 매핑에 '기타'
-- -----------------------------------------------------
--
-- 업종 상권을 붙일 수 없어 병원 · 백화점처럼 빈 목록. 지역 혼잡만 쓴다

update settings
   set value      = value || '{"기타": []}'::jsonb,
       updated_at = now()
 where key = 'commercial_category_map';


-- -----------------------------------------------------
-- 4. add_kakao_place — 앱이 카카오 장소를 places 에 넣는 함수
-- -----------------------------------------------------
--
-- 로그인한 사람만 부를 수 있다. 이미 있는 가게면 넣지 않고 그 id 를 돌려준다.
-- security definer: places 에 쓰기 정책을 열지 않고 이 함수만 쓰게 한다

create or replace function add_kakao_place(
  p_kakao_id       text,
  p_name           text,
  p_category_group text,
  p_category_name  text,
  p_address        text,
  p_lat            double precision,
  p_lng            double precision
) returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id       bigint;
  v_category text;
  v_area     text;
  v_dist     double precision;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요해요';
  end if;
  if p_kakao_id is null or p_kakao_id !~ '^[0-9]{1,20}$' then
    raise exception '카카오 장소 id 형식이 아니에요';
  end if;
  if coalesce(length(trim(p_name)), 0) not between 1 and 100
     or coalesce(length(trim(p_address)), 0) not between 1 and 200 then
    raise exception '이름이나 주소가 비었거나 너무 길어요';
  end if;

  select id into v_id from places where kakao_id = p_kakao_id;
  if v_id is not null then
    return v_id;
  end if;

  -- 카카오 업종 코드: MT1 대형마트 · CS2 편의점 · CE7 카페 · FD6 음식점 · HP8 병원.
  -- 슈퍼마켓 · 백화점 · 미용실은 코드가 비어 있어 업종 이름으로 본다. '마트' 는 '스마트폰' 에 걸리지 않게 '> 마트' 로
  v_category := case
    when p_category_name like '%백화점%'                              then '백화점'
    when p_category_group in ('MT1', 'CS2')
      or p_category_name like '%슈퍼마켓%' or p_category_name like '%> 마트%'  then '마트'
    when p_category_group = 'CE7'                                      then '카페'
    when p_category_group = 'FD6'                                      then '식당'
    when p_category_group = 'HP8'                                      then '병원'
    when p_category_name like '%미용%'                                 then '미용실'
    else '기타'
  end;

  -- 가장 가까운 지역 (지구 반지름 6371km, 하버사인)
  select a.area_cd,
         2 * 6371000 * asin(sqrt(
           power(sin(radians(a.lat - p_lat) / 2), 2)
           + cos(radians(p_lat)) * cos(radians(a.lat)) * power(sin(radians(a.lng - p_lng) / 2), 2)
         ))
    into v_area, v_dist
    from congestion_areas a
   where a.lat is not null
   order by 2
   limit 1;
  if v_dist is null or v_dist > 1000 then
    v_area := null;
  end if;

  insert into places (name, category, address, lat, lng, congestion_area_cd, source, kakao_id)
  values (trim(p_name), v_category, trim(p_address), p_lat, p_lng, v_area, '앱검색', p_kakao_id)
  on conflict (kakao_id) do nothing
  returning id into v_id;

  -- 같은 가게를 거의 동시에 넣어 위 insert 가 건너뛰었으면 먼저 들어간 줄을 돌려준다
  if v_id is null then
    select id into v_id from places where kakao_id = p_kakao_id;
  end if;
  return v_id;
end;
$$;

comment on function add_kakao_place is '앱 검색에서 고른 카카오 장소를 places 에 넣고 id 를 돌려준다. 이미 있으면 그 id';

revoke all on function add_kakao_place(text, text, text, text, text, double precision, double precision) from public, anon;
grant execute on function add_kakao_place(text, text, text, text, text, double precision, double precision) to authenticated;
