-- =====================================================
-- 홍대 측정 장소 5곳과 영업시간
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 먼저 필요: migrations/016_hours_and_store.sql
-- =====================================================
--
-- 10/18 답사에서 소리와 조도를 잰 곳이다.
-- 이름 · 주소 · 좌표는 카카오 Local 검색 결과를 그대로 옮겼다.
-- 이름을 카카오 표기에 맞춘 이유: 나중에 검색을 카카오로 바꾸면
-- 이름으로 같은 가게인지 맞춰본다.
--
-- 다시 실행해도 안전하다. 장소는 같은 이름이 있으면 넣지 않고,
-- 영업시간은 덮어쓴다.


-- -----------------------------------------------------
-- 1. places
-- -----------------------------------------------------
--
-- 혼잡 지역은 칸이 하나라 한 곳만 고른다.
-- 역 출구 앞 4곳은 홍대입구역(POI055), 걷고싶은거리 안쪽 투썸만 홍대 관광특구(POI007).
-- 거리로는 못 나눈다. AK플라자와 투썸이 역에서 둘 다 250m 쯤이다.

insert into places (name, category, address, lat, lng, congestion_area_cd, source)
select v.name, v.category, v.address, v.lat, v.lng, v.area_cd, '카카오수집'
from (values
  ('스타벅스 홍대역8번출구점',        '카페',   '서울 마포구 양화로 166',      37.5567448560452,  126.924168013515,   'POI055'),
  ('하모니마트 홍대입구점',          '마트',   '서울 마포구 양화로18안길 7',   37.556827434107,   126.925088046004,   'POI055'),
  ('홍대마트24시',                  '마트',   '서울 서대문구 신촌로 1',      37.5588191547933,  126.927357543224,   'POI055'),
  ('AK플라자 홍대',                 '백화점', '서울 마포구 양화로 188',      37.55780854901018, 126.92640783829829, 'POI055'),
  ('투썸플레이스 홍대걷고싶은거리점', '카페',   '서울 마포구 어울마당로 109',   37.5547797111847,  126.922671603267,   'POI007')
) as v(name, category, address, lat, lng, area_cd)
where not exists (select 1 from places p where p.name = v.name);


-- -----------------------------------------------------
-- 2. place_hours
-- -----------------------------------------------------
--
-- 요일 범위로 적고 하루씩 펼쳐서 넣는다. 1=월 … 7=일.
-- 자정을 넘기는 곳은 closes_next_day = true.
-- 24시간 영업은 00:00 ~ 24:00.

insert into place_hours (place_id, weekday, is_closed, open_time, close_time, closes_next_day)
select p.id, g.weekday, false, h.open_time::time, h.close_time::time, h.next_day
from (values
  ('스타벅스 홍대역8번출구점',        1, 7, '07:00', '23:00', false),
  ('하모니마트 홍대입구점',          1, 7, '09:00', '01:00', true),
  ('홍대마트24시',                  1, 7, '00:00', '24:00', false),
  ('AK플라자 홍대',                 1, 5, '11:00', '22:00', false),
  ('AK플라자 홍대',                 6, 7, '10:30', '22:00', false),
  ('투썸플레이스 홍대걷고싶은거리점', 1, 7, '08:00', '00:40', true)
) as h(name, from_day, to_day, open_time, close_time, next_day)
join places p on p.name = h.name
cross join lateral generate_series(h.from_day, h.to_day) as g(weekday)
on conflict (place_id, weekday) do update
  set is_closed       = excluded.is_closed,
      open_time       = excluded.open_time,
      close_time      = excluded.close_time,
      closes_next_day = excluded.closes_next_day;


-- 넣은 결과. 35줄이 나와야 한다
select p.name, h.weekday, h.open_time, h.close_time, h.closes_next_day
from place_hours h
join places p on p.id = h.place_id
order by p.id, h.weekday;
