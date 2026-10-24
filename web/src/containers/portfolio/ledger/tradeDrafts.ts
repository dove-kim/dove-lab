import type { SymbolSelection } from "./SymbolPicker";
import type { TradeInput } from "./tradePreview";

/** 안내 입력에서 한 건의 매매를 적는 칸. */
export interface TradeDraft {
  key: number;
  isSell: boolean;
  selection: SymbolSelection;
  quantity: string;
  price: string;
  fee: string;
}

/** 빈 매매 칸을 만든다. */
export function emptyDraft(key: number, isSell: boolean): TradeDraft {
  return {
    key,
    isSell,
    selection: { symbol: "", market: "", ticker: "", currency: "KRW" },
    quantity: "",
    price: "",
    fee: "",
  };
}

/** 매매 칸의 금액(수량×단가). 아직 못 채웠으면 null. */
export function draftAmount(draft: TradeDraft): number | null {
  const quantity = parseFloat(draft.quantity);
  const price = parseFloat(draft.price);
  if (!isFinite(quantity) || !isFinite(price)) return null;
  return Number((quantity * price).toFixed(8));
}

/** 미리보기 계산에 넘길 형태로 바꾼다. */
export function toTradeInput(draft: TradeDraft): TradeInput {
  return {
    symbol: draft.selection.symbol,
    currency: draft.selection.currency,
    isSell: draft.isSell,
    quantity: parseFloat(draft.quantity),
    price: parseFloat(draft.price),
    fee: parseFloat(draft.fee) || 0,
  };
}

/**
 * 고른 할 일에 맞춰 매매 칸을 맞춘다 — 빠진 종류는 칸을 더하고, 빼면 그 종류의 칸을 없앤다.
 *
 * @param drafts  지금 칸 목록
 * @param wantBuy 매수를 고른 상태인지
 * @param wantSell 매도를 고른 상태인지
 * @param nextKey 새 칸에 줄 키
 */
export function syncDrafts(
  drafts: TradeDraft[],
  wantBuy: boolean,
  wantSell: boolean,
  nextKey: number
): TradeDraft[] {
  const kept = drafts.filter((d) => (d.isSell ? wantSell : wantBuy));
  const result = [...kept];
  let key = nextKey;
  if (wantBuy && !result.some((d) => !d.isSell)) result.push(emptyDraft(key++, false));
  if (wantSell && !result.some((d) => d.isSell)) result.push(emptyDraft(key++, true));
  return result;
}
