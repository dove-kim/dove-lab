export const PAD         = { top: 12, right: 60, bottom: 8, left: 60 } as const;
export const CANDLE_H    = 220;
export const GAP_H       = 18;
export const VOL_H       = 72;
export const SCROLL_H    = 18;
export const PANEL_H     = 80;
export const PANEL_GAP   = 10;
export const MAX_VISIBLE  = 120;
export const BAR_GAP      = 2;
export const MAX_BAR_SLOT = 24;

export const PRICE_BOT = PAD.top + CANDLE_H + GAP_H + VOL_H;  // 322
export const SVG_H     = PRICE_BOT + SCROLL_H + PAD.bottom;   // 348

export function clamp(v: number, min: number, max: number) {
  return Math.max(min, Math.min(max, v));
}

export function totalSvgH(panelCount: number): number {
  return SVG_H + panelCount * (PANEL_H + PANEL_GAP);
}
