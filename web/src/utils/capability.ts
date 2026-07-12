/** capability 코드 → 한국어 라벨 (표시 전용, 권한 강제는 서버). */
export const CAPABILITY_LABELS: Record<string, string> = {
  STOCK_VIEW: "종목 조회",
  STOCK_SEARCH: "검색·필터",
  INDICATOR_ML: "ML 예측 지표",
  MODEL_SCORE: "모델 점수",
  CUSTOM_INDICATOR: "커스텀 지표",
};

/** 관리 화면에서 부여 토글할 수 있는 전체 capability 목록(선언 순서). */
export const ALL_CAPABILITIES = ["STOCK_VIEW", "STOCK_SEARCH", "INDICATOR_ML", "MODEL_SCORE", "CUSTOM_INDICATOR"];

export function hasCapability(caps: string[] | undefined, cap: string): boolean {
  return !!caps && caps.includes(cap);
}
