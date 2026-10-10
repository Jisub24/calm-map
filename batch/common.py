"""batch 공통 — Supabase 연결과 설정값 읽기.

collect_congestion.py / calculate_score.py / forecast.py 가 함께 쓴다.
키는 .env 에서 읽는다 (batch/.env.example 참고). 코드에 키를 적지 않는다.
"""

import os

from dotenv import load_dotenv

load_dotenv()

SEOUL_API_KEY = os.getenv("SEOUL_API_KEY")
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_SERVICE_KEY = os.getenv("SUPABASE_SERVICE_KEY")


def get_client():
    """Supabase 연결을 만든다."""
    # TODO: Supabase 프로젝트를 만든 뒤 create_client(SUPABASE_URL, SUPABASE_SERVICE_KEY)
    raise NotImplementedError("Supabase 프로젝트 생성 후 작성")


def get_settings():
    """DB '설정값' 표에서 단계 기준선 · 정보 부족 기준(3건) · 순위별 가중치를 읽는다.

    값을 코드에 적지 않고 표에서 읽는 이유: 기준을 바꿀 때마다 앱을 다시 빌드하지
    않기 위해서다 (docs/04_구현_할일_데이터.md).
    """
    # TODO: 표의 칸 이름이 확정되면 작성
    raise NotImplementedError("DB 표 칸 이름 확정 후 작성")
