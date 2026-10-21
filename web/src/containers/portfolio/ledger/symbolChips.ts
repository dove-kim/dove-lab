import type { PortfolioHolding } from "@/types/portfolio";

/**
 * 빠른 입력의 종목 칩 — 한 번 누르면 거래소·티커·통화·계좌가 함께 정해진다.
 *
 * @param symbol    종목명
 * @param market    상장 시장
 * @param ticker    시장 내 종목 코드
 * @param currency  원통화 코드
 * @param accountId 그 종목을 들고 있는 계좌 ID
 */
export interface SymbolChip {
  symbol: string;
  market: string;
  ticker: string;
  currency: string;
  accountId: number | null;
}

/**
 * 보유 매핑을 최근 거래 순으로 정렬해 종목 칩 목록을 만든다(종목명 기준 중복 제거).
 *
 * @param holdings       보유 종목 식별 정보
 * @param recentSymbols  최근 거래 순 종목명
 * @param limit          최대 칩 개수
 */
export function toSymbolChips(
  holdings: PortfolioHolding[],
  recentSymbols: string[],
  limit: number
): SymbolChip[] {
  const bySymbol = new Map<string, SymbolChip>();
  for (const h of holdings) {
    if (bySymbol.has(h.symbol)) continue;
    bySymbol.set(h.symbol, {
      symbol: h.symbol,
      market: h.market,
      ticker: h.ticker,
      currency: h.currency,
      accountId: h.accountId ?? null,
    });
  }

  const ordered: SymbolChip[] = [];
  const seen = new Set<string>();
  for (const s of recentSymbols) {
    const chip = bySymbol.get(s);
    if (chip && !seen.has(s)) {
      seen.add(s);
      ordered.push(chip);
    }
  }
  for (const [symbol, chip] of bySymbol) {
    if (!seen.has(symbol)) ordered.push(chip);
  }
  return ordered.slice(0, limit);
}
