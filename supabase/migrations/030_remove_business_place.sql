-- =====================================================
-- 무던한 지도 (Calm Map)
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 테이블 사이에 참조가 있어 파일 번호 순서대로 실행해야 한다
-- =====================================================

-- 30. 사업장 웹 — 등록한 가게 지우기
--     먼저 필요: 029 (places.source '사업장등록')
--
--     내 가게 목록의 '빼기' 는 브라우저 목록에서만 빼서 places 줄이 남고 앱 지도 마커도 남았다.
--     places 를 지우면 측정 · 방문 · 측정 요청까지 cascade 로 같이 지워진다. 그래서 둘 다 맞을 때만 지운다.
--       - 사업장 웹으로 들어온 장소 (source '사업장등록'). 답사 장소 · 앱 검색 장소는 못 지운다
--       - 측정 · 방문 · 측정 요청이 없음. 보호자가 남긴 기록을 사업장이 지우지 못하게
--     지우면 영업시간 · 무던한 시간 · 점수 칸은 같이 지워진다.
--
--     돌려주는 값   true  지웠음 (이미 없던 장소도 true)
--                  false 조건이 안 맞아 남겨 둠


create or replace function remove_business_place(p_place_id bigint)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_source text;
begin
  -- 확인하는 사이에 측정이 들어오지 않게 장소 줄을 잡아 둔다
  select source into v_source from places where id = p_place_id for update;
  if not found then
    return true;
  end if;
  if v_source is distinct from '사업장등록'
     or exists (select 1 from measurements where place_id = p_place_id)
     or exists (select 1 from visits where place_id = p_place_id)
     or exists (select 1 from measurement_requests where place_id = p_place_id) then
    return false;
  end if;

  delete from places where id = p_place_id;
  return true;
end;
$$;

comment on function remove_business_place is
  '사업장 웹에서 등록한 가게를 지운다. 출처가 사업장등록이고 측정 · 방문 · 측정 요청이 없을 때만. 로그인 없이 부른다 (시연용)';

revoke all on function remove_business_place(bigint) from public;
grant execute on function remove_business_place(bigint) to anon, authenticated;
