"""혼잡도 수집 — 1시간마다.

서울시 실시간 도시데이터 API에서 지역별 현재 혼잡도와 향후 12시간 예측값을 받아
DB '혼잡도' 표에 쌓는다. 혼잡도는 가게가 아니라 지역 단위다.

확인 필요: API 응답 형식과 호출 제한은 키 발급 후 확인 (CLAUDE.md).
"""

from common import SEOUL_API_KEY, get_client


def main():
    # TODO 1. '장소' 표에서 혼잡도 지역 목록을 모은다 (중복 제거)
    # TODO 2. 지역마다 서울시 실시간 도시데이터 API 호출
    # TODO 3. 현재 혼잡도와 향후 12시간 예측값을 '혼잡도' 표에 저장
    #         (지역 · 기준 시각 · 대상 시각 · 혼잡도 · 실측/예측 구분)
    raise NotImplementedError("서울시 API 키 발급 후 작성")


if __name__ == "__main__":
    main()
