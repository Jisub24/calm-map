-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 20. 우리 아이 설정 — 자극 7개 × 보통/민감/매우, 소리 묶음 '사람의 큰 소리'
--     먼저 필요: 018 (sound_groups 7종), seed/sound_groups.sql
--
--     user_profiles(010) 는 "자극 6개 중 고른 것에 순위를 매기고, 순위별 가중치를
--     점수에 곱한다" 는 전제로 만들었다. 답변서 반영 뒤 맞춤은
--       "보호자가 '매우' 로 고른 자극이 관측되면 다른 점수와 무관하게 '주의' 표시"
--     로 바뀌어 가중치를 곱하지 않는다 (docs/12 1번). 앱 최종안 05 화면도
--     자극 7개마다 보통 / 민감 / 매우 를 고른다. DB 를 화면에 맞춘다.


-- -----------------------------------------------------
-- 1. user_profiles — 순위 + 가중치 → 자극 7개 × 3단계
-- -----------------------------------------------------
--
--   sensitivities 예:
--     {"sudden":"very", "alarm":"normal", "loud_voice":"sensitive", "machine":"normal",
--      "sustained":"normal", "crowd":"very", "light":"normal"}
--
--   키 (앱 SensoryFactor 와 1:1)
--     sudden      갑작스러운 소리 (안내방송 · 알림음 · 부딪힘)
--     alarm       경보음 · 사이렌
--     loud_voice  사람의 큰 소리 (고함 · 울음)
--     machine     기계음
--     sustained   계속되는 큰 소리 (음악 · 웅성거림)
--     crowd       사람 많음
--     light       밝은 조명
--   값: normal 보통 / sensitive 민감 / very 매우. 키가 없으면 normal 로 본다.
--
-- 지금 줄이 없어 값 이전은 필요 없다.

alter table user_profiles
  drop constraint if exists user_profiles_weight_range,
  drop column if exists weight_announcement,
  drop column if exists weight_speech,
  drop column if exists weight_music,
  drop column if exists weight_machine,
  drop column if exists weight_sudden,
  drop column if exists weight_crowd;

alter table user_profiles
  alter column sensitivities set default '{}'::jsonb,
  alter column sensitivities set not null;

-- 모르는 값이 들어오면 맞춤 화면이 조용히 틀어지므로 값을 세 가지로 묶는다.
-- 키가 빠진 것은 normal 과 같으므로 허용한다.
alter table user_profiles
  add constraint user_profiles_sensitivities_valid
    check (
      jsonb_typeof(sensitivities) = 'object'
      and coalesce(sensitivities->>'sudden',     'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'alarm',      'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'loud_voice', 'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'machine',    'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'sustained',  'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'crowd',      'normal') in ('normal', 'sensitive', 'very')
      and coalesce(sensitivities->>'light',      'normal') in ('normal', 'sensitive', 'very')
    );

comment on table  user_profiles is
  '보호자가 고른 아이의 민감 자극. 계정 하나당 한 줄. 아이 이름 · 나이 · 진단명 칸은 없다';
comment on column user_profiles.sensitivities is
  '자극 7개(sudden · alarm · loud_voice · machine · sustained · crowd · light) × normal / sensitive / very. '
  '키가 없으면 normal. very 로 고른 자극이 관측된 시간대는 점수와 무관하게 "주의"';


-- -----------------------------------------------------
-- 2. sound_groups — cry(울음) → loud_voice(사람의 큰 소리)
-- -----------------------------------------------------
--
-- 앱 자극 "사람의 큰 소리 (고함 · 울음)" 에 맞춘다. 018 에서는 울음만 cry 로 두고
-- 고함(Screaming · Yell)을 alarm 에, Shout · Children shouting 을 speech 에 넣었는데,
-- 그러면 "사람의 큰 소리" 를 매우 로 고른 아이에게 고함을 못 잡는다.
-- 돌발 횟수 = alert + impact + alarm + loud_voice (cry 자리를 loud_voice 가 대신한다).

alter table sound_groups drop constraint if exists sound_groups_type_valid;

update sound_groups set sound_type = 'loud_voice'
 where sound_type = 'cry'
    or yamnet_label in ('Screaming', 'Yell', 'Shout', 'Children shouting');

alter table sound_groups
  add constraint sound_groups_type_valid
    check (sound_type in ('alert', 'impact', 'alarm', 'loud_voice', 'speech', 'music', 'machine'));

comment on column sound_groups.sound_type is
  'alert 알림음 / impact 부딪힘 / alarm 경보·사이렌 / loud_voice 사람의 큰 소리(고함 · 울음) / speech 말소리 / music 음악 / machine 기계음. '
  '돌발 횟수 = alert + impact + alarm + loud_voice. 안내 목소리는 라벨로 못 잡아서 announcement 종류는 없다';
