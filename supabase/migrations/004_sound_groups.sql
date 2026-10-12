-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 4. sound_groups — YAMNet 라벨 → 소리 종류 5개 번역표
--    녹음 실험(analysis/01) 결과를 보고 우리가 채운다
--    먼저 필요: 없음

create table sound_groups (
  id           bigint generated always as identity primary key,
  yamnet_label text not null,
  sound_type   text not null,
  weight       real not null default 1.0,

  constraint sound_groups_type_valid
    check (sound_type in ('announcement', 'speech', 'music', 'machine', 'sudden')),

  -- 0 이면 "세지 않는다"는 뜻인데, 그럴 거면 줄을 넣지 않으면 된다
  constraint sound_groups_weight_range
    check (weight > 0 and weight <= 1),

  -- 같은 라벨을 같은 종류로 두 번 적으면 점수가 두 번 세어진다
  constraint sound_groups_no_duplicate
    unique (yamnet_label, sound_type)
);

create index sound_groups_label_idx on sound_groups (yamnet_label);

comment on table  sound_groups is 'YAMNet 원본 라벨을 소리 종류 5개로 바꾸는 번역표';
comment on column sound_groups.yamnet_label is 'YAMNet 원본 라벨 (Speech, Music, Alarm …)';
comment on column sound_groups.sound_type   is '소리 종류 5개 중 하나. 민감 요인 선택지와 1:1 대응';
comment on column sound_groups.weight       is '기여도 0~1. 한 라벨을 두 종류로 나눠 담을 때 쓴다';

alter table sound_groups enable row level security;

create policy "sound_groups 는 로그인한 사람만 읽기"
  on sound_groups
  for select
  to authenticated
  using (true);
