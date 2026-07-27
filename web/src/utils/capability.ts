/** capability 코드 → 한국어 라벨 (표시 전용, 권한 강제는 서버). */
export const CAPABILITY_LABELS: Record<string, string> = {
  STOCK_VIEW: "종목 조회",
  STOCK_SEARCH: "검색·필터",
  MODEL_SCORE: "모델 점수",
  CUSTOM_INDICATOR: "커스텀 지표",
  PORTFOLIO_LEDGER: "포트폴리오(장부)",
  PORTFOLIO_REBALANCE: "매매 계획",
};

/** 관리 화면에서 부여 토글할 수 있는 전체 capability 목록(선언 순서). */
export const ALL_CAPABILITIES = ["STOCK_VIEW", "STOCK_SEARCH", "MODEL_SCORE", "CUSTOM_INDICATOR", "PORTFOLIO_LEDGER", "PORTFOLIO_REBALANCE"];

export function hasCapability(caps: string[] | undefined, cap: string): boolean {
  return !!caps && caps.includes(cap);
}
