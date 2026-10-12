"""batch 공통 — 설정값 읽기와 Supabase 읽고 쓰기.

collect_congestion.py / calculate_score.py / forecast.py 가 함께 쓴다.
키는 .env 에서 읽는다 (batch/.env.example 참고). 코드에 키를 적지 않는다.

Supabase 공식 파이썬 라이브러리 대신 requests 로 REST API 를 직접 부른다.
  - 라이브러리는 버전마다 쓰는 방법이 바뀌어, 팀원이 받았을 때 깨질 수 있다
  - 설치할 것이 적어 GitHub Actions 실행이 빨리 끝난다
  - 주소와 조건이 코드에 그대로 보여서 처음 보는 사람도 무슨 요청인지 안다
"""

import os
import sys
from pathlib import Path

import requests
from dotenv import load_dotenv

# batch/.env 를 읽어 os.environ 에 넣는다.
#
# 경로를 직접 지정하는 이유: load_dotenv() 를 인자 없이 부르면 '현재 폴더'에서
# .env 를 찾는다. 저장소 루트에서 `python batch/collect_congestion.py` 로 돌리면
# 루트에는 .env 가 없어서 키가 비는데, 오류가 "URL 이 잘못됐다"로 나와
# 원인을 찾기 어렵다. 어디서 실행해도 같은 파일을 읽게 한다.
#
# GitHub Actions 에는 .env 가 없고 Secrets 가 환경변수로 들어온다.
# load_dotenv 는 파일이 없으면 조용히 넘어가므로 양쪽에서 같은 코드가 돌아간다.
load_dotenv(Path(__file__).with_name(".env"))

SEOUL_API_KEY = os.getenv("SEOUL_API_KEY")
SUPABASE_URL = (os.getenv("SUPABASE_URL") or "").rstrip("/")
SUPABASE_SERVICE_KEY = os.getenv("SUPABASE_SERVICE_KEY")

# 서울시 API 는 한국 시각을 시간대 표시 없이 준다. 저장할 때 붙여야 한다.
KST = "+09:00"


def require_env():
    """키가 비어 있으면 바로 멈춘다.

    없이 진행하면 Supabase 가 401 을 돌려주는데, "키가 없다"인지
    "권한이 없다"인지 구분이 안 되어 원인을 찾느라 시간을 버린다.
    """
    missing = [
        name
        for name, value in (
            ("SEOUL_API_KEY", SEOUL_API_KEY),
            ("SUPABASE_URL", SUPABASE_URL),
            ("SUPABASE_SERVICE_KEY", SUPABASE_SERVICE_KEY),
        )
        if not value
    ]
    if missing:
        print("[중단] 다음 값이 비어 있습니다: " + ", ".join(missing))
        print("       batch/.env.example 을 복사해 batch/.env 를 만들고 값을 넣으세요.")
        sys.exit(1)


def _headers(extra=None):
    """Supabase 요청에 늘 붙는 머리말.

    apikey 와 Authorization 에 같은 키를 넣는다. 둘 중 하나만 보내면
    Supabase 가 요청을 거부한다.
    """
    headers = {
        "apikey": SUPABASE_SERVICE_KEY,
        "Authorization": f"Bearer {SUPABASE_SERVICE_KEY}",
        "Content-Type": "application/json",
    }
    if extra:
        headers.update(extra)
    return headers


def sb_select(table, params=None):
    """테이블을 읽는다. params 는 PostgREST 질의 조건."""
    response = requests.get(
        f"{SUPABASE_URL}/rest/v1/{table}",
        headers=_headers(),
        params=params or {},
        timeout=30,
    )
    response.raise_for_status()
    return response.json()


def sb_upsert(table, rows, on_conflict):
    """여러 줄을 한 번에 넣는다. on_conflict 칸이 겹치면 덮어쓴다.

    merge-duplicates 가 없으면 이미 있는 줄 하나 때문에 보낸 줄 전체가 취소된다.
    한 요청은 전부 저장되거나 전부 안 되기 때문이다.
    """
    if not rows:
        return 0
    response = requests.post(
        f"{SUPABASE_URL}/rest/v1/{table}",
        headers=_headers({"Prefer": "resolution=merge-duplicates,return=minimal"}),
        params={"on_conflict": on_conflict},
        json=rows,
        timeout=60,
    )
    if response.status_code >= 300:
        raise RuntimeError(
            f"{table} 저장 실패 {response.status_code}: {response.text[:300]}"
        )
    return len(rows)


def sb_count(table, params=None):
    """줄 수만 센다. 내용을 받아오지 않아 빠르다."""
    merged = dict(params or {})
    merged.setdefault("select", "*")
    response = requests.get(
        f"{SUPABASE_URL}/rest/v1/{table}",
        headers=_headers({"Prefer": "count=exact", "Range": "0-0"}),
        params=merged,
        timeout=30,
    )
    response.raise_for_status()
    # content-range 는 "0-0/121" 형태로 온다. 슬래시 뒤가 전체 개수다.
    return int(response.headers.get("content-range", "0-0/0").split("/")[-1])


def get_settings():
    """settings 테이블을 {키: 값} 사전으로 읽는다.

    기준선과 가중치를 코드에 적지 않는 이유: 값을 바꿀 때마다 앱을 다시
    빌드하지 않기 위해서다 (docs/08_DB_설계.md).
    """
    rows = sb_select("settings", {"select": "key,value"})
    return {row["key"]: row["value"] for row in rows}
