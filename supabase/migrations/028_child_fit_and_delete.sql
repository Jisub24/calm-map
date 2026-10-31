-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 28. 우리 아이 맞춤 · 내 데이터 모두 삭제 · 회원 탈퇴
--     먼저 필요: 024 (place_sound_summary), 006 · 010 · 011 · 014
--
--     1. place_sound_summary 에 heard_count — 그 소리가 한 번이라도 들린 측정 수
--        place_measure_summary 에 sudden_heard_count · announce_heard_count — 돌발 · 안내방송이 있었던 측정 수
--     2. 측정은 계정을 지워도 익명으로 남긴다 (measurements.user_id 비움 허용)
--     3. delete_my_data()    방문 기록 · 우리 아이 설정 · 측정 요청을 지우고, 내 측정은 계정 연결만 끊는다
--     4. delete_my_account() 3 + 계정까지 지운다


-- -----------------------------------------------------
-- 1. place_sound_summary — heard_count
-- -----------------------------------------------------
--
-- 우리 아이 맞춤은 '매우' 로 고른 자극이 관측되면 다른 점수와 상관없이 '주의' 로 알린다 (답변서1 질의1).
-- ratio 는 측정끼리 중앙값이라, 측정 절반 이상에서 들려야 0 보다 커진다.
-- 경보음이 측정 3번 중 1번 들린 곳도 '관측된 곳' 이라 한 번이라도 들린 측정 수를 따로 낸다.
-- 앞 칸 순서는 024 와 같게 둔다 (create or replace view 는 칸을 뒤에만 붙일 수 있다)

create or replace view place_sound_summary as
with valid as (
  select id, place_id from measurements where duration_sec >= 60
),
per_measurement as (
  select v.place_id, v.id as measurement_id, g.sound_type, max(l.ratio) as ratio
    from valid v
    join measurement_labels l on l.measurement_id = v.id
    join sound_groups g       on g.yamnet_label = l.yamnet_label
   group by v.place_id, v.id, g.sound_type
),
filled as (
  select v.place_id, v.id as measurement_id, t.sound_type, coalesce(p.ratio, 0) as ratio
    from valid v
    cross join (select distinct sound_type from sound_groups) t
    left join per_measurement p on p.measurement_id = v.id and p.sound_type = t.sound_type
)
select place_id,
       sound_type,
       round((percentile_cont(0.5) within group (order by ratio))::numeric, 3) as ratio,
       count(*)::int                                                       as measurement_count,
       (count(*) filter (where ratio > 0))::int                            as heard_count
  from filled
 group by place_id, sound_type;

comment on column place_sound_summary.heard_count is
  '그 소리가 한 번이라도 들린 측정 수. 우리 아이 맞춤에서 ''매우'' 로 고른 소리가 관측됐는지 볼 때 쓴다';


-- -----------------------------------------------------
-- 1-2. place_measure_summary — sudden_heard_count · announce_heard_count
-- -----------------------------------------------------
--
-- '갑작스러운 소리' 도 같은 이유. sudden_per_hour · announce_per_hour 는 측정끼리 중앙값이라
-- 측정 3번 중 1번만 돌발이 있으면 0 이 된다. 돌발 · 안내방송이 한 번이라도 있었던 측정 수를 따로 낸다.
-- 앞 칸은 024 와 같다

create or replace view place_measure_summary as
with valid as (
  select *,
         greatest(duration_sec, 300) as rate_sec,
         coalesce(announce_count::numeric,
                  case announce_level
                    when 0 then 0 when 1 then 1.5 when 2 then 4 when 3 then 7
                  end) as announce_times
    from measurements
   where duration_sec >= 60
)
select place_id,
       count(*)::int                                                              as measurement_count,
       round((percentile_cont(0.5) within group (order by avg_db))::numeric, 1)   as db_median,
       round((percentile_cont(0.5) within group (order by avg_lux)
              filter (where lux_status = '측정유효'))::numeric)                    as lux_median,
       count(*) filter (where lux_status = '측정유효')::int                        as lux_count,
       round((percentile_cont(0.5) within group
              (order by sudden_count::numeric / rate_sec * 3600))::numeric, 1)    as sudden_per_hour,
       round((percentile_cont(0.5) within group
              (order by announce_times / rate_sec * 3600)
              filter (where announce_times is not null))::numeric, 1)             as announce_per_hour,
       percentile_disc(0.5) within group (order by crowd_level)                   as crowd_median,
       (count(*) filter (where sudden_count > 0))::int                            as sudden_heard_count,
       (count(*) filter (where announce_times > 0))::int                          as announce_heard_count
  from valid
 group by place_id;

