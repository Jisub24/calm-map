-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 설계: docs/08_DB_설계.md
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 7. measurement_labels — 그 측정에서 들린 소리 종류
--    측정 1건에 5~8줄이 딸린다
--    먼저 필요: 006_measurements
--
--    이 테이블에는 user_id 칸이 없다. 소유자는 measurements 가 알고 있으므로
--    정책에서 한 다리 건너 확인한다 (exists).
--    user_id 를 여기에도 두면 두 값이 어긋날 수 있어서 일부러 두지 않는다.

create table measurement_labels (
  id             bigint generated always as identity primary key,
  measurement_id bigint not null references measurements(id) on delete cascade,
  yamnet_label   text   not null,
  ratio          real   not null,
  avg_confidence real   not null,

  -- 0 이면 "한 번도 안 들렸다"는 뜻이라 줄을 보내지 않으면 된다
  constraint measurement_labels_ratio_range
    check (ratio > 0 and ratio <= 1),

  constraint measurement_labels_confidence_range
    check (avg_confidence > 0 and avg_confidence <= 1),

  -- 앱의 집계 코드가 같은 라벨을 쪼개 보내면 점수가 부풀려진다
  constraint measurement_labels_no_duplicate
    unique (measurement_id, yamnet_label)
);

create index measurement_labels_measurement_idx on measurement_labels (measurement_id);
create index measurement_labels_label_idx       on measurement_labels (yamnet_label);

comment on table  measurement_labels is '측정 1건에 딸린 소리 종류. 줄로 나눠야 batch 의 평균 계산이 단순하다';
comment on column measurement_labels.yamnet_label   is 'YAMNet 원본 라벨. 묶은 결과가 아니라 원본을 저장한다';
comment on column measurement_labels.ratio          is '측정 구간 중 이 라벨이 상위에 뜬 비율 0~1';
comment on column measurement_labels.avg_confidence is '상위에 들었을 때 확신도의 평균 0~1';

alter table measurement_labels enable row level security;

-- 읽기: 자기 측정에 딸린 라벨만
create policy "measurement_labels 는 자기 측정 것만 읽기"
  on measurement_labels
  for select
  to authenticated
  using (
    exists (
      select 1 from measurements m
      where m.id = measurement_labels.measurement_id
        and m.user_id = auth.uid()
    )
  );

-- 넣기: 자기 측정에만 달 수 있음
create policy "measurement_labels 는 자기 측정에만 넣기"
  on measurement_labels
  for insert
  to authenticated
  with check (
    exists (
      select 1 from measurements m
      where m.id = measurement_labels.measurement_id
        and m.user_id = auth.uid()
    )
  );

-- update / delete 정책은 만들지 않는다. 측정과 같은 이유로 사실 기록이다.
