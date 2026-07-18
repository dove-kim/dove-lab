/** 거래 유형. */
export type TxType = "BUY" | "SELL" | "DEPOSIT" | "WITHDRAW" | "DIVIDEND" | "INTEREST";

/**
 * 포트폴리오 거래 한 건 (표시용).
 *
 * @param id        거래 식별자
 * @param tradedAt  체결/발생 일시 (YYYY-MM-DD)
 * @param type      거래 유형
 * @param symbol    종목명 (입출금이면 없음)
 * @param account   계좌명
 * @param tag       자유 태그 (없을 수 있음)
 * @param currency  원통화 코드 (KRW/USD/…)
 * @param quantity  수량 (원통화 종목 단위)
 * @param price     단가 (원통화)
 * @param amount    거래 금액 (거래 통화 기준)
 * @param memo      메모
 */
export interface PortfolioTx {
  id: number;
  accountId?: number;
  tradedAt: string;
  type: TxType;
  symbol?: string;
  account: string;
  tag?: string;
  currency: string;
  quantity?: number;
  price?: number;
  amount: number;
  fee?: number;
  memo?: string;
}

/** 환전 한 건(통화 전환, 예: KRW→USD). 순납입엔 잡히지 않음. */
export interface PortfolioFxConversion {
  id: number;
  accountId?: number;
  account: string;
  convDate: string;
  fromCurrency: string;
  fromAmount: number;
  toCurrency: string;
  toAmount: number;
  fee?: number;
  memo?: string;
}

/** 거래 유형별 한국어 라벨. */
export const TX_TYPE_LABEL: Record<TxType, string> = {
  BUY: "매수",
  SELL: "매도",
  DEPOSIT: "입금",
  WITHDRAW: "출금",
  DIVIDEND: "배당",
  INTEREST: "이자",
};

/** 포트폴리오 요약 (백엔드 응답과 일치). 금액은 원화. */
export interface PortfolioSummary {
  totalKrw: number;
  cashKrw: number;
  netContribKrw: number;
  growthKrw: number;
  evalPnlKrw: number;
  evalPnlPct: number;
  xirrPct: number;
  cashByCurrency: Record<string, number>;
}

/** 보유 포지션 (표시용). 단가는 원통화, 평가·손익은 KRW. */
export interface PortfolioPosition {
  symbol: string;
  account: string;
  currency: string;
  tag?: string;
  quantity: number;
  avgPriceNat: number;
  curPriceNat: number;
  evalKrw: number;
  pnlKrw: number;
  pnlPct: number;
  weightPct: number;
  holdingId?: number | null;
  annualDividendPct?: number | null;
  dividendTracked?: boolean;
}

/** 리밸런싱 계획의 목표 배분 항목. */
export interface RebalancePlanEntry {
  symbol: string;
  account: string | null;
  currency: string;
  targetPct: number;
}

/** 저장된 리밸런싱 계획. */
export interface PortfolioRebalancePlan {
  id: number;
  name: string;
  entries: RebalancePlanEntry[];
}

/** 계좌 (백엔드 응답과 일치). 잔액·평가액은 아직 미집계. */
export interface PortfolioAccount {
  id: number;
  name: string;
  brokerName?: string;
  description?: string;
  createdAt?: string;
}

/** 상장 시장 — 백엔드 PortfolioMarket enum과 일치. 현재가 자동조회 키의 시장 축. */
export const MARKETS = [
  { value: "KOSPI", label: "코스피", currency: "KRW" },
  { value: "KOSDAQ", label: "코스닥", currency: "KRW" },
  { value: "KONEX", label: "코넥스", currency: "KRW" },
  { value: "NASDAQ", label: "나스닥", currency: "USD" },
  { value: "NYSE", label: "뉴욕", currency: "USD" },
  { value: "AMEX", label: "아멕스", currency: "USD" },
  { value: "HKEX", label: "홍콩", currency: "HKD" },
  { value: "TSE", label: "도쿄", currency: "JPY" },
  { value: "SSE", label: "상해", currency: "CNY" },
  { value: "SZSE", label: "심천", currency: "CNY" },
] as const;

/** 거래 입력용 통화 목록. */
export const CURRENCIES = ["KRW", "USD", "HKD", "JPY", "CNY", "EUR"] as const;

/** 라운드트립(진입~청산) 성과. */
export interface PortfolioRoundTrip {
  id: number;
  symbol: string;
  currency: string;
  group: string;
  entry: string;
  exit?: string;
  holdingDays: number;
  avgNat: number;
  exitNat?: number;
  pnlNat: number;
  pnlKrw: number;
  pnlPct: number;
  open: boolean;
}

/** 계좌 공유 grant. */
export interface PortfolioShare {
  id: number;
  accountId: number;
  accountName: string;
  grantee: string;
  permission: "READ" | "READ_RELATIVE" | "WRITE";
  direction: "OUT" | "IN";
}

/** 공유 권한 한국어 라벨. */
export const SHARE_PERM_LABEL: Record<PortfolioShare["permission"], string> = {
  READ: "읽기",
  READ_RELATIVE: "읽기(상대값)",
  WRITE: "읽기·쓰기",
};

export const CUR_SYMBOL: Record<string, string> = { USD: "$", JPY: "¥", CNY: "元", EUR: "€", HKD: "HK$" };

/** 원통화 금액 문자열(KRW면 심볼 없이 숫자). */
export function natMoney(v: number, cur: string): string {
  const n = v.toLocaleString("ko-KR", { maximumFractionDigits: 2 });
  return cur === "KRW" ? n : (CUR_SYMBOL[cur] ?? "") + n;
}

/** 원통화 부호 금액 문자열(+/− + 통화 심볼). */
export function natSigned(v: number, cur: string): string {
  const n = Math.abs(v).toLocaleString("ko-KR", { maximumFractionDigits: 2 });
  const body = cur === "KRW" ? n : (CUR_SYMBOL[cur] ?? "") + n;
  return (v >= 0 ? "+" : "−") + body;
}
