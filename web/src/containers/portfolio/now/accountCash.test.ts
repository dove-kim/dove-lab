import { describe, expect, it } from "vitest";
import type { PortfolioFxConversion, PortfolioTx, TxType } from "@/types/portfolio";
import { cashByAccount, cashEffect } from "./accountCash";

function tx(type: TxType, amount: number, over: Partial<PortfolioTx> = {}): PortfolioTx {
  return {
    id: 1,
    accountId: 1,
    account: "미국",
    type,
    tradedAt: "2026-10-21",
    currency: "KRW",
    amount,
    ...over,
  };
}

describe("cashEffect", () => {
  it("입금·배당·이자는 현금을 늘린다", () => {
    expect(cashEffect(tx("DEPOSIT", 1_000))).toBe(1_000);
    expect(cashEffect(tx("DIVIDEND", 50))).toBe(50);
    expect(cashEffect(tx("INTEREST", 10))).toBe(10);
  });

  it("출금은 현금을 줄인다", () => {
    expect(cashEffect(tx("WITHDRAW", 1_000))).toBe(-1_000);
  });

  it("매수는 금액과 수수료를 함께 뺀다", () => {
    expect(cashEffect(tx("BUY", 1_000, { fee: 15 }))).toBe(-1_015);
  });

  it("매도는 수수료를 뺀 금액만 더한다", () => {
    expect(cashEffect(tx("SELL", 1_000, { fee: 15 }))).toBe(985);
  });
});

describe("cashByAccount", () => {
  it("계좌별·통화별로 접는다", () => {
    const result = cashByAccount(
      [tx("DEPOSIT", 1_000_000), tx("BUY", 300, { currency: "USD", fee: 1 })],
      []
    );

    expect(result.get(1)).toEqual({ KRW: 1_000_000, USD: -301 });
  });

  it("환전은 보낸 통화에서 수수료까지 빼고 받은 통화에 더한다", () => {
    const conv: PortfolioFxConversion = {
      id: 1,
      accountId: 1,
      account: "미국",
      convDate: "2026-10-21",
      fromCurrency: "KRW",
      fromAmount: 500_000,
      toCurrency: "USD",
      toAmount: 350,
      fee: 1_000,
    };

    expect(cashByAccount([], [conv]).get(1)).toEqual({ KRW: -501_000, USD: 350 });
  });

  it("계좌가 없는 기록은 건너뛴다", () => {
    const result = cashByAccount([tx("DEPOSIT", 1_000, { accountId: undefined })], []);

    expect(result.size).toBe(0);
  });
});
