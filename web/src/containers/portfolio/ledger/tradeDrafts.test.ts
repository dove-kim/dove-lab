import { describe, expect, it } from "vitest";
import { type TradeDraft, draftAmount, emptyDraft, syncDrafts, toTradeInput } from "./tradeDrafts";

function filled(key: number, isSell: boolean, over: Partial<TradeDraft> = {}): TradeDraft {
  return {
    ...emptyDraft(key, isSell),
    selection: { symbol: "DIA", market: "NASDAQ", ticker: "DIA", currency: "USD" },
    quantity: "2",
    price: "150",
    ...over,
  };
}

describe("draftAmount", () => {
  it("수량×단가를 계산한다", () => {
    expect(draftAmount(filled(1, false))).toBe(300);
  });

  it("수량이나 단가가 비면 없다", () => {
    expect(draftAmount(emptyDraft(1, false))).toBeNull();
    expect(draftAmount(filled(1, false, { price: "" }))).toBeNull();
  });
});

describe("toTradeInput", () => {
  it("수수료가 비면 0으로 넘긴다", () => {
    expect(toTradeInput(filled(1, false)).fee).toBe(0);
  });

  it("적은 수수료를 그대로 넘긴다", () => {
    expect(toTradeInput(filled(1, false, { fee: "15" })).fee).toBe(15);
  });

  it("소수점 수수료를 그대로 넘긴다", () => {
    expect(toTradeInput(filled(1, false, { fee: "0.0035" })).fee).toBeCloseTo(0.0035, 8);
  });

  it("매도 여부를 그대로 넘긴다", () => {
    expect(toTradeInput(filled(1, true)).isSell).toBe(true);
  });
});

describe("syncDrafts", () => {
  it("매수만 고르면 매수 칸 하나를 만든다", () => {
    const drafts = syncDrafts([], true, false, 1);

    expect(drafts).toHaveLength(1);
    expect(drafts[0].isSell).toBe(false);
  });

  it("매수·매도를 함께 고르면 칸을 하나씩 만든다", () => {
    const drafts = syncDrafts([], true, true, 1);

    expect(drafts.map((d) => d.isSell)).toEqual([false, true]);
  });

  it("이미 적은 칸은 그대로 둔다", () => {
    const existing = filled(7, false, { quantity: "9" });
    const drafts = syncDrafts([existing], true, false, 8);

    expect(drafts).toEqual([existing]);
  });

  it("할 일에서 빼면 그 종류의 칸이 사라진다", () => {
    const drafts = syncDrafts([filled(1, false), filled(2, true)], true, false, 3);

    expect(drafts.map((d) => d.isSell)).toEqual([false]);
  });

  it("같은 종류의 칸이 여럿이면 모두 남긴다", () => {
    const drafts = syncDrafts([filled(1, false), filled(2, false)], true, false, 3);

    expect(drafts).toHaveLength(2);
  });

  it("아무것도 안 고르면 칸이 없다", () => {
    expect(syncDrafts([filled(1, false)], false, false, 2)).toEqual([]);
  });
});
