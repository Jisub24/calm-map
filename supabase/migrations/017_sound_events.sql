-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 17. 홍대 녹음 분석(10/19~10/23) 뒤 바뀐 돌발 · 알림음 · 안내방송 규칙 반영
--     먼저 필요: 013 (sudden_count, announce_count), 005 (settings)
--
--     013 은 돌발을 "소리 크기 변화"로, 안내방송을 "앱이 자동 추정"으로 전제했다.
--     녹음을 돌려 보니 둘 다 틀렸다.
--       - 크기 변화로 세면 측정자 대화가 전부 돌발로 잡힌다 → 소리 라벨로 센다
--       - YAMNet 에 안내방송 라벨이 없고 말소리와 구분이 안 된다 → 측정자가 직접 표시
--       - 알림음(차임 · 벨 · 주문 완료음)은 라벨로 잘 잡힌다 → 따로 센다


-- -----------------------------------------------------
-- 1. measurements — 알림음 칸 추가, 안내방송은 "모름"을 허용
-- -----------------------------------------------------

alter table measurements
  add column alert_count smallint not null default 0;

comment on column measurements.alert_count is
  '알림음 횟수. 앱이 라벨(Chime · Bell · Mallet percussion 등)로 자동으로 센다. 3초 안의 연속음은 하나';

-- 안내방송은 사람이 표시한다. "없었어요" 는 0, "잘 모르겠어요" 는 비움.
-- 0 을 강제로 넣으면 "없었음" 과 "모름" 이 섞여 점수를 지어내게 된다.
alter table measurements
  alter column announce_count drop not null,
  alter column announce_count drop default;

comment on column measurements.announce_count is
  '안내방송 횟수. 측정자가 직접 표시한다 (측정 중 버튼 횟수, 또는 끝나고 있었어요=1 · 없었어요=0). 비어 있으면 "잘 모르겠어요"';

comment on column measurements.sudden_count is
  '돌발 횟수. 소리 라벨(알림음 · 부딪힘 · 경보 · 울음)로 센다. 말소리와 폰 만지는 소리는 빼고, 3초 안은 하나로 합친다';

comment on column measurements.sudden_max_rise is
  '돌발 중 배경(LA90) 대비 가장 크게 튄 정도(dB). 0.1초 최대값 기준';

-- 안내방송은 사람이, 돌발은 기계가 세므로 서로 크기를 비교할 수 없다
alter table measurements
  drop constraint if exists measurements_announce_within_sudden;

-- 알림음은 돌발의 일부라서 돌발보다 많을 수 없다
alter table measurements
  add constraint measurements_alert_within_sudden
    check (alert_count <= sudden_count);


-- -----------------------------------------------------
-- 2. sound_groups — 'announcement' 묶음은 알림음 라벨을 담는다
-- -----------------------------------------------------
--
-- 종류 이름은 그대로 두고(앱 민감 요인 선택지와 1:1) 뜻만 바꾼다.
-- 안내방송 자체는 라벨로 못 잡으니, 이 묶음에는 안내 앞에 나오는
-- 알림음 라벨만 들어간다. 실제 줄은 seed 에서 넣는다.

comment on column sound_groups.sound_type is
  '소리 종류 5개 중 하나. 민감 요인 선택지와 1:1 대응. announcement 는 안내방송 앞 알림음 라벨(차임 · 벨)을 담는다 — 안내 목소리는 라벨로 못 잡는다';


-- -----------------------------------------------------
-- 3. settings — 점수로 바꾸는 기준값 (10/22 임시값, 아이폰 15 답사 기준)
-- -----------------------------------------------------
--
-- batch 코드에 숫자를 박지 않고 여기서 읽는다.
-- 안드로이드 기기로 다시 재면 값만 고친다.

insert into settings (key, value, description) values
  ('score_sound_level', '{"db_zero": 50, "db_full": 80}'::jsonb,
   '소리 크기 → 점수. Leq(에너지 평균) 50dB 가 0점, 80dB 가 100점, 사이는 직선'),

  ('score_sudden_rate', '{"per_min": 10, "floor_sec": 300, "points": [[0, 0], [2, 40], [5, 70], [8, 100]]}'::jsonb,
   '돌발 횟수 → 점수. 10분당 횟수 0 · 2 · 5 · 8 번이 0 · 40 · 70 · 100 점. 5분보다 짧은 측정은 5분으로 친다'),

  ('score_sudden_rise', '{"points": [[0, 0], [10, 40], [20, 70], [30, 100]]}'::jsonb,
   '가장 크게 튄 정도(dB) → 점수. 0 · 10 · 20 · 30 dB 가 0 · 40 · 70 · 100 점. 돌발 횟수 점수와 높은 쪽을 쓴다'),

  ('score_light', '{"lux_zero": 150, "lux_full": 2000}'::jsonb,
   '밝기 → 점수. 조도 중앙값 150 lux 가 0점, 2000 lux 가 100점, 사이는 로그'),

  ('calm_min_total_sec', '600'::jsonb,
   '"무던함" 으로 내려면 한 시간대 칸에 모인 측정 시간 합이 이만큼(10분) 넘어야 한다. 모자라면 조용해도 "후보"')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();

-- measure_full_sec(한 번 측정이 10분 넘어야 무던함) 은 calm_min_total_sec 로 바뀐다.
-- 한 번에 10분 들고 있기는 어렵고, 같은 시간대에 여러 번 잰 합이 10분이면 충분하다.
-- 코드에서 쓰는 곳이 없어 바로 지운다.
delete from settings where key = 'measure_full_sec';

-- sudden_rise_db(10) 는 그대로 둔다. 돌발을 라벨로 세더라도
-- "튄 정도" 를 잴 때 배경 대비 기준선으로 계속 쓴다.
