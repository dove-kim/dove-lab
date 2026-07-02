export type LogicOperator = "AND" | "OR";
export type CompareOp = "GT" | "GTE" | "LT" | "LTE" | "EQ" | "NEQ";
export type PriceField = "OPEN" | "HIGH" | "LOW" | "CLOSE";
export type MarketTypeFilter = "KOSPI" | "KOSDAQ" | "KONEX";
export type PriceTypeFilter = "RAW" | "ADJUSTED";

export const PRICE_TYPE_LABELS: Record<PriceTypeFilter, string> = {
  RAW: "비수정",
  ADJUSTED: "수정",
};

export type VenueFilter = "KRX" | "NXT" | "INTEGRATED";

export const VENUE_LABELS: Record<VenueFilter, string> = {
  KRX: "KRX",
  NXT: "NXT",
  INTEGRATED: "통합",
};

export type IndicatorType =
  | "SMA_5" | "SMA_10" | "SMA_20" | "SMA_50" | "SMA_60" | "SMA_120" | "SMA_200"
  | "EMA_5" | "EMA_10" | "EMA_20" | "EMA_60" | "EMA_120" | "EMA_200"
  | "RSI_9" | "RSI_14" | "RSI_21"
  | "MACD_LINE" | "MACD_SIGNAL" | "MACD_HISTOGRAM"
  | "STOCHASTIC_K_14_7" | "STOCHASTIC_D_14_7"
  | "ADX_14" | "PLUS_DI_14" | "MINUS_DI_14"
  | "VOLUME_RATIO_20" | "OBV" | "VOLUME_MA20_RATIO"
  | "BB_UPPER_20" | "BB_MIDDLE_20" | "BB_LOWER_20" | "BB_PERCENT_B_20" | "BB_WIDTH_20"
  | "ATR" | "MFI" | "CCI" | "WILLIAMS_R"
  | "VOLATILITY_5D" | "VOLATILITY_20D"
  | "HIGH_20D_RATIO" | "HIGH_52W_RATIO"
  | "GAP_OPEN" | "IS_52W_HIGH" | "IS_52W_LOW" | "IS_20D_HIGH" | "IS_20D_LOW";

export type ConditionType =
  | "INDICATOR_VALUE"
  | "INDICATOR_RANGE"
  | "INDICATOR_CROSS"
  | "PRICE_VALUE"
  | "PRICE_RANGE"
  | "PRICE_VS_INDICATOR"
  | "VOLUME_VALUE"
  | "VOLUME_RANGE"
  | "MARKET_FILTER"
  | "MODEL_SCORE_VALUE"
  | "MODEL_SCORE_RANGE"
  | "RANK_VALUE"
  | "RANK_RANGE"
  | "BREADTH_VALUE"
  | "BREADTH_RANGE"
  | "STOCK_STATUS";

export type StockStatusExclude = "TRADING_HALT" | "ADMIN_ITEM";

export const STOCK_STATUS_LABELS: Record<StockStatusExclude, string> = {
  TRADING_HALT: "거래정지",
  ADMIN_ITEM: "관리종목",
};

export type RankType =
  | "RANK_RET_1D" | "RANK_RET_5D" | "RANK_RET_10D"
  | "RANK_VOLUME_RATIO_20" | "RANK_RSI_14" | "RANK_MACD_HISTOGRAM"
  | "RANK_HIGH_52W_RATIO" | "RANK_VOLATILITY_20D" | "RANK_TURNOVER";

/**
 * 활성 모델 요약(필터 조건에서 모델 선택용).
 *
 * @param id         모델 식별자
 * @param name       모델 이름
 * @param version    버전
 * @param outputType 출력 의미(PROBABILITY/REGRESSION)
 */
export interface ModelSummary {
  id: number;
  name: string;
  version: string;
  outputType: string;
}

export interface GroupNode {
  id: string;
  nodeType: "GROUP";
  negated: boolean;
  children: ExpressionNode[];
  childOps: LogicOperator[];
}

interface BaseCondition {
  id: string;
  nodeType: "CONDITION";
  negated: boolean;
}

