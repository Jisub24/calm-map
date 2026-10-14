-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/12_답변서_반영.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 14. measurement_requests — 측정 요청
--     먼저 필요: 002_places
--
--     답변서 질의4에서 나온 기능이다.
--       "'측정 요청' 버튼 : 요청이 많은 장소부터 관측 확보
--        (원음 저장·전송 없음). 한 번의 관측으로 대표값을 확정하지 않음"
--
--     소리·빛 정보가 없는 장소는 종합 지수를 보여줄 수 없다.
--     그런 장소에 "정보 부족"만 띄우고 끝내면 사용자가 할 수 있는 것이 없다.
--     요청을 받아서, 어디부터 측정하러 갈지 정하는 데 쓴다.

create table measurement_requests (
  id         bigint generated always as identity primary key,
  place_id   bigint not null references places(id) on delete cascade,
  user_id    uuid   not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),

  -- 한 사람이 한 장소에 한 번만.
  -- 안 막으면 한 명이 버튼을 연타해서 순위를 끌어올릴 수 있고,
  -- 그러면 "요청이 많은 장소부터" 라는 기준 자체가 무의미해진다.
  constraint measurement_requests_one_per_user
    unique (place_id, user_id)
);

-- "요청이 많은 장소" 를 뽑는 것이 이 테이블의 유일한 용도다
create index measurement_requests_place_idx on measurement_requests (place_id);

comment on table  measurement_requests is
  '측정 요청. 요청이 많은 장소부터 관측을 확보한다 (답변서 질의4)';
comment on column measurement_requests.user_id is
  '누가 요청했나. 한 사람이 같은 장소를 여러 번 누르는 것을 막는 용도';
comment on column measurement_requests.created_at is
  '요청 시각. 오래된 요청의 비중을 낮출 때 쓸 수 있다';

alter table measurement_requests enable row level security;

-- 읽기: 로그인한 사람은 누구나.
-- 요청 수를 화면에 "측정 요청 12명" 처럼 보여줘야 해서 남의 요청도 세어야 한다.
-- 다만 user_id 가 함께 보이므로, 앱은 개수만 쓰고 명단을 보여주지 않는다.
create policy "measurement_requests 는 로그인하면 읽기"
  on measurement_requests for select
  to authenticated
  using (true);

-- 넣기: 자기 이름으로만. 남의 이름으로 요청을 꾸며 넣는 것을 막는다
create policy "measurement_requests 는 자기 것만 넣기"
  on measurement_requests for insert
  to authenticated
  with check (user_id = auth.uid());

-- 지우기: 자기 것만. 요청을 취소할 수 있어야 한다
create policy "measurement_requests 는 자기 것만 지우기"
  on measurement_requests for delete
  to authenticated
  using (user_id = auth.uid());

-- update 정책은 만들지 않는다. 고칠 내용이 없다 (장소와 사람이 전부다).
