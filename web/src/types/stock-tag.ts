export type TagSource = "KRX" | "KIS";
export type TagFieldType = "CATEGORY" | "BOOLEAN";

export interface StockTagValueItem {
  id: number;
  value: string;
  label: string;
}

export interface StockTagFieldGroup {
  field: string;
  label: string;
  source: TagSource;
  type: TagFieldType;
  values: StockTagValueItem[];
}

export interface StockTagNumericField {
  field: string;
  label: string;
  source: TagSource;
}

export interface StockTagsResponse {
  tagFields: StockTagFieldGroup[];
  numericFields: StockTagNumericField[];
}
