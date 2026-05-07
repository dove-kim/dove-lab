import type { IndicatorType } from "@/types/filter";

export type PanelId =
  | "OVERLAY"
  | "RSI"
  | "MACD"
  | "STOCH"
  | "ADX"
  | "OSCILLATOR"
  | "VOLUME_IND"
  | "OBV"
  | "VOLATILITY"
  | "BB_MISC";

export interface IndicatorMeta {
  panel: PanelId;
  color: string;
  label: string;
}

export const INDICATOR_META: Record<IndicatorType, IndicatorMeta> = {
  SMA_5:   { panel: "OVERLAY", color: "#fbbf24", label: "SMA 5" },
  SMA_10:  { panel: "OVERLAY", color: "#fb923c", label: "SMA 10" },
  SMA_20:  { panel: "OVERLAY", color: "#f87171", label: "SMA 20" },
  SMA_50:  { panel: "OVERLAY", color: "#c084fc", label: "SMA 50" },
  SMA_60:  { panel: "OVERLAY", color: "#818cf8", label: "SMA 60" },
  SMA_120: { panel: "OVERLAY", color: "#60a5fa", label: "SMA 120" },
  SMA_200: { panel: "OVERLAY", color: "#38bdf8", label: "SMA 200" },
  EMA_5:   { panel: "OVERLAY", color: "#a3e635", label: "EMA 5" },
  EMA_10:  { panel: "OVERLAY", color: "#4ade80", label: "EMA 10" },
  EMA_20:  { panel: "OVERLAY", color: "#34d399", label: "EMA 20" },
  EMA_60:  { panel: "OVERLAY", color: "#2dd4bf", label: "EMA 60" },
  EMA_120: { panel: "OVERLAY", color: "#22d3ee", label: "EMA 120" },
  EMA_200: { panel: "OVERLAY", color: "#a78bfa", label: "EMA 200" },
  BB_UPPER_20:  { panel: "OVERLAY", color: "#fb923c", label: "BB상단" },
  BB_MIDDLE_20: { panel: "OVERLAY", color: "#fbbf24", label: "BB중간" },
  BB_LOWER_20:  { panel: "OVERLAY", color: "#fb923c", label: "BB하단" },

  RSI_9:  { panel: "RSI", color: "#f472b6", label: "RSI 9" },
  RSI_14: { panel: "RSI", color: "#a78bfa", label: "RSI 14" },
  RSI_21: { panel: "RSI", color: "#60a5fa", label: "RSI 21" },

  MACD_LINE:      { panel: "MACD", color: "#60a5fa", label: "MACD" },
  MACD_SIGNAL:    { panel: "MACD", color: "#f87171", label: "시그널" },
  MACD_HISTOGRAM: { panel: "MACD", color: "#6b7280", label: "히스토그램" },

  STOCHASTIC_K_14_7: { panel: "STOCH", color: "#60a5fa", label: "%K" },
  STOCHASTIC_D_14_7: { panel: "STOCH", color: "#f87171", label: "%D" },

  ADX_14:      { panel: "ADX", color: "#fbbf24", label: "ADX" },
  PLUS_DI_14:  { panel: "ADX", color: "#4ade80", label: "+DI" },
  MINUS_DI_14: { panel: "ADX", color: "#f87171", label: "-DI" },

  MFI:       { panel: "OSCILLATOR", color: "#818cf8", label: "MFI" },
  CCI:       { panel: "OSCILLATOR", color: "#34d399", label: "CCI" },
  WILLIAMS_R:{ panel: "OSCILLATOR", color: "#fb923c", label: "W%R" },

  VOLUME_RATIO_20:  { panel: "VOLUME_IND", color: "#60a5fa", label: "거래량비율" },
  VOLUME_MA20_RATIO:{ panel: "VOLUME_IND", color: "#4ade80", label: "거래량/MA20" },
  OBV: { panel: "OBV", color: "#a78bfa", label: "OBV" },

  ATR: { panel: "VOLATILITY", color: "#f87171", label: "ATR" },
  VOLATILITY_5D:  { panel: "VOLATILITY", color: "#fbbf24", label: "변동성5일" },
  VOLATILITY_20D: { panel: "VOLATILITY", color: "#fb923c", label: "변동성20일" },

  BB_PERCENT_B_20: { panel: "BB_MISC", color: "#fb923c", label: "%B" },
  BB_WIDTH_20:     { panel: "BB_MISC", color: "#fbbf24", label: "밴드폭" },

  HIGH_20D_RATIO: { panel: "OVERLAY", color: "#a3e635", label: "20일고점비" },
  HIGH_52W_RATIO: { panel: "OVERLAY", color: "#4ade80", label: "52주고점비" },

  GAP_OPEN:   { panel: "OVERLAY", color: "#fbbf24", label: "갭상승" },
  IS_52W_HIGH:{ panel: "OVERLAY", color: "#4ade80", label: "52주신고가" },
  IS_52W_LOW: { panel: "OVERLAY", color: "#f87171", label: "52주신저가" },
  IS_20D_HIGH:{ panel: "OVERLAY", color: "#a3e635", label: "20일신고가" },
  IS_20D_LOW: { panel: "OVERLAY", color: "#fb923c", label: "20일신저가" },
};

export const PANEL_LABELS: Record<PanelId, string> = {
  OVERLAY:    "가격 차트 오버레이",
  RSI:        "RSI",
  MACD:       "MACD",
  STOCH:      "스토캐스틱",
  ADX:        "ADX / DI",
  OSCILLATOR: "오실레이터",
  VOLUME_IND: "거래량 지표",
  OBV:        "OBV",
  VOLATILITY: "변동성",
  BB_MISC:    "볼린저 %B / 폭",
};

export const SELECTOR_GROUPS: { label: string; types: IndicatorType[]; bundled?: boolean }[] = [
  { label: "단순이평(SMA)",   types: ["SMA_5","SMA_10","SMA_20","SMA_50","SMA_60","SMA_120","SMA_200"] },
  { label: "지수이평(EMA)",   types: ["EMA_5","EMA_10","EMA_20","EMA_60","EMA_120","EMA_200"] },
  { label: "볼린저밴드",      types: ["BB_UPPER_20","BB_MIDDLE_20","BB_LOWER_20"], bundled: true },
  { label: "볼린저 %B/폭",    types: ["BB_PERCENT_B_20","BB_WIDTH_20"] },
  { label: "RSI",             types: ["RSI_9","RSI_14","RSI_21"] },
  { label: "MACD",            types: ["MACD_LINE","MACD_SIGNAL","MACD_HISTOGRAM"], bundled: true },
  { label: "스토캐스틱",      types: ["STOCHASTIC_K_14_7","STOCHASTIC_D_14_7"], bundled: true },
  { label: "ADX / DI",        types: ["ADX_14","PLUS_DI_14","MINUS_DI_14"], bundled: true },
  { label: "오실레이터",      types: ["MFI","CCI","WILLIAMS_R"] },
  { label: "거래량 지표",     types: ["VOLUME_RATIO_20","VOLUME_MA20_RATIO","OBV"] },
  { label: "변동성",          types: ["ATR","VOLATILITY_5D","VOLATILITY_20D"] },
];
