-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 11. visits — 방문 예정과 다녀온 뒤 기록
--     [방문 확정] 에서 줄이 생기고, 다녀온 뒤 [괜찮았어요/힘들었어요] 로 채워진다
--     먼저 필요: 002_places
--
--     사용자가 수정 · 삭제까지 하는 유일한 테이블이다.
--     방문 예정은 사실 기록이 아니라 계획이라 취소할 수 있어야 한다.

create table visits (
  id           bigint generated always as identity primary key,
  user_id      uuid   not null references auth.users(id) on delete cascade,
  place_id     bigint not null references places(id) on delete cascade,
  planned_at   timestamptz not null,
  result       text,
  hard_factors text[],
  recorded_at  timestamptz,
  created_at   timestamptz not null default now(),

  constraint visits_result_valid
    check (result is null or result in ('ok', 'hard')),

  -- 기록과 기록 시각은 함께 있거나 함께 없어야 한다.
  -- 한쪽만 있으면 지도 배너가 영영 안 사라지거나, 학습이 시간을 못 쓴다.
  constraint visits_recorded_with_result
    check (
      (result is null     and recorded_at is null)
      or
      (result is not null and recorded_at is not null)
    ),

  -- 앱이 오타(crowded 등)를 보내면 학습이 조용히 무시하게 된다
  constraint visits_hard_factors_valid
    check (
      hard_factors is null
      or hard_factors <@ array['announcement','speech','music','machine','sudden','crowd']::text[]
    ),

  constraint visits_factors_only_when_hard
    check (result = 'hard' or hard_factors is null)
);

create index visits_user_planned_idx on visits (user_id, planned_at desc);
create index visits_place_idx        on visits (place_id);

comment on table  visits is '방문 예정과 다녀온 뒤 기록. 자동 학습이 이 테이블을 재료로 쓴다';
comment on column visits.planned_at   is '방문 예정 시각. 지도 배너에 "15시 방문 예정"으로 표시';
comment on column visits.result       is 'ok 괜찮았어요 / hard 힘들었어요. 아직 기록 전이면 비움';
comment on column visits.hard_factors is '힘들었던 자극. 건너뛰기 가능';
comment on column visits.recorded_at  is '기록한 시각';

alter table visits enable row level security;

create policy "visits 는 자기 것만 읽기"
  on visits for select to authenticated
  using (user_id = auth.uid());

create policy "visits 는 자기 것만 만들기"
  on visits for insert to authenticated
  with check (user_id = auth.uid());

create policy "visits 는 자기 것만 고치기"
  on visits for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- 방문 예정 취소를 위해 삭제를 허용한다 (이 테이블만)
create policy "visits 는 자기 것만 지우기"
  on visits for delete to authenticated
  using (user_id = auth.uid());
