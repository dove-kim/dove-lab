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
 * @param signalThreshold 진입 문턱 — 이 값 이상은 초록 세모 (0~1)
 * @param watchThreshold  관찰 문턱 — 이 값 이상 진입 문턱 미만은 회색 세모 (null이면 관찰 미표시)
 * @param seriesMetricIds 하단 서브패널로 표시할 커스텀 지표 ID 목록 (빈 배열이면 미표시)
 */
export interface ChartOverlayConfig {
  signalModelId: number | null;
  signalThreshold: number;
  watchThreshold: number | null;
  seriesMetricIds: number[];
}

export const DEFAULT_CHART_OVERLAY: ChartOverlayConfig = {
  signalModelId: null,
  signalThreshold: 0.9,
  watchThreshold: null,
  seriesMetricIds: [],
};