// 거래일 오프셋: 0=기준일, 양수=미래, 음수=과거. 미지정 시 0.
export interface IndicatorValueCondition extends BaseCondition {
  conditionType: "INDICATOR_VALUE";
  indicator: IndicatorType;
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface IndicatorRangeCondition extends BaseCondition {
  conditionType: "INDICATOR_RANGE";
  indicator: IndicatorType;
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface IndicatorCrossCondition extends BaseCondition {
  conditionType: "INDICATOR_CROSS";
  leftIndicator: IndicatorType;
  leftOffset?: number;
  operator: CompareOp;
  rightIndicator: IndicatorType;
  rightOffset?: number;
}

export interface PriceValueCondition extends BaseCondition {
  conditionType: "PRICE_VALUE";
  priceField: PriceField;
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface PriceRangeCondition extends BaseCondition {
  conditionType: "PRICE_RANGE";
  priceField: PriceField;
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface VolumeValueCondition extends BaseCondition {
  conditionType: "VOLUME_VALUE";
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface VolumeRangeCondition extends BaseCondition {
  conditionType: "VOLUME_RANGE";
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface PriceVsIndicatorCondition extends BaseCondition {
  conditionType: "PRICE_VS_INDICATOR";
  priceField: PriceField;
  leftOffset?: number;
  operator: CompareOp;
  indicator: IndicatorType;
  rightOffset?: number;
}

export interface MarketFilterCondition extends BaseCondition {
  conditionType: "MARKET_FILTER";
  markets: MarketTypeFilter[];
}

export interface ModelScoreValueCondition extends BaseCondition {
  conditionType: "MODEL_SCORE_VALUE";
  modelId: number;
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface ModelScoreRangeCondition extends BaseCondition {
  conditionType: "MODEL_SCORE_RANGE";
  modelId: number;
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface RankValueCondition extends BaseCondition {
  conditionType: "RANK_VALUE";
  rank: RankType;
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface RankRangeCondition extends BaseCondition {
  conditionType: "RANK_RANGE";
  rank: RankType;
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface BreadthValueCondition extends BaseCondition {
  conditionType: "BREADTH_VALUE";
  offset?: number;
  operator: CompareOp;
  value: number;
}

export interface BreadthRangeCondition extends BaseCondition {
  conditionType: "BREADTH_RANGE";
  offset?: number;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

/**
 * 종목상태 제외 조건. 지정한 상태의 종목을 결과에서 제외한다.
 * 종목상태는 현재값이라 최신일자에서만 유효 — DateRule≠LATEST면 백엔드가 무시한다.
 */
export interface StockStatusCondition extends BaseCondition {
  conditionType: "STOCK_STATUS";
  exclude: StockStatusExclude[];
}

export type ConditionNode =
  | IndicatorValueCondition
  | IndicatorRangeCondition
  | IndicatorCrossCondition
  | PriceValueCondition
  | PriceRangeCondition
  | PriceVsIndicatorCondition
  | VolumeValueCondition
  | VolumeRangeCondition
  | MarketFilterCondition
  | ModelScoreValueCondition
  | ModelScoreRangeCondition
  | RankValueCondition
  | RankRangeCondition
  | BreadthValueCondition
  | BreadthRangeCondition
  | StockStatusCondition;

export type ExpressionNode = GroupNode | ConditionNode;

export type DateRule = "LATEST" | "SPECIFIC_DATE" | "PREV_1D" | "PREV_3D" | "PREV_5D" | "PREV_10D";

export const DATE_RULE_LABELS: Record<DateRule, string> = {
  LATEST: "최신 날짜 자동",
  SPECIFIC_DATE: "날짜 직접 지정",
  PREV_1D: "기준일 전 1 거래일",
  PREV_3D: "기준일 전 3 거래일",
  PREV_5D: "기준일 전 5 거래일",
  PREV_10D: "기준일 전 10 거래일",
};

export interface SearchFilter {
  id: number;
  name: string;
  dateRule: DateRule;
  markets: string[];
  priceType: PriceTypeFilter;
  exchange: VenueFilter;
  expression: string;
  stockFilterId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface StockMatchResult {
  code: string;
  name: string;
  marketType: string;
  closePrice: number | null;
  volume: number | null;
  tradingHalt?: boolean;
  adminItem?: boolean;
}

export interface ExecuteFilterResponse {
  filterId: number;
  filterName: string;
  evaluationDate: string;
  dateRule: string;
  markets: string[];
  totalCandidates: number;
  matchCount: number;
  results: StockMatchResult[];
}

// ─── Display helpers ────────────────────────────────────────────────────────

export const INDICATOR_LABELS: Record<IndicatorType, string> = {
  SMA_5: "단순이평(5)", SMA_10: "단순이평(10)", SMA_20: "단순이평(20)",
  SMA_50: "단순이평(50)", SMA_60: "단순이평(60)", SMA_120: "단순이평(120)", SMA_200: "단순이평(200)",
  EMA_5: "지수이평(5)", EMA_10: "지수이평(10)", EMA_20: "지수이평(20)",
  EMA_60: "지수이평(60)", EMA_120: "지수이평(120)", EMA_200: "지수이평(200)",
  RSI_9: "RSI(9)", RSI_14: "RSI(14)", RSI_21: "RSI(21)",
  MACD_LINE: "MACD선", MACD_SIGNAL: "MACD시그널", MACD_HISTOGRAM: "MACD히스토그램",
  STOCHASTIC_K_14_7: "스토캐스틱%K(14,7)", STOCHASTIC_D_14_7: "스토캐스틱%D(14,7)",
  ADX_14: "ADX(14)", PLUS_DI_14: "+DI(14)", MINUS_DI_14: "-DI(14)",
  VOLUME_RATIO_20: "거래량비율(20)", OBV: "OBV", VOLUME_MA20_RATIO: "거래량/20일평균",
  BB_UPPER_20: "볼린저상단(20)", BB_MIDDLE_20: "볼린저중간(20)", BB_LOWER_20: "볼린저하단(20)",
  BB_PERCENT_B_20: "%B(20)", BB_WIDTH_20: "밴드폭(20)",
  ATR: "ATR", MFI: "MFI", CCI: "CCI", WILLIAMS_R: "윌리엄스%R",
  VOLATILITY_5D: "변동성(5일)", VOLATILITY_20D: "변동성(20일)",
  HIGH_20D_RATIO: "20일고점비율", HIGH_52W_RATIO: "52주고점비율",
  GAP_OPEN: "갭상승", IS_52W_HIGH: "52주신고가", IS_52W_LOW: "52주신저가",
  IS_20D_HIGH: "20일신고가", IS_20D_LOW: "20일신저가",
};

export const PRICE_FIELD_LABELS: Record<PriceField, string> = {
  OPEN: "시가", HIGH: "고가", LOW: "저가", CLOSE: "종가",
};

/** 횡단면 순위(0~1, 1=최상위) 종류 라벨. */
export const RANK_TYPE_LABELS: Record<RankType, string> = {
  RANK_RET_1D: "1일 수익률 순위",
  RANK_RET_5D: "5일 수익률 순위",
  RANK_RET_10D: "10일 수익률 순위",
  RANK_VOLUME_RATIO_20: "거래량비율(20) 순위",
  RANK_RSI_14: "RSI(14) 순위",
  RANK_MACD_HISTOGRAM: "MACD히스토그램 순위",
  RANK_HIGH_52W_RATIO: "52주고점비율 순위",
  RANK_VOLATILITY_20D: "변동성(20일) 순위",
  RANK_TURNOVER: "거래대금 순위",
};

export const ALL_RANK_TYPES: RankType[] = [
  "RANK_TURNOVER", "RANK_RET_1D", "RANK_RET_5D", "RANK_RET_10D",
  "RANK_VOLUME_RATIO_20", "RANK_RSI_14", "RANK_MACD_HISTOGRAM",
  "RANK_HIGH_52W_RATIO", "RANK_VOLATILITY_20D",
];

export const COMPARE_OP_LABELS: Record<CompareOp, string> = {
  GT: ">", GTE: "≥", LT: "<", LTE: "≤", EQ: "=", NEQ: "≠",
};

export const INDICATOR_GROUPS: { label: string; types: IndicatorType[] }[] = [
  { label: "단순이평(SMA)", types: ["SMA_5","SMA_10","SMA_20","SMA_50","SMA_60","SMA_120","SMA_200"] },
  { label: "지수이평(EMA)", types: ["EMA_5","EMA_10","EMA_20","EMA_60","EMA_120","EMA_200"] },
  { label: "RSI", types: ["RSI_9","RSI_14","RSI_21"] },
  { label: "MACD", types: ["MACD_LINE","MACD_SIGNAL","MACD_HISTOGRAM"] },
  { label: "스토캐스틱", types: ["STOCHASTIC_K_14_7","STOCHASTIC_D_14_7"] },
  { label: "추세", types: ["ADX_14","PLUS_DI_14","MINUS_DI_14"] },
  { label: "볼린저밴드", types: ["BB_UPPER_20","BB_MIDDLE_20","BB_LOWER_20","BB_PERCENT_B_20","BB_WIDTH_20"] },
  { label: "거래량지표", types: ["VOLUME_RATIO_20","OBV","VOLUME_MA20_RATIO"] },
  { label: "변동성", types: ["ATR","VOLATILITY_5D","VOLATILITY_20D"] },
  { label: "모멘텀", types: ["MFI","CCI","WILLIAMS_R"] },
  { label: "고점비율/플래그", types: ["HIGH_20D_RATIO","HIGH_52W_RATIO","GAP_OPEN","IS_52W_HIGH","IS_52W_LOW","IS_20D_HIGH","IS_20D_LOW"] },
];

