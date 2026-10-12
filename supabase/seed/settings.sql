-- =====================================================
-- settings 기본값
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 먼저 필요: migrations/005_settings.sql
-- =====================================================
--
-- 여기 있는 값은 전부 "일단 정한 값"이다.
-- analysis/01 녹음 실험과 첫 측정 데이터를 보고 고친다.
-- 고칠 때 앱을 다시 빌드하지 않아도 되도록 DB 에 두는 것이 이 테이블의 목적이다.
--
-- 다시 실행해도 안전하다 (on conflict do update).

insert into settings (key, value, description) values
  ('level_thresholds',
   '{"calm": 40, "normal": 70}'::jsonb,
   '단계 기준선. 점수 40 미만 무던함 / 40~70 보통 / 70 이상 붐빔'),

  ('min_measurements',
   '3'::jsonb,
   '단계를 보여주기 위한 최소 측정 수. 이보다 적으면 추정 또는 정보 부족'),

  ('rank_weights',
   '{"1": 1.5, "2": 1.3, "3": 1.1}'::jsonb,
   '민감 요인 순위별 가중치. 1순위로 고른 자극에 1.5배'),

  ('learning_rate',
   '0.05'::jsonb,
   '자동 학습이 가중치를 한 번에 움직이는 폭. 방문 기록 1건당 0.05'),

  ('measure_duration_sec',
   '15'::jsonb,
   '측정 길이(초). 매장에서 폰을 들고 서 있는 부담을 줄이려 15초로 정했다')

on conflict (key) do update
  set value       = excluded.value,
      description = excluded.description,
      updated_at  = now();

-- device_calibration(기종별 보정 계수)은 아직 넣지 않는다.
-- 실기기로 재 보기 전에는 넣을 값이 없다. 기종이 없으면 1.0 으로 보면 된다.
