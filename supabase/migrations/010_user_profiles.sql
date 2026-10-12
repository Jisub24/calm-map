-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 10. user_profiles — 보호자 민감 요인과 가중치
--     '우리 아이 맞춤'의 재료. 계정 하나당 한 줄
--     먼저 필요: 없음 (auth.users 는 Supabase 가 이미 만들어 둠)
--
--     아이 이름 · 나이 · 진단명 칸은 없다 (CLAUDE.md 원칙 2).
--     이메일과 비밀번호도 여기 없다. Supabase Auth 가 따로 관리한다.

create table user_profiles (
  user_id             uuid primary key references auth.users(id) on delete cascade,
  sensitivities       jsonb,
  weight_announcement real not null default 1.0,
  weight_speech       real not null default 1.0,
  weight_music        real not null default 1.0,
  weight_machine      real not null default 1.0,
  weight_sudden       real not null default 1.0,
  weight_crowd        real not null default 1.0,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),

  -- 자동 학습이 폭주해도 최대 3배까지만 움직이게 막는다.
  -- 하한 0.5 는 "안 고른 자극의 영향을 0 으로 만들지 않기" 위해서다.
  constraint user_profiles_weight_range
    check (
      weight_announcement between 0.5 and 3 and
      weight_speech       between 0.5 and 3 and
      weight_music        between 0.5 and 3 and
      weight_machine      between 0.5 and 3 and
      weight_sudden       between 0.5 and 3 and
      weight_crowd        between 0.5 and 3
    )
);

comment on table  user_profiles is '보호자 민감 요인과 가중치. 아이 이름 · 나이 · 진단명 칸은 없다';
comment on column user_profiles.sensitivities       is '보호자가 등록한 원본. [{"type":"announcement","rank":1}, …]';
comment on column user_profiles.weight_announcement is '안내방송 가중치. hourly_forecast.score_announcement 에 곱한다';
comment on column user_profiles.weight_crowd        is '혼잡 가중치';
comment on column user_profiles.updated_at          is '마지막으로 고친 시각';

alter table user_profiles enable row level security;

create policy "user_profiles 는 자기 것만 읽기"
  on user_profiles for select to authenticated
  using (user_id = auth.uid());

create policy "user_profiles 는 자기 것만 만들기"
  on user_profiles for insert to authenticated
  with check (user_id = auth.uid());

-- using 과 with check 가 둘 다 필요하다.
--   using      : 고치기 전 — 이 줄을 건드릴 자격이 있나
--   with check : 고친 뒤   — 결과가 규칙에 맞나
-- with check 가 없으면 자기 프로필의 user_id 를 남의 번호로 바꿔
-- 남의 맞춤 설정을 덮어쓸 수 있다.
create policy "user_profiles 는 자기 것만 고치기"
  on user_profiles for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- delete 정책 없음. 다시 등록하고 싶으면 고치면 되고,
-- 탈퇴하면 on delete cascade 로 자동으로 사라진다.
