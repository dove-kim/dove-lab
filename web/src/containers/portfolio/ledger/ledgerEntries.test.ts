import { describe, expect, it } from "vitest";
import type { PortfolioFxConversion, PortfolioTx, TxType } from "@/types/portfolio";
import { groupOf, ledgerDateText, toLedgerEntries } from "./ledgerEntries";

function tx(id: number, type: TxType, tradedAt: string, over: Partial<PortfolioTx> = {}): PortfolioTx {
  return {
    id,
    accountId: 1,
    account: "미국",
    type,
    tradedAt,
    currency: "USD",
    amount: 100,
    ...over,
  };
}

function fx(id: number, convDate: string): PortfolioFxConversion {
  return {
    id,
    accountId: 1,
    account: "미국",
    convDate,
    fromCurrency: "KRW",
    fromAmount: 500_000,
    toCurrency: "USD",
    toAmount: 350,
  };
}

describe("ledgerEntries", () => {
  describe("groupOf", () => {
    it("매수·매도는 매매 묶음이다", () => {
      expect(groupOf("BUY")).toBe("trade");
      expect(groupOf("SELL")).toBe("trade");
    });

    it("입금·출금은 입출금 묶음이다", () => {
      expect(groupOf("DEPOSIT")).toBe("cash");
      expect(groupOf("WITHDRAW")).toBe("cash");
    });

    it("배당·이자는 수익 묶음이다", () => {
      expect(groupOf("DIVIDEND")).toBe("income");
      expect(groupOf("INTEREST")).toBe("income");
    });
  });

  describe("toLedgerEntries", () => {
    it("거래와 환전을 한 타임라인으로 합쳐 일자 내림차순으로 돌려준다", () => {
      const entries = toLedgerEntries(
        [tx(1, "BUY", "2026-06-19", { symbol: "QQQM" }), tx(2, "DEPOSIT", "2026-10-21")],
        [fx(9, "2026-07-18")]
      );

      expect(entries.map((e) => e.date)).toEqual(["2026-10-21", "2026-07-18", "2026-06-19"]);
      expect(entries.map((e) => e.group)).toEqual(["cash", "fx", "trade"]);
    });

    it("환전은 받은 통화와 금액을 쓰고 통화 전환을 내용으로 적는다", () => {
      const [entry] = toLedgerEntries([], [fx(9, "2026-07-18")]);

      expect(entry.label).toBe("환전");
      expect(entry.title).toBe("KRW → USD");
      expect(entry.amount).toBe(350);
      expect(entry.currency).toBe("USD");
      expect(entry.fx?.fromAmount).toBe(500_000);
      expect(entry.tx).toBeNull();
    });

    it("거래와 환전의 키가 겹치지 않는다", () => {
      const entries = toLedgerEntries([tx(9, "BUY", "2026-07-18", { symbol: "DIA" })], [fx(9, "2026-07-18")]);

      expect(new Set(entries.map((e) => e.key)).size).toBe(2);
    });

    it("종목이 없는 거래는 계좌명을 내용으로 쓴다", () => {
      const [entry] = toLedgerEntries([tx(1, "DEPOSIT", "2026-10-21")], []);

      expect(entry.title).toBe("미국");
      expect(entry.quantity).toBeNull();
    });
  });

  describe("ledgerDateText", () => {
    it("올해 기록은 월·일만 보여준다", () => {
      expect(ledgerDateText("2026-10-21", 2026)).toBe("10-21");
    });

    it("다른 해 기록은 연도까지 보여준다", () => {
      expect(ledgerDateText("2032-06-14", 2026)).toBe("2032-06-14");
      expect(ledgerDateText("2025-10-15", 2026)).toBe("2025-10-15");
    });
  });
});
