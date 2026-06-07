export type SystemEventType = "KRX_API_FAILURE" | "KIS_API_FAILURE" | "KRX_RATE_LIMIT_EXCEEDED";

export type MarketType = "KOSPI" | "KOSDAQ";

export interface SystemEvent {
  id: number;
  eventType: SystemEventType;
  marketType: MarketType | null;
  occurredAt: string;
  detail: Record<string, string>;
}

export interface SystemEventPage {
  content: SystemEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
