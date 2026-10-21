import { describe, expect, it } from "vitest";
import type { PortfolioRealization, PortfolioRoundTrip, PortfolioTx } from "@/types/portfolio";
import { type Period } from "./period";
import { foldSymbolPerformance, realizedByYear } from "./symbolPerformance";

const ALL: Period = { key: "all", label: "전체 기간", from: null, to: null };
const YEAR_2026: Period = { key: "thisYear", label: "올해", from: "2026-01-01", to: "2026-12-31" };
const AS_OF = "2026-10-21";

function realization(date: string, pnlKrw: number, costKrw: number): PortfolioRealization {
  return { date, quantity: 1, pnlNat: pnlKrw, pnlKrw, costKrw, pnlPct: 0 };
}

function cycle(over: Partial<PortfolioRoundTrip> = {}): PortfolioRoundTrip {
  return {
    id: 1,
    symbol: "DIA",
    currency: "USD",
    group: "미국",
    entry: "2026-01-01",
    holdingDays: 100,
    avgNat: 100,
    pnlNat: 0,
    pnlKrw: 0,
    pnlPct: 0,
    open: true,
    realizations: [],
    ...over,
  };
}

function dividend(symbol: string, tradedAt: string, amount: number): PortfolioTx {
  return {
    id: 1,
    account: "미국",
    type: "DIVIDEND",
    tradedAt,
    symbol,
    currency: "USD",
    amount,
  };
}

describe("foldSymbolPerformance", () => {
  it("한 종목의 여러 사이클을 한 줄로 합친다", () => {
    const rows = foldSymbolPerformance(
      [
        cycle({ id: 1, entry: "2024-03-01", exit: "2024-11-01", open: false }),
        cycle({ id: 2, entry: "2025-04-01" }),
      ],
      [],
      ALL,
      AS_OF
    );

    expect(rows).toHaveLength(1);
    expect(rows[0].cycles).toHaveLength(2);
    expect(rows[0].open).toBe(true);
  });

  it("전량 판 뒤 다시 사기까지의 공백은 보유기간에서 뺀다", () => {
    const rows = foldSymbolPerformance(
      [
        cycle({ id: 1, entry: "2026-01-01", exit: "2026-01-11", open: false }),
        cycle({ id: 2, entry: "2026-03-01", exit: "2026-03-11", open: false }),
      ],
      [],
      ALL,
      AS_OF
    );

    expect(rows[0].heldDays).toBe(20);
  });

  it("여러 계좌가 같은 날 들고 있어도 하루는 하루다", () => {
    const rows = foldSymbolPerformance(
      [
        cycle({ id: 1, group: "미국", entry: "2026-01-01", exit: "2026-01-11", open: false }),
        cycle({ id: 2, group: "미국 장투", entry: "2026-01-01", exit: "2026-01-11", open: false }),
      ],
      [],
      ALL,
      AS_OF
    );

    expect(rows[0].heldDays).toBe(10);
    expect(rows[0].accounts).toEqual(["미국", "미국 장투"]);
  });

  it("실현 손익은 매도 날짜로 기간을 자른다", () => {
    const rows = foldSymbolPerformance(
      [
        cycle({
          entry: "2025-01-01",
          realizations: [realization("2025-10-15", 1_000, 10_000), realization("2026-01-21", 500, 2_000)],
        }),
      ],
      [],
      YEAR_2026,
      AS_OF
    );

    expect(rows[0].realizedKrw).toBe(500);
    expect(rows[0].sellCount).toBe(1);
  });

  it("수익률은 판 만큼의 원가로 나눈다", () => {
    const rows = foldSymbolPerformance(
      [cycle({ realizations: [realization("2026-02-01", 1_000, 10_000), realization("2026-03-01", 1_000, 10_000)] })],
      [],
      ALL,
      AS_OF
    );

    expect(rows[0].realizedPct).toBe(10);
  });

  it("매도가 없으면 수익률은 없다", () => {
    const rows = foldSymbolPerformance([cycle()], [], ALL, AS_OF);

    expect(rows[0].sellCount).toBe(0);
    expect(rows[0].realizedPct).toBeNull();
  });

  it("기간에 보유도 매도도 걸치지 않는 사이클은 제외한다", () => {
    const rows = foldSymbolPerformance(
      [cycle({ entry: "2024-01-01", exit: "2024-06-01", open: false })],
      [],
      YEAR_2026,
      AS_OF
    );

    expect(rows).toHaveLength(0);
  });

  it("배당은 기간 안의 것만 원통화로 합산한다", () => {
    const rows = foldSymbolPerformance(
      [cycle()],
      [dividend("DIA", "2026-03-01", 70.56), dividend("DIA", "2025-03-01", 100), dividend("QQQM", "2026-03-01", 50)],
      YEAR_2026,
      AS_OF
    );

    expect(rows[0].dividendNat).toBeCloseTo(70.56, 2);
  });

  it("실현 손익이 큰 종목이 먼저 온다", () => {
    const rows = foldSymbolPerformance(
      [
        cycle({ id: 1, symbol: "A", realizations: [realization("2026-02-01", 100, 1_000)] }),
        cycle({ id: 2, symbol: "B", realizations: [realization("2026-02-01", 900, 1_000)] }),
      ],
      [],
      ALL,
      AS_OF
    );

    expect(rows.map((r) => r.symbol)).toEqual(["B", "A"]);
  });
});

describe("realizedByYear", () => {
  it("매도 연도별로 손익과 원가를 합쳐 수익률을 낸다", () => {
    const rows = realizedByYear([
      cycle({ realizations: [realization("2025-10-15", 1_000, 10_000), realization("2026-01-21", 500, 2_000)] }),
      cycle({ id: 2, realizations: [realization("2025-02-01", 1_000, 10_000)] }),
    ]);

    expect(rows).toEqual([
      { year: "2025", pnlKrw: 2_000, costKrw: 20_000, pct: 10 },
      { year: "2026", pnlKrw: 500, costKrw: 2_000, pct: 25 },
    ]);
  });

  it("손실 난 해는 음수로 남는다", () => {
    const rows = realizedByYear([cycle({ realizations: [realization("2026-02-01", -3_000, 10_000)] })]);

    expect(rows[0].pnlKrw).toBe(-3_000);
    expect(rows[0].pct).toBe(-30);
  });

  it("매도가 없으면 빈 목록이다", () => {
    expect(realizedByYear([cycle()])).toEqual([]);
  });
});
