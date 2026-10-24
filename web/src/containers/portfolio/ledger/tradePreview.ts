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
 * 미리보기 대상 매매 한 건.
 *
 * @param symbol   종목명
 * @param currency 원통화 코드
 * @param isSell   매도 여부
 * @param quantity 수량
 * @param price    단가(원통화)
 * @param fee      수수료(원통화)
 */
export interface TradeInput {
  symbol: string;
  currency: string;
  isSell: boolean;
  quantity: number;
  price: number;
  fee: number;
}

/** 미리보기 계산 중 한 (계좌, 종목)의 보유 상태. */
interface Holding {
  quantity: number;
  /** 수량×단가 합 — 평단의 분자(수수료 제외, 백엔드 costNat과 같은 규칙). */
  costNat: number;
  /** 매입 원가 — 실현손익의 기준(수수료 포함, 백엔드 investedNat과 같은 규칙). */
  investedNat: number;
}

/** 대상 계좌의 해당 종목 보유를 합쳐 온다. */
function holdingOf(positions: PortfolioPosition[], symbol: string, accountName: string | null): Holding {
  const matched = positions.filter(
    (p) => p.symbol === symbol.trim() && (accountName == null || p.account === accountName)
  );
  return {
    quantity: matched.reduce((s, p) => s + p.quantity, 0),
    costNat: matched.reduce((s, p) => s + p.quantity * p.avgPriceNat, 0),
    investedNat: matched.reduce((s, p) => s + (p.investedNat ?? p.quantity * p.avgPriceNat), 0),
  };
}

/** 매매 한 건을 보유 상태에 얹는다(이동평균 원가법). */
function apply(held: Holding, trade: TradeInput): { next: Holding; preview: TradePreview } {
  const beforeAvg = held.quantity > 0 ? held.costNat / held.quantity : null;

  if (trade.isSell) {
    const sellQty = Math.min(trade.quantity, held.quantity);
    const afterQty = held.quantity - sellQty;
    const remainRatio = held.quantity > 0 ? afterQty / held.quantity : 0;
    const costOfSold = held.investedNat * (1 - remainRatio);
    const next: Holding = {
      quantity: afterQty,
      costNat: held.costNat * remainRatio,
      investedNat: held.investedNat * remainRatio,
    };
    return {
      next,
      preview: {
        currency: trade.currency,
        beforeQty: held.quantity,
        afterQty,
        beforeAvg,
        afterAvg: afterQty > 0 ? beforeAvg : null,
        // 매도 대금에서 수수료를 빼고 판 만큼의 원가를 뺀다(백엔드와 같은 규칙).
        realizedNat: held.quantity > 0 ? trade.price * sellQty - trade.fee - costOfSold : null,
        overSelling: trade.quantity > held.quantity,
      },
    };
  }

  const afterQty = held.quantity + trade.quantity;
  const next: Holding = {
    quantity: afterQty,
    costNat: held.costNat + trade.quantity * trade.price,
    investedNat: held.investedNat + trade.quantity * trade.price + trade.fee,
  };
  return {
    next,
    preview: {
      currency: trade.currency,
      beforeQty: held.quantity,
      afterQty,
      beforeAvg,
      // 평단에는 수수료가 들어가지 않는다(백엔드 costNat과 같은 규칙).
      afterAvg: next.costNat / afterQty,
      realizedNat: null,
      overSelling: false,
    },
  };
}

/** 수량·단가가 들어찬 매매인지. */
function usable(trade: TradeInput): boolean {
  return !!trade.symbol.trim() && trade.quantity > 0 && trade.price > 0;
}

/**
 * 매매 한 건을 지금 보유에 얹어 바뀔 수량과 평단을 미리 계산한다.
 *
 * @param positions   보유 포지션 목록
 * @param accountName 대상 계좌명(없으면 종목 전체 합산)
 * @param trade       미리볼 매매
 */
export function previewTrade(
  positions: PortfolioPosition[],
  accountName: string | null,
  trade: TradeInput
): TradePreview | null {
  if (!usable(trade)) return null;
  return apply(holdingOf(positions, trade.symbol, accountName), trade).preview;
}

/**
 * 여러 매매를 입력 순서대로 얹어 각 건의 미리보기를 만든다.
 *
 * <p>같은 종목을 여러 번 사고팔면 앞 건의 결과 위에 다음 건이 쌓인다.
 *
 * @param positions   보유 포지션 목록
 * @param accountName 대상 계좌명(없으면 종목 전체 합산)
 * @param trades      미리볼 매매 목록
 */
export function previewTrades(
  positions: PortfolioPosition[],
  accountName: string | null,
  trades: TradeInput[]
): (TradePreview | null)[] {
  const working = new Map<string, Holding>();
  return trades.map((trade) => {
    if (!usable(trade)) return null;
    const key = trade.symbol.trim();
    const held = working.get(key) ?? holdingOf(positions, key, accountName);
    const { next, preview } = apply(held, trade);
    working.set(key, next);
    return preview;
  });
}
