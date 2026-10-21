import { type PortfolioFxConversion, type PortfolioTx, type TxType, TX_TYPE_LABEL } from "@/types/portfolio";

/** 기록 타임라인의 묶음 — 유형 칩과 1:1 대응한다. */
export type LedgerGroup = "trade" | "cash" | "fx" | "income";

/**
 * 기록 타임라인의 한 줄 — 거래와 환전을 같은 모양으로 맞춘 표시용 모델.
 *
 * @param key      행 식별자(거래와 환전이 겹치지 않도록 접두사를 붙임)
 * @param date     발생 일자(YYYY-MM-DD)
 * @param group    유형 묶음
 * @param label    유형 라벨(매수/입금/환전 등)
 * @param account  계좌명
 * @param title    주 내용(종목명 또는 통화 전환)
 * @param quantity 수량(매매만)
 * @param price    단가(매매만)
 * @param amount   금액(환전은 받은 금액)
 * @param currency 금액의 통화(환전은 받은 통화)
 * @param tx       원본 거래(환전이면 null)
 * @param fx       원본 환전(거래면 null)
 */
export interface LedgerEntry {
  key: string;
  date: string;
  group: LedgerGroup;
  label: string;
  account: string;
  title: string;
  quantity: number | null;
  price: number | null;
  amount: number;
  currency: string;
  tx: PortfolioTx | null;
  fx: PortfolioFxConversion | null;
}

/**
 * 타임라인에 쓰는 날짜 표기 — 올해면 월·일만, 다른 해면 연도까지.
 *
 * <p>연도를 잘못 넣은 기록이 눈에 띄게 한다.
 *
 * @param date     ISO 날짜
 * @param thisYear 기준 연도(보통 올해)
 */
export function ledgerDateText(date: string, thisYear: number): string {
  const d = date.slice(0, 10);
  return d.slice(0, 4) === String(thisYear) ? d.slice(5) : d;
}

/** 거래 유형이 속하는 묶음. */
export function groupOf(type: TxType): LedgerGroup {
  if (type === "BUY" || type === "SELL") return "trade";
  if (type === "DEPOSIT" || type === "WITHDRAW") return "cash";
  return "income";
}

/**
 * 거래와 환전을 하나의 타임라인으로 합친다(일자 내림차순).
 */
export function toLedgerEntries(txns: PortfolioTx[], convs: PortfolioFxConversion[]): LedgerEntry[] {
  const entries: LedgerEntry[] = txns.map((t) => ({
    key: `tx-${t.id}`,
    date: t.tradedAt.slice(0, 10),
    group: groupOf(t.type),
    label: TX_TYPE_LABEL[t.type],
    account: t.account,
    title: t.symbol ?? t.account,
    quantity: t.quantity ?? null,
    price: t.price ?? null,
    amount: t.amount,
    currency: t.currency,
    tx: t,
    fx: null,
  }));

  for (const c of convs) {
    entries.push({
      key: `fx-${c.id}`,
      date: c.convDate.slice(0, 10),
      group: "fx",
      label: "환전",
      account: c.account,
      title: `${c.fromCurrency} → ${c.toCurrency}`,
      quantity: null,
      price: null,
      amount: c.toAmount,
      currency: c.toCurrency,
      tx: null,
      fx: c,
    });
  }

  return entries.sort((a, b) => (a.date === b.date ? b.key.localeCompare(a.key) : b.date.localeCompare(a.date)));
}
