-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 29. 사업장 웹 — 로그인 없이 내 가게 찾기 · 영업시간 저장
--     먼저 필요: 027 (add_kakao_place, places.kakao_id), 016 (place_hours 분 단위)
--
--     사업장 웹은 회원가입이 없는 시연용이라 Publishable(anon) key 로만 붙는다 (docs/03 8번).
--     표에 쓰기 정책을 여는 대신, 값을 확인하고 넣는 함수 두 개만 연다.
--
--     1. places.source 에 '사업장등록'
--     2. kakao_place_id()    027 add_kakao_place 의 본문을 출처만 받게 떼어 낸 것 (밖에서 못 부름)
--        add_kakao_place()   앱 검색. 로그인한 사람만, 출처 '앱검색' (이름 · 받는 값 그대로)
--        add_business_place() 사업장 웹. 로그인 없이, 출처 '사업장등록'
--     3. set_place_hours()   한 장소의 요일별 영업시간 7줄을 한 번에 바꾼다
--
--     영업시간은 점수 칸을 만드는 데 쓰여서(batch 가 영업 중인 시각만 칸을 만든다)
--     누구나 바꿀 수 있으면 장난 입력으로 그래프가 틀어질 수 있다. 시연 동안만 이렇게 두고,
--     사업장 인증을 붙이면 함수 권한을 거둔다.


-- -----------------------------------------------------
-- 1. places.source — '사업장등록'
-- -----------------------------------------------------

alter table places drop constraint if exists places_source_valid;
alter table places
  add constraint places_source_valid
    check (source in ('직접입력', '카카오수집', '제주수기지도', '앱검색', '사업장등록'));


-- -----------------------------------------------------
-- 2. 카카오 장소 넣기 — 앱 · 사업장 웹이 같은 규칙
-- -----------------------------------------------------
--
-- 업종 · 혼잡 지역을 정하는 규칙은 027 과 같다. 두 곳에 같은 코드를 두면 한쪽만 고치게 되어 떼어 낸다.

create or replace function kakao_place_id(
  p_kakao_id       text,
  p_name           text,
  p_category_group text,
  p_category_name  text,
  p_address        text,
  p_lat            double precision,
  p_lng            double precision,
  p_source         text
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
  if p_kakao_id is null or p_kakao_id !~ '^[0-9]{1,20}$' then
    raise exception '카카오 장소 id 형식이 아니에요';
  end if;
  if coalesce(length(trim(p_name)), 0) not between 1 and 100
     or coalesce(length(trim(p_address)), 0) not between 1 and 200 then
    raise exception '이름이나 주소가 비었거나 너무 길어요';
  end if;
  if p_lat not between 33 and 39 or p_lng not between 124 and 132 then
    raise exception '국내 좌표가 아니에요';
  end if;

  select id into v_id from places where kakao_id = p_kakao_id;
  if v_id is not null then
    return v_id;
  end if;

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
  values (trim(p_name), v_category, trim(p_address), p_lat, p_lng, v_area, p_source, p_kakao_id)
  on conflict (kakao_id) do nothing
  returning id into v_id;

  if v_id is null then
    select id into v_id from places where kakao_id = p_kakao_id;
  end if;
  return v_id;
end;
$$;

comment on function kakao_place_id is
  '카카오 장소를 places 에 넣고 id 를 돌려준다. 이미 있으면 그 id. add_kakao_place · add_business_place 가 부른다';

revoke all on function kakao_place_id(text, text, text, text, text, double precision, double precision, text) from public, anon, authenticated;


-- 앱 검색 (027 과 이름 · 받는 값 · 권한이 같다)
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
begin
  if auth.uid() is null then
    raise exception '로그인이 필요해요';
  end if;
  return kakao_place_id(p_kakao_id, p_name, p_category_group, p_category_name, p_address, p_lat, p_lng, '앱검색');
end;
$$;


-- 사업장 웹. 로그인이 없다
create or replace function add_business_place(
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
begin
  return kakao_place_id(p_kakao_id, p_name, p_category_group, p_category_name, p_address, p_lat, p_lng, '사업장등록');
end;
$$;

comment on function add_business_place is
  '사업장 웹에서 고른 카카오 장소를 places 에 넣고 id 를 돌려준다. 이미 있으면 그 id. 로그인 없이 부른다 (시연용)';

revoke all on function add_business_place(text, text, text, text, text, double precision, double precision) from public;
grant execute on function add_business_place(text, text, text, text, text, double precision, double precision) to anon, authenticated;


-- -----------------------------------------------------
-- 3. set_place_hours — 요일별 영업시간 7줄을 한 번에
-- -----------------------------------------------------
--
-- p_hours 예 (요일 1=월 … 7=일, 7개 모두)
--   [{"weekday":1, "is_closed":false, "open_time":"09:00", "close_time":"22:00", "closes_next_day":false},
--    {"weekday":7, "is_closed":true},
--    {"weekday":5, "is_closed":false, "open_time":"10:00", "close_time":"01:00", "closes_next_day":true}, …]
--
-- 일부 요일만 넣으면 나머지가 '영업 안 함' 이 되어 그 요일 점수 칸이 사라진다. 그래서 7개를 다 받는다.
-- 시간 규칙(여는 · 닫는 순서, 자정 넘김, 쉬는 날)은 016 의 표 규칙이 그대로 막는다.
-- 지우고 넣는 것이 한 함수 안이라 중간에 실패하면 원래 영업시간이 남는다.

create or replace function set_place_hours(p_place_id bigint, p_hours jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (select 1 from places where id = p_place_id) then
    raise exception '장소를 찾지 못했어요';
  end if;
  if jsonb_typeof(p_hours) <> 'array'
     or jsonb_array_length(p_hours) <> 7
     or (select count(distinct (h->>'weekday')::int) from jsonb_array_elements(p_hours) h
          where (h->>'weekday')::int between 1 and 7) <> 7 then
    raise exception '월요일부터 일요일까지 7일을 모두 보내 주세요';
  end if;

  delete from place_hours where place_id = p_place_id;
  insert into place_hours (place_id, weekday, is_closed, open_time, close_time, closes_next_day)
  select p_place_id,
         (h->>'weekday')::smallint,
         coalesce((h->>'is_closed')::boolean, false),
         case when coalesce((h->>'is_closed')::boolean, false) then null else (h->>'open_time')::time end,
         case when coalesce((h->>'is_closed')::boolean, false) then null else (h->>'close_time')::time end,
         coalesce((h->>'closes_next_day')::boolean, false)
    from jsonb_array_elements(p_hours) h;
end;
$$;

comment on function set_place_hours is
  '사업장 웹에서 한 장소의 요일별 영업시간 7줄을 바꾼다. 로그인 없이 부른다 (시연용)';

revoke all on function set_place_hours(bigint, jsonb) from public;
grant execute on function set_place_hours(bigint, jsonb) to anon, authenticated;
