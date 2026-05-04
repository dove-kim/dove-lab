export type MarketType = "KOSPI" | "KOSDAQ" | "KONEX";
export type TradingStatus = "ACTIVE" | "SUSPENDED" | "DELISTED";

export interface FilterState {
  query: string;
  market: MarketType | "ALL";
}

export interface Stock {
  code: string;
  name: string;
  marketType: MarketType;
  tradingStatus: TradingStatus;
}

