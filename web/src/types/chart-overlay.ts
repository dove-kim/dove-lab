/**
 * 활성 모델 요약(차트 시그널 선택용).
 */
export interface ModelSummary {
  id: number;
  name: string;
  version: string;
  outputType: string;
}

/**
 * 커스텀 지표 요약(차트 SERIES 오버레이 선택용).
 */
export interface CustomMetricSummary {
  id: number;
  name: string;
  shape: string;
}

/**
 * 한 거래일의 모델 채점 점수. score 없음 = 미채점.
 */
export interface ModelScorePoint {
  date: string;
  score: number | null;
}

/**
 * 한 거래일의 커스텀 지표 값.
 */
export interface CustomMetricPoint {
  date: string;
  value: number;
}

/**
 * 차트 오버레이(모델 시그널·커스텀 지표) 세션 설정.
 *
 * @param signalModelId   시그널로 표시할 모델 ID (없으면 미표시)
 * @param signalThreshold 시그널 마커를 찍을 점수 임계값 (0~1)
 * @param seriesMetricIds 하단 서브패널로 표시할 커스텀 지표 ID 목록 (빈 배열이면 미표시)
 */
export interface ChartOverlayConfig {
  signalModelId: number | null;
  signalThreshold: number;
  seriesMetricIds: number[];
}

export const DEFAULT_CHART_OVERLAY: ChartOverlayConfig = {
  signalModelId: null,
  signalThreshold: 0.5,
  seriesMetricIds: [],
};
