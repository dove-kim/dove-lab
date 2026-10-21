import type { PortfolioPosition } from "@/types/portfolio";

/**
 * 이 매매를 기록하면 보유가 어떻게 바뀌는지 — 저장 직전 확인용.
 *
 * @param currency      원통화 코드
 * @param beforeQty     지금 보유 수량
 * @param afterQty      기록 후 보유 수량
 * @param beforeAvg     지금 평단(보유가 없으면 null)
 * @param afterAvg      기록 후 평단(전량 매도면 null)
 * @param realizedNat   이번 매도로 확정되는 손익(매수면 null)
 * @param overSelling   보유 수량보다 많이 파는지
 */
export interface TradePreview {
  currency: string;
  beforeQty: number;
  afterQty: number;
  beforeAvg: number | null;
  afterAvg: number | null;
  realizedNat: number | null;
  overSelling: boolean;
}

/**
 * 매매 한 건을 지금 보유에 얹어 바뀔 수량과 평단을 미리 계산한다(이동평균 원가법).
 *
 * @param positions   보유 포지션 목록
 * @param symbol      종목명
 * @param accountName 대상 계좌명(없으면 종목 전체 합산)
 * @param currency    원통화 코드
 * @param isSell      매도 여부
 * @param quantity    수량
 * @param price       단가(원통화)
 */
export function previewTrade(
  positions: PortfolioPosition[],
  symbol: string,
  accountName: string | null,
  currency: string,
  isSell: boolean,
  quantity: number,
  price: number
): TradePreview | null {
  if (!symbol.trim() || !(quantity > 0) || !(price > 0)) return null;

  const matched = positions.filter(
    (p) => p.symbol === symbol.trim() && (accountName == null || p.account === accountName)
  );
  const beforeQty = matched.reduce((s, p) => s + p.quantity, 0);
  const beforeCost = matched.reduce((s, p) => s + p.quantity * p.avgPriceNat, 0);
  const beforeAvg = beforeQty > 0 ? beforeCost / beforeQty : null;

  if (isSell) {
    const sellQty = Math.min(quantity, beforeQty);
    const afterQty = beforeQty - sellQty;
    return {
      currency,
      beforeQty,
      afterQty,
      beforeAvg,
      afterAvg: afterQty > 0 ? beforeAvg : null,
      realizedNat: beforeAvg == null ? null : (price - beforeAvg) * sellQty,
      overSelling: quantity > beforeQty,
    };
  }

  const afterQty = beforeQty + quantity;
  return {
    currency,
    beforeQty,
    afterQty,
    beforeAvg,
    afterAvg: (beforeCost + quantity * price) / afterQty,
    realizedNat: null,
    overSelling: false,
  };
}
