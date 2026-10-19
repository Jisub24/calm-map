-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 16. 백화점 업종 추가 · 영업시간을 분 단위로
--
--     10/18 홍대 답사에서 5곳을 재고 실제 영업시간을 넣어보니
--     지금 구조로 들어가지 않는 것이 셋 있었다.
--
--       하모니마트     09:00 ~ 다음날 01:00    자정을 넘김
--       투썸플레이스   08:00 ~ 다음날 00:40    자정을 넘기고 분 단위
--       AK PLAZA      10:30 ~ 22:00          분 단위
--
--     그리고 AK PLAZA 는 업종 다섯 개(마트·카페·식당·병원·미용실)에
--     해당하는 것이 없다.
--
--     녹음 길이 기준도 같이 고친다 (4번).


-- -----------------------------------------------------
-- 1. places — 백화점 업종 추가
-- -----------------------------------------------------
--
-- 화면에 업종이 그대로 보인다. '마트' 로 넣으면 백화점을 마트라고
-- 안내하게 된다.

alter table places drop constraint if exists places_category_valid;

alter table places
  add constraint places_category_valid
    check (category in ('마트', '카페', '식당', '병원', '미용실', '백화점'));


-- -----------------------------------------------------
-- 2. place_hours — 시간을 분 단위로, 자정 넘김 허용
-- -----------------------------------------------------
--
-- smallint 로 '몇 시'만 담고 있어서 10:30 과 다음날 01:00 을 넣을 수 없었다.
--
-- 그래프는 1시간 칸이라 분이 필요 없지만, 장소 상세에서 영업시간을
-- 그대로 보여주려면 분까지 들고 있어야 한다.
-- 저장은 정확하게 하고, 그래프는 시(hour)만 꺼내 쓴다.
--
-- 자정 넘김은 time 으로 표현할 수 없어(최대 24:00) 플래그를 따로 둔다.
--   09:00 ~ 01:00 + closes_next_day = true   → 다음날 새벽 1시까지
--
-- 아직 한 줄도 안 들어 있고 읽는 코드도 없어서 지금이 바꾸기 가장 싸다.

alter table place_hours drop constraint if exists place_hours_open_range;
alter table place_hours drop constraint if exists place_hours_close_range;
alter table place_hours drop constraint if exists place_hours_open_before_close;
alter table place_hours drop constraint if exists place_hours_closed_or_hours;

alter table place_hours drop column if exists open_hour;
alter table place_hours drop column if exists close_hour;

alter table place_hours
  add column open_time       time,
  add column close_time      time,
  add column closes_next_day boolean not null default false;

-- 휴무면 시간이 비어 있고, 영업이면 둘 다 있어야 한다
alter table place_hours
  add constraint place_hours_closed_or_hours
    check (
      (is_closed = true  and open_time is null     and close_time is null)
      or
      (is_closed = false and open_time is not null and close_time is not null)
    );

-- 자정을 안 넘기면 여는 시각이 앞이고, 넘기면 닫는 시각이 앞이다.
-- 이 검사가 없으면 09:00~01:00 을 자정 넘김 표시 없이 넣어도 통과해서
-- 영업시간이 16시간이 아니라 '마이너스' 가 된다.
alter table place_hours
  add constraint place_hours_time_order
    check (
      is_closed = true
      or (closes_next_day = false and open_time < close_time)
      or (closes_next_day = true  and close_time < open_time)
    );

-- 쉬는 날인데 '다음날까지 영업' 이 켜져 있으면 말이 안 된다
alter table place_hours
  add constraint place_hours_closed_not_next_day
    check (is_closed = false or closes_next_day = false);

comment on column place_hours.open_time is
  '여는 시각. 분까지 저장한다. 그래프는 시(hour)만 꺼내 쓴다';
comment on column place_hours.close_time is
  '닫는 시각. 24시간 영업은 00:00 ~ 24:00 으로 넣는다';
comment on column place_hours.closes_next_day is
  '닫는 시각이 다음날인가. 09:00~01:00 처럼 자정을 넘기는 가게에 쓴다';


-- -----------------------------------------------------
-- 3. settings — 상권 매핑에 백화점 추가
-- -----------------------------------------------------
--
-- AK PLAZA 는 건물 전체를 돌며 측정해서 업종 상권을 붙일 수 없다.
-- 병원과 같은 처리로 빈 목록을 두고 지역 혼잡만 쓴다.

update settings
   set value      = value || '{"백화점": []}'::jsonb,
       updated_at = now()
 where key = 'commercial_category_map';


-- -----------------------------------------------------
-- 4. settings — 점수에 반영하는 최소 측정 길이 3분 → 1분
-- -----------------------------------------------------
--
-- 답사 녹음 중 하모니마트가 1분 53초라 3분 기준이면 점수에 못 쓴다.
-- 1분은 배경 소음(LA90)을 계산할 수 있는 최소 길이이고,
-- measurements.duration_sec 도 이미 60초 이상만 받는다.
--
-- 짧은 측정이 틀리는 경우는 '마침 조용했던 몇 분'인데,
-- 10분 미만은 '무던함'을 줄 수 없어서(measure_full_sec) 위험한 쪽으로 틀리지 않는다.
-- 10분 미만 측정이 조용하게 나오면 '후보'로 보여준다.

update settings
   set value       = '60'::jsonb,
       description = '점수에 반영하는 최소 측정 길이(초). 1분. 배경 소음(LA90)을 내려면 최소 1분이 필요하다',
       updated_at  = now()
 where key = 'measure_min_sec';
