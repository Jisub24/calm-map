-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 24. 장소별 측정 요약 뷰 — 장소 상세의 소리 · 빛 탭이 읽는다
--     먼저 필요: 007, 018(sound_groups 7종)
--
--     measurements · measurement_labels 는 "자기 것만 읽기" 라서(006 · 007) 앱이
--     다른 사람이 잰 값을 볼 수 없다. 그런데 장소 상세는 그 장소에서 잰 모든 측정을
--     합쳐 "이 장소에서 들리는 소리" · "밝기 N lux" 를 보여줘야 한다.
--
--     원본을 열지 않고 장소별 대표값만 뷰로 낸다. user_id · device_model · 시각 같은
--     개인을 가리킬 수 있는 칸은 뷰에 넣지 않는다. 뷰는 만든 사람(postgres) 권한으로
--     돌아서 원본의 RLS 를 넘어 읽고, 결과만 anon · authenticated 에게 준다.
--
--     답변서 기준
--       - 밝기 · 배경음악 · 안내방송 빈도는 장소 고유값이다 (답변서2 질의1).
--         그래서 시간대로 나누지 않고 장소마다 하나로 낸다.
--         시간대마다 달라지는 혼잡 · 소리 크기는 hourly_forecast 가 칸별로 낸다
--       - 관측을 모을 때는 평균이 아니라 중앙값 (답변서1 질의2).
--         한 번 튄 측정이 대표값을 끌고 가지 않게 한다
--
--     1분 미만 측정은 점수(013)와 같은 이유로 뺀다.


-- -----------------------------------------------------
-- 1. place_sound_summary — 장소별 소리 종류 7종이 들린 비율
-- -----------------------------------------------------
--
--   ratio  그 종류가 측정 시간 중 얼마나 들렸나 (0~1).
--          측정 하나 안에서는 종류에 속한 라벨 중 가장 높은 비율을 쓴다
--          (더하면 비슷한 라벨이 많은 말소리가 부풀려진다. 앱 SoundGroups.scoresOf 와 같은 이유).
--          측정끼리는 중앙값. 그 종류가 안 잡힌 측정은 0 으로 센다

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
-- 안 잡힌 종류를 0 으로 채워야 중앙값이 "잡힌 측정만의 중앙값" 으로 부풀지 않는다
filled as (
  select v.place_id, v.id as measurement_id, t.sound_type, coalesce(p.ratio, 0) as ratio
    from valid v
    cross join (select distinct sound_type from sound_groups) t
    left join per_measurement p on p.measurement_id = v.id and p.sound_type = t.sound_type
)
select place_id,
       sound_type,
       round((percentile_cont(0.5) within group (order by ratio))::numeric, 3) as ratio,
       count(*)::int                                                       as measurement_count
  from filled
 group by place_id, sound_type;

comment on view place_sound_summary is
  '장소별 소리 종류 비율 (측정끼리 중앙값). 측정 원본 대신 앱이 읽는다 (소리 탭). 개인을 가리키는 칸 없음';


-- -----------------------------------------------------
-- 2. place_measure_summary — 장소별 소리 크기 · 밝기 · 돌발 · 안내방송 · 혼잡 답
-- -----------------------------------------------------
--
--   db_median          기종 보정 전 Leq 의 중앙값. 화면에 "평균 소리 크기 N dB" 로 쓴다
--                      (측정 하나의 Leq 자체가 에너지 평균이라 문구는 '평균')
--   lux_median         조도가 유효한 측정만의 중앙값
--   sudden_per_hour    측정마다 돌발 횟수 ÷ 측정 시간 × 3600 → 중앙값. "1시간에 N회"
--   announce_per_hour  안내방송. 버튼 횟수가 있으면 그것, 없으면 설문 답을 횟수로
--                      (0 · 1.5 · 4 · 7 — settings.score_announce_rate.level_counts 와 같은 값)
--                      둘 다 비어 있는 측정은 '모름' 이라 뺀다
--   두 횟수 모두 짧은 측정은 5분으로 친다 (settings.score_*_rate.floor_sec = 300 과 같은 이유:
--   1분 측정에서 한 번 걸리면 1시간에 60회가 된다)
--   crowd_median       혼잡 설문 답의 중앙값 (0 한가함 ~ 3 매우 붐빔, 023). 답이 있는 측정만

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
       percentile_disc(0.5) within group (order by crowd_level)                   as crowd_median
  from valid
 group by place_id;

comment on view place_measure_summary is
  '장소별 측정 대표값 (측정끼리 중앙값). 빛 탭 · 소리 탭 돌발 · 점수 탭 주 요인 설명이 읽는다. 개인을 가리키는 칸 없음';


grant select on place_sound_summary, place_measure_summary to anon, authenticated;
