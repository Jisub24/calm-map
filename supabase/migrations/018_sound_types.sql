-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 18. 소리 종류 7종, 안내방송 설문 칸, 안내방송 점수표
--     먼저 필요: 017 (alert_count, announce_count 비움 허용)
--
--     013 은 "안내방송 · 돌발은 dB 로 잡으니 종류에서 뺀다" 며 소리 종류를
--     답변서의 4종(경보음 · 사람의 큰 소리 · 기계음 · 지속 소음)으로 바꿨다.
--     017 에서 그 전제가 뒤집혔고(라벨로 센다), 4종은 앱의 민감 요인 선택지
--     (안내방송 · 아기 울음 · 사이렌·경보 · 사람 말소리 · 음악 · 공사 소음)와
--     맞지 않아 "우리 아이 맞춤" 에 쓸 수 없다. 선택지 하나하나에 대응하는 7종으로 바꾼다.


-- -----------------------------------------------------
-- 1. sound_groups — 소리 종류 4종 → 7종
-- -----------------------------------------------------
--
--   alert    알림음        Chime · Bell · Mallet percussion …   → 민감 요인 "안내방송" (안내 앞 신호)
--   impact   부딪힘        Dishes · Cutlery · Glass · Bang …    → (선택지 없음. 돌발 횟수에만)
--   alarm    경보·큰 소리   Alarm · Siren · Beep · Screaming …   → "사이렌·경보"
--   cry      울음          Baby cry · Crying                    → "아기 울음"
--   speech   말소리        Speech · Conversation · Crowd …      → "사람 말소리"
--   music    음악          Music · Background music …           → "음악"
--   machine  기계음        Air conditioning · Drill · Hammer …  → "공사 소음"
--
-- "갑작스러운 소리" 는 종류가 아니라 규칙이다: 돌발 횟수 = alert + impact + alarm + cry.
-- 한 라벨을 두 종류에 넣지 않아도 되어 비율이 두 번 세어질 일이 없다.
-- 지금 0줄이라 값 이전이 필요 없다.

alter table sound_groups drop constraint if exists sound_groups_type_valid;
alter table sound_groups
  add constraint sound_groups_type_valid
    check (sound_type in ('alert', 'impact', 'alarm', 'cry', 'speech', 'music', 'machine'));

comment on column sound_groups.sound_type is
  'alert 알림음 / impact 부딪힘 / alarm 경보·큰 소리 / cry 울음 / speech 말소리 / music 음악 / machine 기계음. '
  '돌발 횟수 = alert + impact + alarm + cry. 안내 목소리는 라벨로 못 잡아서 announcement 종류는 없다';


-- -----------------------------------------------------
-- 2. measurements — 안내방송 설문 답
-- -----------------------------------------------------
--
-- 측정 중 버튼을 누르기 어렵다 (보호자는 아이를 보고 있다). 끝나고 한 번 묻는다.
--   "측정하는 동안 안내방송이 몇 번 나왔나요?"
--     없었어요 0 / 1~2번 1 / 3~5번 2 / 6번 이상 3 / 잘 모르겠어요 비움
-- "많았음 · 보통" 같은 말 대신 횟수 범위로 묻는다. 사람마다 "보통" 이 다르다.
--
-- announce_count(버튼 횟수)는 그대로 둔다. 둘 다 있으면 버튼 횟수가 정확하니 그쪽을 쓴다.

alter table measurements
  add column announce_level smallint;

comment on column measurements.announce_level is
  '안내방송 설문 답. 0 없었어요 / 1 = 1~2번 / 2 = 3~5번 / 3 = 6번 이상 / 비어 있으면 잘 모르겠어요. '
  '버튼 횟수(announce_count)가 있으면 그쪽을 우선한다';

alter table measurements
  add constraint measurements_announce_level_range
    check (announce_level is null or announce_level between 0 and 3);


-- -----------------------------------------------------
-- 3. settings — 안내방송 점수표
-- -----------------------------------------------------
--
-- 답변서에 숫자가 없어서 정한 임시값 [10/24].
-- 10분 측정에서 1~2번(계산 1.5)이 45점 보통, 3~5번(4)이 80점 부담 높음, 6번 이상(7)이 100점.
-- 설문 범위 세 개가 서로 다른 점수로 갈라지게 점수표를 완만하게 잡았다.

insert into settings (key, value, description) values
  ('score_announce_rate',
   '{"per_min": 10, "floor_sec": 300, "points": [[0, 0], [1, 35], [2, 55], [4, 80], [6, 100]], "level_counts": [0, 1.5, 4, 7]}'::jsonb,
   '안내방송 → 점수. 10분당 횟수 0 · 1 · 2 · 4 · 6 번이 0 · 35 · 55 · 80 · 100 점. 5분보다 짧은 측정은 5분으로 친다. '
   'level_counts 는 설문 답(0~3)을 횟수로 바꿀 때 쓰는 값')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();
