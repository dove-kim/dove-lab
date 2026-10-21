import { describe, expect, it } from "vitest";
import type { PortfolioHolding } from "@/types/portfolio";
import { toSymbolChips } from "./symbolChips";

function holding(id: number, symbol: string, over: Partial<PortfolioHolding> = {}): PortfolioHolding {
  return {
    id,
    accountId: 1,
    account: "미국",
    symbol,
    market: "NASDAQ",
    ticker: symbol,
    currency: "USD",
    ...over,
  };
}

describe("toSymbolChips", () => {
  it("최근 거래 순으로 칩을 정렬한다", () => {
    const chips = toSymbolChips(
      [holding(1, "QQQM"), holding(2, "DIA"), holding(3, "SCHD")],
      ["DIA", "SCHD"],
      8
    );

    expect(chips.map((c) => c.symbol)).toEqual(["DIA", "SCHD", "QQQM"]);
  });

  it("거래 이력이 없는 보유 종목도 뒤에 붙인다", () => {
    const chips = toSymbolChips([holding(1, "QQQM")], [], 8);

    expect(chips.map((c) => c.symbol)).toEqual(["QQQM"]);
  });

  it("같은 종목이 여러 계좌에 있어도 칩은 하나다", () => {
    const chips = toSymbolChips(
      [holding(1, "DIA", { accountId: 1 }), holding(2, "DIA", { accountId: 2 })],
      [],
      8
    );

    expect(chips).toHaveLength(1);
    expect(chips[0].accountId).toBe(1);
  });

  it("칩은 상한 개수까지만 만든다", () => {
    const holdings = ["A", "B", "C", "D"].map((s, i) => holding(i, s));

    expect(toSymbolChips(holdings, [], 2)).toHaveLength(2);
  });

  it("칩은 거래소·티커·통화를 함께 들고 있다", () => {
    const [chip] = toSymbolChips([holding(1, "삼성전자", { market: "KOSPI", ticker: "005930", currency: "KRW" })], [], 8);

    expect(chip).toEqual({
      symbol: "삼성전자",
      market: "KOSPI",
      ticker: "005930",
      currency: "KRW",
      accountId: 1,
    });
  });

  it("거래 이력에만 있고 보유 매핑이 없는 종목은 칩이 되지 않는다", () => {
    const chips = toSymbolChips([holding(1, "DIA")], ["없는종목", "DIA"], 8);

    expect(chips.map((c) => c.symbol)).toEqual(["DIA"]);
  });
});
