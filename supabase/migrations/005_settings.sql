-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 5. settings — 기준선 · 가중치 등 설정값
--    앱과 batch 가 둘 다 읽는다. 값을 한 곳에만 두어 양쪽이 어긋나지 않게 한다
--    먼저 필요: 없음
--
--    value 는 줄마다 생김새가 다르다 (숫자 하나 / 객체 / 목록).
--    그래서 check 제약을 걸 수 없고, 읽는 쪽(batch · 앱)에서 검사한다.

create table settings (
  key         text  primary key,
  value       jsonb not null,
  description text,
  updated_at  timestamptz not null default now()
);

comment on table  settings is '기준선 · 가중치 등 설정값. 앱 재빌드 없이 DB 에서 바꾼다';
comment on column settings.key         is '설정 이름 (level_thresholds, min_measurements …)';
comment on column settings.value       is '값. 숫자 하나도, 짝을 이룬 값 여러 개도 들어간다';
comment on column settings.description is '한글 설명. 나중에 봐도 무슨 값인지 알아보려고';
comment on column settings.updated_at  is '마지막으로 고친 시각. 값을 고칠 때 같이 써준다';

alter table settings enable row level security;

create policy "settings 는 로그인한 사람만 읽기"
  on settings
  for select
  to authenticated
  using (true);
