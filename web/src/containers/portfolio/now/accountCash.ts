import type { PortfolioFxConversion, PortfolioTx } from "@/types/portfolio";

/** 거래 1건이 자기 통화 현금에 미치는 영향(백엔드 PortfolioCashCalculator와 동일 규칙). */
export function cashEffect(t: PortfolioTx): number {
  const amount = t.amount;
  const fee = t.fee ?? 0;
  switch (t.type) {
    case "DEPOSIT":
    case "DIVIDEND":
    case "INTEREST":
      return amount;
    case "WITHDRAW":
      return -amount;
    case "BUY":
      return -(amount + fee);
    case "SELL":
      return amount - fee;
    default:
      return 0;
  }
}

/**
 * 거래와 환전을 접어 계좌별·통화별 예수금을 계산한다.
 */
export function cashByAccount(
  txns: PortfolioTx[],
  convs: PortfolioFxConversion[]
): Map<number, Record<string, number>> {
  const m = new Map<number, Record<string, number>>();
  const bump = (accountId: number, currency: string, delta: number) => {
    const rec = m.get(accountId) ?? {};
    rec[currency] = (rec[currency] ?? 0) + delta;
    m.set(accountId, rec);
  };
  for (const t of txns) {
    if (t.accountId == null) continue;
    bump(t.accountId, t.currency, cashEffect(t));
  }
  for (const c of convs) {
    if (c.accountId == null) continue;
    bump(c.accountId, c.fromCurrency, -(c.fromAmount + (c.fee ?? 0)));
    bump(c.accountId, c.toCurrency, c.toAmount);
  }
  return m;
}
