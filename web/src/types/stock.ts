export type MarketType = "KOSPI" | "KOSDAQ" | "KONEX";

export interface Stock {
  code: string;
  isinCode: string | null;
  name: string;
  marketType: MarketType;
  listingDate: string;
}
