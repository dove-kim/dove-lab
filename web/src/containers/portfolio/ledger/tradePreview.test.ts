import { describe, expect, it } from "vitest";
import type { PortfolioPosition } from "@/types/portfolio";
import { previewTrade } from "./tradePreview";

function position(over: Partial<PortfolioPosition> = {}): PortfolioPosition {
  return {
    symbol: "DIA",
    account: "미국 적립식",
    currency: "USD",
    quantity: 23,
    avgPriceNat: 125.15133,
    curPriceNat: 125.15133,
    evalKrw: 0,
    pnlKrw: 0,
    pnlPct: 0,
    weightPct: 0,
    ...over,
  };
}

describe("previewTrade", () => {
  describe("매수", () => {
    it("이동평균 원가법으로 평단을 다시 계산한다", () => {
      const preview = previewTrade([position()], "DIA", "미국 적립식", "USD", false, 2, 150);

      // 실제 저장 결과와 같은 값: (23×125.15133 + 2×150) / 25
      expect(preview?.beforeQty).toBe(23);
      expect(preview?.afterQty).toBe(25);
      expect(preview?.afterAvg).toBeCloseTo(127.1392236, 6);
    });

    it("처음 사는 종목은 평단이 매수가가 된다", () => {
      const preview = previewTrade([], "TSLA", "미국", "USD", false, 10, 412.3);

      expect(preview?.beforeQty).toBe(0);
      expect(preview?.beforeAvg).toBeNull();
      expect(preview?.afterQty).toBe(10);
      expect(preview?.afterAvg).toBeCloseTo(412.3, 6);
    });

    it("계좌를 지정하면 그 계좌 보유만 센다", () => {
      const positions = [position(), position({ account: "미국", quantity: 36, avgPriceNat: 4282.7 })];
      const preview = previewTrade(positions, "DIA", "미국 적립식", "USD", false, 2, 150);

      expect(preview?.beforeQty).toBe(23);
    });

    it("계좌를 지정하지 않으면 종목 전체를 합산한다", () => {
      const positions = [position(), position({ account: "미국", quantity: 37, avgPriceNat: 100 })];
      const preview = previewTrade(positions, "DIA", null, "USD", false, 2, 150);

      expect(preview?.beforeQty).toBe(60);
    });
  });

  describe("매도", () => {
    it("수량만 줄고 평단은 그대로다", () => {
      const preview = previewTrade([position({ quantity: 100, avgPriceNat: 300 })], "DIA", "미국 적립식", "USD", true, 40, 350);

      expect(preview?.afterQty).toBe(60);
      expect(preview?.afterAvg).toBeCloseTo(300, 6);
    });

    it("이번 매도로 확정되는 손익을 계산한다", () => {
      const preview = previewTrade([position({ quantity: 100, avgPriceNat: 300 })], "DIA", "미국 적립식", "USD", true, 40, 350);

      expect(preview?.realizedNat).toBeCloseTo(2000, 6);
    });

    it("전량 매도하면 평단이 사라진다", () => {
      const preview = previewTrade([position({ quantity: 10, avgPriceNat: 300 })], "DIA", "미국 적립식", "USD", true, 10, 350);

      expect(preview?.afterQty).toBe(0);
      expect(preview?.afterAvg).toBeNull();
    });

    it("보유보다 많이 팔면 표시만 하고 손익은 보유분까지만 센다", () => {
      const preview = previewTrade([position({ quantity: 10, avgPriceNat: 300 })], "DIA", "미국 적립식", "USD", true, 30, 350);

      expect(preview?.overSelling).toBe(true);
      expect(preview?.afterQty).toBe(0);
      expect(preview?.realizedNat).toBeCloseTo(500, 6);
    });
  });

  describe("입력이 모자랄 때", () => {
    it("종목·수량·단가가 비면 미리보기를 만들지 않는다", () => {
      expect(previewTrade([], "", "미국", "USD", false, 2, 150)).toBeNull();
      expect(previewTrade([], "DIA", "미국", "USD", false, NaN, 150)).toBeNull();
      expect(previewTrade([], "DIA", "미국", "USD", false, 2, 0)).toBeNull();
    });
  });
});
