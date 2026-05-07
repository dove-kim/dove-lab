export type LogicOperator = "AND" | "OR";
export type CompareOp = "GT" | "GTE" | "LT" | "LTE" | "EQ" | "NEQ";
export type PriceField = "OPEN" | "HIGH" | "LOW" | "CLOSE";
export type MarketTypeFilter = "KOSPI" | "KOSDAQ" | "KONEX";

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
  | "MARKET_FILTER";

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

export interface IndicatorValueCondition extends BaseCondition {
  conditionType: "INDICATOR_VALUE";
  indicator: IndicatorType;
  operator: CompareOp;
  value: number;
}

export interface IndicatorRangeCondition extends BaseCondition {
  conditionType: "INDICATOR_RANGE";
  indicator: IndicatorType;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface IndicatorCrossCondition extends BaseCondition {
  conditionType: "INDICATOR_CROSS";
  leftIndicator: IndicatorType;
  operator: CompareOp;
  rightIndicator: IndicatorType;
}

export interface PriceValueCondition extends BaseCondition {
  conditionType: "PRICE_VALUE";
  priceField: PriceField;
  operator: CompareOp;
  value: number;
}

export interface PriceRangeCondition extends BaseCondition {
  conditionType: "PRICE_RANGE";
  priceField: PriceField;
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface VolumeValueCondition extends BaseCondition {
  conditionType: "VOLUME_VALUE";
  operator: CompareOp;
  value: number;
}

export interface VolumeRangeCondition extends BaseCondition {
  conditionType: "VOLUME_RANGE";
  minValue: number;
  minInclusive: boolean;
  maxValue: number;
  maxInclusive: boolean;
}

export interface PriceVsIndicatorCondition extends BaseCondition {
  conditionType: "PRICE_VS_INDICATOR";
  priceField: PriceField;
  operator: CompareOp;
  indicator: IndicatorType;
}

export interface MarketFilterCondition extends BaseCondition {
  conditionType: "MARKET_FILTER";
  markets: MarketTypeFilter[];
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
  | MarketFilterCondition;

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

export interface StockSetSummary {
  id: number;
  name: string;
  codeCount: number;
}

export interface StockSet {
  id: number;
  name: string;
  codes: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SearchFilter {
  id: number;
  name: string;
  dateRule: DateRule;
  markets: string[];
  expression: string;
  includeStockSetId: number | null;
  excludeStockSetId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface StockMatchResult {
  code: string;
  name: string;
  marketType: string;
  closePrice: number | null;
  volume: number | null;
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

