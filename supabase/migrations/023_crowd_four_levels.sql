-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 23. 장소 혼잡 설문을 4단계로
--     먼저 필요: 019
--
--     앱 측정 완료 화면의 "사람이 얼마나 있었나요?" 선택지를
--     한가함 / 보통 / 붐빔 / 매우 붐빔 / 잘 모르겠어요 로 바꿨다.
--     019 의 3단계(빈자리 많음 / 반쯤 / 거의 다 참)에는 "한가함"에 해당하는 답이 없어
--     한산한 곳을 측정해도 0점 쪽 답을 고를 수 없었다.
--
--     0 한가함 / 1 보통 / 2 붐빔 / 3 매우 붐빔 / 비어 있으면 잘 모르겠어요
--     batch(calculate_score.py)는 settings.score_crowd_level[답] 을 그대로 쓰므로
--     값 4개로 늘리면 코드를 안 고쳐도 된다.

alter table measurements
  drop constraint if exists measurements_crowd_level_range;

alter table measurements
  add constraint measurements_crowd_level_range
    check (crowd_level is null or crowd_level between 0 and 3);

comment on column measurements.crowd_level is
  '장소 혼잡 설문 답. 0 한가함 / 1 보통 / 2 붐빔 / 3 매우 붐빔 / 비어 있으면 잘 모르겠어요. 종합 점수의 "장소 혼잡" 항목';

-- 단계 경계(40 · 70)에 맞춘 임시값. 한가함은 무던함, 보통은 보통, 붐빔부터 부담 높음
update settings
   set value       = '[15, 45, 70, 90]'::jsonb,
       description = '장소 혼잡 설문 답 0 · 1 · 2 · 3 (한가함 / 보통 / 붐빔 / 매우 붐빔) → 15 · 45 · 70 · 90 점. 임시값 [10/28]',
       updated_at  = now()
 where key = 'score_crowd_level';
