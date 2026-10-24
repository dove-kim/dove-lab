import { describe, expect, it } from "vitest";
import type { PortfolioPosition } from "@/types/portfolio";
import { type TradeInput, previewTrade, previewTrades } from "./tradePreview";

function position(over: Partial<PortfolioPosition> = {}): PortfolioPosition {
  const quantity = over.quantity ?? 23;
  const avgPriceNat = over.avgPriceNat ?? 125.15133;
  return {
    symbol: "DIA",
    account: "미국 적립식",
    currency: "USD",
    quantity,
    avgPriceNat,
    curPriceNat: avgPriceNat,
    investedNat: quantity * avgPriceNat,
    evalKrw: 0,
    pnlKrw: 0,
    pnlPct: 0,
    weightPct: 0,
    ...over,
  };
}

function buy(symbol: string, quantity: number, price: number, fee = 0): TradeInput {
  return { symbol, currency: "USD", isSell: false, quantity, price, fee };
}

function sell(symbol: string, quantity: number, price: number, fee = 0): TradeInput {
  return { symbol, currency: "USD", isSell: true, quantity, price, fee };
}

describe("previewTrade", () => {
  describe("매수", () => {
    it("이동평균 원가법으로 평단을 다시 계산한다", () => {
      const preview = previewTrade([position()], "미국 적립식", buy("DIA", 2, 150));

      // 실제 저장 결과와 같은 값: (23×125.15133 + 2×150) / 25
      expect(preview?.beforeQty).toBe(23);
      expect(preview?.afterQty).toBe(25);
      expect(preview?.afterAvg).toBeCloseTo(127.1392236, 6);
    });

    it("수수료는 평단에 섞이지 않는다", () => {
      const withoutFee = previewTrade([position()], "미국 적립식", buy("DIA", 2, 150));
      const withFee = previewTrade([position()], "미국 적립식", buy("DIA", 2, 150, 5));

      expect(withFee?.afterAvg).toBeCloseTo(withoutFee?.afterAvg as number, 8);
    });

    it("처음 사는 종목은 평단이 매수가가 된다", () => {
      const preview = previewTrade([], "미국", buy("TSLA", 10, 412.3));

      expect(preview?.beforeQty).toBe(0);
      expect(preview?.beforeAvg).toBeNull();
      expect(preview?.afterAvg).toBeCloseTo(412.3, 6);
    });

    it("계좌를 지정하면 그 계좌 보유만 센다", () => {
      const positions = [position(), position({ account: "미국", quantity: 36, avgPriceNat: 4282.7 })];

      expect(previewTrade(positions, "미국 적립식", buy("DIA", 2, 150))?.beforeQty).toBe(23);
    });

    it("계좌를 지정하지 않으면 종목 전체를 합산한다", () => {
      const positions = [position(), position({ account: "미국", quantity: 37, avgPriceNat: 100 })];

      expect(previewTrade(positions, null, buy("DIA", 2, 150))?.beforeQty).toBe(60);
    });
  });

  describe("매도", () => {
    it("수량만 줄고 평단은 그대로다", () => {
      const preview = previewTrade([position({ quantity: 100, avgPriceNat: 300 })], "미국 적립식", sell("DIA", 40, 350));

      expect(preview?.afterQty).toBe(60);
      expect(preview?.afterAvg).toBeCloseTo(300, 6);
    });

    it("이번 매도로 확정되는 손익을 계산한다", () => {
      const preview = previewTrade([position({ quantity: 100, avgPriceNat: 300 })], "미국 적립식", sell("DIA", 40, 350));

      expect(preview?.realizedNat).toBeCloseTo(2_000, 6);
    });

    it("소수점 수수료도 실현손익에 그대로 반영한다", () => {
      const preview = previewTrade(
        [position({ quantity: 100, avgPriceNat: 300 })],
        "미국 적립식",
        sell("DIA", 40, 350, 0.0035)
      );

      expect(preview?.realizedNat).toBeCloseTo(1_999.9965, 8);
    });

    it("매도 수수료는 실현손익에서 뺀다", () => {
      const preview = previewTrade(
        [position({ quantity: 100, avgPriceNat: 300 })],
        "미국 적립식",
        sell("DIA", 40, 350, 15)
      );

      expect(preview?.realizedNat).toBeCloseTo(1_985, 6);
    });

    it("과거 매수 수수료가 쌓인 원가를 기준으로 손익을 낸다", () => {
      // 평단은 300이지만 매수 수수료 1,000이 더 들어간 보유.
      const held = position({ quantity: 100, avgPriceNat: 300, investedNat: 31_000 });
      const preview = previewTrade([held], "미국 적립식", sell("DIA", 40, 350));

      // 판 만큼의 원가 = 31,000 × 0.4 = 12,400 → 14,000 − 12,400
      expect(preview?.realizedNat).toBeCloseTo(1_600, 6);
    });

    it("전량 매도하면 평단이 사라진다", () => {
      const preview = previewTrade([position({ quantity: 10, avgPriceNat: 300 })], "미국 적립식", sell("DIA", 10, 350));

      expect(preview?.afterQty).toBe(0);
      expect(preview?.afterAvg).toBeNull();
    });

    it("보유보다 많이 팔면 표시만 하고 손익은 보유분까지만 센다", () => {
      const preview = previewTrade([position({ quantity: 10, avgPriceNat: 300 })], "미국 적립식", sell("DIA", 30, 350));

      expect(preview?.overSelling).toBe(true);
      expect(preview?.afterQty).toBe(0);
      expect(preview?.realizedNat).toBeCloseTo(500, 6);
    });
  });

  describe("입력이 모자랄 때", () => {
    it("종목·수량·단가가 비면 미리보기를 만들지 않는다", () => {
      expect(previewTrade([], "미국", buy("", 2, 150))).toBeNull();
      expect(previewTrade([], "미국", buy("DIA", NaN, 150))).toBeNull();
      expect(previewTrade([], "미국", buy("DIA", 2, 0))).toBeNull();
    });
  });
});

describe("previewTrades", () => {
  it("같은 종목을 여러 번 사면 앞 건의 결과 위에 쌓인다", () => {
    const previews = previewTrades([], "미국", [buy("TSLA", 10, 100), buy("TSLA", 10, 200)]);

    expect(previews[0]?.afterQty).toBe(10);
    expect(previews[0]?.afterAvg).toBeCloseTo(100, 6);
    expect(previews[1]?.beforeQty).toBe(10);
    expect(previews[1]?.afterQty).toBe(20);
    expect(previews[1]?.afterAvg).toBeCloseTo(150, 6);
  });

  it("사고 바로 파는 경우도 순서대로 접는다", () => {
    const previews = previewTrades([], "미국", [buy("TSLA", 10, 100), sell("TSLA", 4, 120)]);

    expect(previews[1]?.beforeQty).toBe(10);
    expect(previews[1]?.afterQty).toBe(6);
    expect(previews[1]?.realizedNat).toBeCloseTo(80, 6);
  });

  it("다른 종목끼리는 서로 영향을 주지 않는다", () => {
    const previews = previewTrades([position()], "미국 적립식", [buy("DIA", 2, 150), buy("QQQM", 1, 300)]);

    expect(previews[0]?.beforeQty).toBe(23);
    expect(previews[1]?.beforeQty).toBe(0);
  });

  it("아직 못 채운 칸은 미리보기가 없다", () => {
    const previews = previewTrades([], "미국", [buy("TSLA", 10, 100), buy("", 0, 0)]);

    expect(previews[0]).not.toBeNull();
    expect(previews[1]).toBeNull();
  });
});