comment on column place_measure_summary.sudden_heard_count is
  '돌발이 한 번이라도 있었던 측정 수. 우리 아이 맞춤 ''갑작스러운 소리'' 관측 여부';
comment on column place_measure_summary.announce_heard_count is
  '안내방송이 한 번이라도 있었던 측정 수 (버튼 · 설문). 우리 아이 맞춤 ''갑작스러운 소리'' 관측 여부';


-- -----------------------------------------------------
-- 2. measurements — 계정을 지우면 측정은 익명으로
-- -----------------------------------------------------
--
-- 측정은 장소 점수의 재료다. 원음 · 이름 · 연락처가 없어서 계정 연결만 끊으면 누구 것인지 알 수 없다.
-- 측정까지 지우면 다른 보호자가 보는 장소 점수가 바뀐다.
-- user_id 가 빈 측정은 RLS("자기 것만 읽기")에 아무에게도 안 보이고, 요약 뷰와 batch 에만 쓰인다.

alter table measurements alter column user_id drop not null;
alter table measurements drop constraint if exists measurements_user_id_fkey;
alter table measurements
  add constraint measurements_user_id_fkey
    foreign key (user_id) references auth.users(id) on delete set null;

comment on column measurements.user_id is
  '누가 쟀나. RLS 가 "자기 것만" 을 강제하는 기준. 내 데이터 삭제 · 회원 탈퇴 뒤에는 비어서 익명 측정으로 남는다';


-- -----------------------------------------------------
-- 3. delete_my_data — 내 데이터 모두 삭제
-- -----------------------------------------------------
--
-- user_profiles 와 measurements 에는 지우기 · 바꾸기 정책이 없어서 앱이 직접 못 한다.
-- 정책을 넓히는 대신 자기 것만 지우는 이 함수만 연다.

create or replace function delete_my_data()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  if v_me is null then
    raise exception '로그인이 필요해요';
  end if;

  delete from visits               where user_id = v_me;
  delete from measurement_requests where user_id = v_me;
  delete from user_profiles        where user_id = v_me;
  update measurements set user_id = null where user_id = v_me;
end;
$$;

comment on function delete_my_data() is
  '방문 기록 · 우리 아이 설정 · 측정 요청을 지우고 내 측정은 익명으로 남긴다. 계정은 그대로';

revoke all on function delete_my_data() from public, anon;
grant execute on function delete_my_data() to authenticated;


-- -----------------------------------------------------
-- 4. delete_my_account — 회원 탈퇴
-- -----------------------------------------------------
--
-- 계정(auth.users)을 지우면 visits · user_profiles · measurement_requests 는 cascade 로,
-- measurements 는 2번 규칙으로 익명이 된다. 먼저 delete_my_data 로 같은 일을 해 두고 계정을 지운다.
-- 계정을 못 지우면 함수 전체가 되돌려져서 데이터만 지워진 채 계정이 남지 않는다.

create or replace function delete_my_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  if v_me is null then
    raise exception '로그인이 필요해요';
  end if;

  perform delete_my_data();
  delete from auth.users where id = v_me;
end;
$$;

comment on function delete_my_account() is
  '회원 탈퇴. 내 데이터를 지우고(측정은 익명) 계정을 지운다';

revoke all on function delete_my_account() from public, anon;
grant execute on function delete_my_account() to authenticated;
