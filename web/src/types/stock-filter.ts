export type FilterMode = "INCLUDE" | "EXCLUDE";
export type FilterScope = "SYSTEM" | "MEMBER";

export interface TagCondition {
  field: string;
  value: string;
  mode: FilterMode;
}

export interface NumericCondition {
  field: string;
  min: number | null;
  max: number | null;
}

export interface StockCondition {
  marketType: string;
  stockCode: string;
  mode: FilterMode;
}

export type NameMatchType = "CONTAINS" | "STARTS_WITH" | "ENDS_WITH";

export interface NamePatternCondition {
  pattern: string;
  mode: FilterMode;
  matchType: NameMatchType;
}

export interface StockFilterResponse {
  id: number;
  scope: FilterScope;
  memberId: number | null;
  name: string;
  description: string | null;
  tagConditions: TagCondition[];
  numericConditions: NumericCondition[];
  stockConditions: StockCondition[];
  namePatternConditions: NamePatternCondition[];
  enabled: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string | null;
}

export interface StockSummary {
  marketType: string;
  code: string;
  name: string;
}
