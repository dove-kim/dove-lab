import type { PortfolioRoundTrip, PortfolioTx } from "@/types/portfolio";
import { type Period, inPeriod, overlapDays, unionOverlapDays } from "./period";

/**
 * 한 종목에서 사고팔며 낸 성과 — 사이클 여러 번을 합친 결과.
 *
 * @param symbol      종목명
 * @param currency    원통화 코드
 * @param accounts    이 종목을 거래한 계좌명
 * @param cycles      기간과 겹치는 사이클(진입일 내림차순)
 * @param realizedKrw 기간 내 실현 손익 합(원화)
 * @param realizedPct 기간 내 실현 수익률(%, 판 만큼의 원가 대비 — 매도가 없으면 null)
 * @param sellCount   기간 내 매도 횟수
 * @param heldDays    기간과 겹치는 보유일(달력 기준 — 여러 계좌가 겹쳐 들고 있어도 하루는 하루)
 * @param dividendNat 기간 내 배당 합(원통화)
 * @param open        지금 보유 중인지
 */
export interface SymbolPerformance {
  symbol: string;
  currency: string;
  accounts: string[];
  cycles: PortfolioRoundTrip[];
  realizedKrw: number;
  realizedPct: number | null;
  sellCount: number;
  heldDays: number;
  dividendNat: number;
  open: boolean;
}

/** 사이클의 보유 종료일 — 청산했으면 청산일, 보유 중이면 기준일. */
function cycleEnd(cycle: PortfolioRoundTrip, asOf: string): string {
  return cycle.exit ?? asOf;
}

/**
 * 라운드트립과 배당 거래를 종목 단위로 접어 기간별 성과를 만든다(실현 손익 내림차순).
 *
 * @param roundTrips 라운드트립 목록
 * @param txns       거래 목록(배당 집계에 쓴다)
 * @param period     집계 기간
 * @param asOf       보유 중 사이클의 기준일(ISO)
 */
export function foldSymbolPerformance(
  roundTrips: PortfolioRoundTrip[],
  txns: PortfolioTx[],
  period: Period,
  asOf: string
): SymbolPerformance[] {
  const bySymbol = new Map<string, SymbolPerformance>();
  const costBySymbol = new Map<string, number>();
  const spansBySymbol = new Map<string, [string, string][]>();

  for (const cycle of roundTrips) {
    const held = overlapDays(cycle.entry, cycleEnd(cycle, asOf), period);
    const realized = cycle.realizations.filter((r) => inPeriod(r.date, period));
    // 기간에 보유도 매도도 걸치지 않은 사이클은 그 기간의 성과가 아니다.
    if (held === 0 && realized.length === 0) continue;

    const acc = bySymbol.get(cycle.symbol) ?? {
      symbol: cycle.symbol,
      currency: cycle.currency,
      accounts: [],
      cycles: [],
      realizedKrw: 0,
      realizedPct: null,
      sellCount: 0,
      heldDays: 0,
      dividendNat: 0,
      open: false,
    };
    acc.cycles.push(cycle);
    if (!acc.accounts.includes(cycle.group)) acc.accounts.push(cycle.group);
    acc.realizedKrw += realized.reduce((s, r) => s + r.pnlKrw, 0);
    costBySymbol.set(cycle.symbol, (costBySymbol.get(cycle.symbol) ?? 0) + realized.reduce((s, r) => s + r.costKrw, 0));
    acc.sellCount += realized.length;
    acc.open = acc.open || cycle.open;
    bySymbol.set(cycle.symbol, acc);

    const spans = spansBySymbol.get(cycle.symbol) ?? [];
    spans.push([cycle.entry, cycleEnd(cycle, asOf)]);
    spansBySymbol.set(cycle.symbol, spans);
  }

  for (const t of txns) {
    if (t.type !== "DIVIDEND" || !t.symbol) continue;
    if (!inPeriod(t.tradedAt, period)) continue;
    const acc = bySymbol.get(t.symbol);
    if (acc) acc.dividendNat += t.amount;
  }

  const result = Array.from(bySymbol.values());
  for (const s of result) {
    s.cycles.sort((a, b) => b.entry.localeCompare(a.entry));
    const cost = costBySymbol.get(s.symbol) ?? 0;
    s.realizedPct = cost > 0 ? Math.round((s.realizedKrw / cost) * 1000) / 10 : null;
    s.heldDays = unionOverlapDays(spansBySymbol.get(s.symbol) ?? [], period);
  }
  return result.sort((a, b) => b.realizedKrw - a.realizedKrw);
}

/**
 * 한 해에 확정된 실현 성과.
 *
 * @param year    연도
 * @param pnlKrw  그 해 실현 손익 합(원화)
 * @param costKrw 그 해 판 것들의 매입 원가 합(원화)
 * @param pct     원가 대비 수익률(%, 원가가 0이면 null)
 */
export interface YearlyRealized {
  year: string;
  pnlKrw: number;
  costKrw: number;
  pct: number | null;
}

/**
 * 실현 손익을 매도 연도별로 합친다(연도 오름차순). 수익률은 그 해 판 것들의 원가 대비다.
 */
export function realizedByYear(roundTrips: PortfolioRoundTrip[]): YearlyRealized[] {
  const m = new Map<string, { pnlKrw: number; costKrw: number }>();
  for (const cycle of roundTrips) {
    for (const r of cycle.realizations) {
      const year = r.date.slice(0, 4);
      const acc = m.get(year) ?? { pnlKrw: 0, costKrw: 0 };
      acc.pnlKrw += r.pnlKrw;
      acc.costKrw += r.costKrw;
      m.set(year, acc);
    }
  }
  return Array.from(m.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([year, v]) => ({
      year,
      pnlKrw: v.pnlKrw,
      costKrw: v.costKrw,
      pct: v.costKrw > 0 ? Math.round((v.pnlKrw / v.costKrw) * 1000) / 10 : null,
    }));
}
