"use client";

import { useEffect, useRef, useState, type ReactElement } from "react";
import {
  PAD, CANDLE_H, GAP_H, VOL_H, SCROLL_H, PANEL_H, PANEL_GAP,
  PRICE_BOT, MAX_VISIBLE, BAR_GAP, totalSvgH,
} from "./chartConstants";
import { useChartInteraction } from "./useChartInteraction";
import { INDICATOR_META, PANEL_LABELS, type PanelId } from "./indicatorMeta";
import type { IndicatorType } from "@/types/filter";
import type { IndicatorPresetItem } from "@/types/indicator-preset";

export interface PriceBar {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface IndicatorBar {
  date: string;
  values: Partial<Record<IndicatorType, number>>;
}

interface Props {
  code: string;
  market: string;
  presetItems: IndicatorPresetItem[];
  panelOrder?: PanelId[];
  mode: Mode;
}

type Mode = "candle" | "line";

const RISING     = "#ef4444";
const FALLING    = "#3b82f6";
const LINE_COLOR = "#a78bfa";

function fmtPrice(p: number): string {
  const n = Math.round(p);
  if (n >= 1_000_000) return Math.round(n / 10_000) + "만";
  return n.toLocaleString("ko-KR");
}

function fmtVol(v: number): string {
  if (v >= 100_000_000) return Math.round(v / 100_000_000) + "억";
  if (v >= 10_000)      return Math.round(v / 10_000)      + "만";
  return v.toLocaleString("ko-KR");
}

function fmtVal(v: number): string {
  if (Math.abs(v) >= 1_000_000) return (v / 1_000_000).toFixed(1) + "M";
  if (Math.abs(v) >= 1_000)     return (v / 1_000).toFixed(1) + "k";
  return v.toFixed(2);
}

// 두 날짜 사이 평일(거래일) 수 (from 초과 ~ to 미만)
function countWeekdaysBetween(from: Date, to: Date): number {
  let count = 0;
  const d = new Date(from);
  d.setDate(d.getDate() + 1);
  while (d < to) {
    const dow = d.getDay();
    if (dow !== 0 && dow !== 6) count++;
    d.setDate(d.getDate() + 1);
  }
  return count;
}

// 거래정지 구간에 null 슬롯 삽입 — x축을 달력 시간에 비례하게 만듦
function expandBarsWithGaps(rawBars: PriceBar[]): (PriceBar | null)[] {
  const result: (PriceBar | null)[] = [];
  for (let i = 0; i < rawBars.length; i++) {
    if (i > 0) {
      const prev = new Date(rawBars[i - 1].date);
      const curr = new Date(rawBars[i].date);
      const missing = countWeekdaysBetween(prev, curr);
      if (missing > 5) {
        for (let j = 0; j < missing; j++) result.push(null);
      }
    }
    result.push(rawBars[i]);
  }
  return result;
}

// 두 값 배열 사이 영역을 crossover 기준으로 색상 분리해 채움
function crossoverFill(
  aVals: (number | undefined)[],
  bVals: (number | undefined)[],
  toYFn: (v: number) => number,
  xFn: (i: number) => number,
  colorAAbove: string,
  colorBAbove: string,
): ReactElement[] {
  const n = aVals.length;
  const paths: ReactElement[] = [];
  for (let i = 0; i < n - 1; i++) {
    const a0 = aVals[i], a1 = aVals[i + 1];
    const b0 = bVals[i], b1 = bVals[i + 1];
    if (a0 == null || a1 == null || b0 == null || b1 == null) continue;
    const x0 = xFn(i), x1 = xFn(i + 1);
    const ay0 = toYFn(a0), ay1 = toYFn(a1);
    const by0 = toYFn(b0), by1 = toYFn(b1);
    const aAbove0 = a0 > b0;
    const aAbove1 = a1 > b1;
    if (aAbove0 === aAbove1) {
      const c = aAbove0 ? colorAAbove : colorBAbove;
      if (!c) continue;
      paths.push(<path key={`co-${i}`} d={`M${x0} ${ay0}L${x1} ${ay1}L${x1} ${by1}L${x0} ${by0}Z`} fill={c} stroke="none" />);
    } else {
      const diff0 = a0 - b0, diff1 = a1 - b1;
      const t  = diff0 / (diff0 - diff1);
      const ix = x0 + t * (x1 - x0);
      const iy = toYFn(a0 + t * (a1 - a0));
      const c0 = aAbove0 ? colorAAbove : colorBAbove;
      const c1 = aAbove1 ? colorAAbove : colorBAbove;
      if (c0) paths.push(<path key={`co-${i}a`} d={`M${x0} ${ay0}L${ix} ${iy}L${x0} ${by0}Z`} fill={c0} stroke="none" />);
      if (c1) paths.push(<path key={`co-${i}b`} d={`M${ix} ${iy}L${x1} ${ay1}L${x1} ${by1}Z`} fill={c1} stroke="none" />);
    }
  }
  return paths;
}

export default function StockChart({ code, market, presetItems, panelOrder, mode }: Props) {
  const selectedIndicators = presetItems.filter(i => i.enabled).map(i => i.type);

  function itemStyle(type: IndicatorType) {
    const item = presetItems.find(i => i.type === type);
    return {
      color:     item?.color     ?? INDICATOR_META[type]?.color ?? "#94a3b8",
      lineWidth: item?.lineWidth ?? 1.5,
    };
  }

  const [bars, setBars]                           = useState<PriceBar[]>([]);
  const [expandedBars, setExpandedBars]           = useState<(PriceBar | null)[]>([]);
  const [indicatorData, setIndicatorData]         = useState<IndicatorBar[]>([]);
  const [indicatorLoading, setIndicatorLoading]   = useState(false);
  const [loading, setLoading]                     = useState(true);
  const [error, setError]                         = useState<string | null>(null);
  const [width, setWidth]                         = useState(0);
  const [visibleCount, setVisibleCount]           = useState(15);
  const [rightIndex, setRightIndex]               = useState(14);
  const [hoverIdx, setHoverIdx]                   = useState<number | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const vcRef    = useRef(15);
  const riRef    = useRef(14);
  const widthRef = useRef(0);
  const totalRef = useRef(0);

  useEffect(() => { vcRef.current    = visibleCount;       }, [visibleCount]);
  useEffect(() => { riRef.current    = rightIndex;         }, [rightIndex]);
  useEffect(() => { widthRef.current = width;              }, [width]);
  useEffect(() => { totalRef.current = expandedBars.length; }, [expandedBars]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setBars([]);
    setExpandedBars([]);
    fetch(`/api/stocks/${code}/prices?market=${market}&limit=120`)
      .then((r) => { if (!r.ok) throw new Error(); return r.json(); })
      .then((data: PriceBar[]) => {
        if (!cancelled) {
          const arr      = Array.isArray(data) ? data : [];
          const expanded = expandBarsWithGaps(arr);
          setBars(arr);
          setExpandedBars(expanded);
          totalRef.current = expanded.length;
          const plotW     = Math.max(1, widthRef.current - PAD.left - PAD.right);
          const defaultVc = Math.max(10, Math.round(plotW / 14));
          const vc = Math.max(2, Math.min(defaultVc, expanded.length, MAX_VISIBLE));
          setVisibleCount(vc); vcRef.current = vc;
          setRightIndex(expanded.length - 1); riRef.current = expanded.length - 1;
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) { setError("차트 데이터를 불러올 수 없습니다."); setLoading(false); }
      });
    return () => { cancelled = true; };
  }, [code, market]);

  useEffect(() => {
    if (selectedIndicators.length === 0) { setIndicatorData([]); return; }
    let cancelled = false;
    setIndicatorLoading(true);
    const types = selectedIndicators.join(",");
    fetch(`/api/stocks/${code}/indicators?market=${market}&limit=120&types=${types}`)
      .then((r) => r.ok ? r.json() : [])
      .then((data: IndicatorBar[]) => {
        if (!cancelled) {
          setIndicatorData(Array.isArray(data) ? data : []);
          setIndicatorLoading(false);
        }
      })
      .catch(() => { if (!cancelled) { setIndicatorData([]); setIndicatorLoading(false); } });
    return () => { cancelled = true; };
  }, [code, market, selectedIndicators]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([e]) => {
      const w = e.contentRect.width;
      setWidth(w); widthRef.current = w;
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const { handleTouchStart, handleTouchMove, handleTouchEnd, handlePointerMove, handlePointerLeave } =
    useChartInteraction({ containerRef, vcRef, riRef, widthRef, totalRef, setVisibleCount, setRightIndex, setHoverIdx });

  const startIdx    = Math.max(0, rightIndex - visibleCount + 1);
  const visibleSlots = expandedBars.slice(startIdx, rightIndex + 1);
  const hoveredBar   = hoverIdx !== null ? (visibleSlots[hoverIdx] ?? null) : null;

  const indicatorMap = new Map<string, Partial<Record<IndicatorType, number>>>();
  for (const bar of indicatorData) indicatorMap.set(bar.date, bar.values);

  const activePanels = new Set<PanelId>();
  for (const t of selectedIndicators) {
    const panel = INDICATOR_META[t]?.panel;
    if (panel && panel !== "OVERLAY") activePanels.add(panel);
  }
  const activePanelList = Array.from(activePanels);
  const subPanels = panelOrder
    ? [...panelOrder.filter(p => activePanels.has(p)), ...activePanelList.filter(p => !panelOrder.includes(p))]
    : activePanelList;
  const svgH = totalSvgH(subPanels.length);
  const hasIndicatorData = indicatorData.length > 0;

  function renderSVG() {
    if (width === 0) return null;

    const w     = width;
    const plotL = PAD.left;
    const plotR = w - PAD.right;
    const plotW = plotR - plotL;
    const cTop  = PAD.top;
    const cBot  = cTop + CANDLE_H;
    const vTop  = cBot + GAP_H;
    const vBot  = vTop + VOL_H;

    const n    = visibleSlots.length;
    const slot = plotW / n;
    const bw   = Math.max(1, slot - BAR_GAP);

    const xAt = (i: number) => plotL + i * slot + slot / 2;

    const realBars = visibleSlots.filter((s): s is PriceBar => s !== null);
    // 거래정지 구간만 보일 때(realBars 없음) → 전체 데이터 기준 가격/거래량 범위 유지
    const rangeBars = realBars.length > 0 ? realBars : bars;
    const pMax    = Math.max(...rangeBars.map(b => b.high));
    const pMin    = Math.min(...rangeBars.map(b => b.low));
    const pSpan   = (pMax - pMin) || pMax * 0.01 || 1;
    const pad     = pSpan * 0.5;
    const pBotVal = pMin - pad;
    const pRange  = pSpan + pad * 2;

    const toY    = (p: number) => cBot - ((p - pBotVal) / pRange) * CANDLE_H;
    const maxVol = Math.max(...rangeBars.map(b => b.volume)) || 1;
    const toVH   = (v: number) => Math.max(1, (v / maxVol) * VOL_H);

    const chartBot = subPanels.length > 0
      ? vBot + SCROLL_H + subPanels.length * (PANEL_H + PANEL_GAP)
      : vBot;

    const grid: ReactElement[] = [];
    for (let i = 0; i <= 4; i++) {
      const p = pBotVal + (pRange * i) / 4;
      const y = toY(p);
      if (y < cTop - 2 || y > cBot + 2) continue;
      grid.push(
        <g key={i}>
          <line x1={plotL} y1={y} x2={plotR} y2={y} stroke="rgba(255,255,255,0.05)" strokeWidth="1" />
          <text x={plotL - 4} y={y + 4} textAnchor="end" fontSize="11" fill="#94a3b8" fontFamily="ui-monospace, monospace">
            {fmtPrice(p)}
          </text>
        </g>
      );
    }

    const volLabels: ReactElement[] = [];
    for (let i = 0; i <= 2; i++) {
      const ratio = i / 2;
      const y     = vBot - ratio * VOL_H;
      volLabels.push(
        <g key={`vl${i}`}>
          <line x1={plotL} y1={y} x2={plotR} y2={y} stroke="rgba(255,255,255,0.04)" strokeWidth="1" />
          <text x={plotR + 4} y={y + 4} textAnchor="start" fontSize="11" fill="#94a3b8" fontFamily="ui-monospace, monospace">
            {fmtVol(maxVol * ratio)}
          </text>
        </g>
      );
    }

    const gapRegions: ReactElement[] = [];
    let gapStartIdx = -1;
    const flushGap = (endIdx: number) => {
      if (gapStartIdx === -1) return;
      const x1   = xAt(gapStartIdx) - slot / 2;
      const x2   = endIdx < n ? xAt(endIdx) - slot / 2 : plotR;
      const gw   = x2 - x1;
      const midX = (x1 + x2) / 2;
      gapRegions.push(
        <g key={`gr-${gapStartIdx}`}>
          <rect x={x1} y={cTop} width={gw} height={chartBot - cTop} fill="rgba(251,191,36,0.04)" />
          <line x1={x1} y1={cTop} x2={x1} y2={chartBot} stroke="rgba(251,191,36,0.28)" strokeWidth="1" strokeDasharray="3 3" />
          <line x1={x2} y1={cTop} x2={x2} y2={chartBot} stroke="rgba(251,191,36,0.28)" strokeWidth="1" strokeDasharray="3 3" />
          {gw > 44 && (
            <>
              <rect x={midX - 24} y={cTop + 6} width={48} height={15} rx="3" fill="rgba(15,23,42,0.9)" />
              <text x={midX} y={cTop + 18} textAnchor="middle" fontSize="10" fill="#fbbf24" fontFamily="sans-serif">거래정지</text>
            </>
          )}
        </g>
      );
      gapStartIdx = -1;
    };
    visibleSlots.forEach((s, i) => {
      if (s === null && gapStartIdx === -1) gapStartIdx = i;
      if (s !== null && gapStartIdx !== -1) flushGap(i);
    });
    flushGap(n);

    const bbFills: ReactElement[] = [];
    const hasBBOverlay = selectedIndicators.includes("BB_UPPER_20") && selectedIndicators.includes("BB_LOWER_20");
    if (hasBBOverlay) {
      const upperVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.BB_UPPER_20 : undefined);
      const lowerVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.BB_LOWER_20 : undefined);
      const closeVals = visibleSlots.map(s => s?.close);

      let upPts: string[] = [], dnPts: string[] = [];
      const flushBB = (key: string) => {
        if (upPts.length > 1)
          bbFills.push(<path key={`bb-${key}`} d={upPts.join("") + dnPts.slice().reverse().join("") + "Z"} fill="rgba(251,146,60,0.07)" stroke="none" />);
        upPts = []; dnPts = [];
      };
      visibleSlots.forEach((s, i) => {
        if (!s || upperVals[i] == null || lowerVals[i] == null) { flushBB(String(i)); return; }
        const cx = xAt(i);
        upPts.push(`${upPts.length ? "L" : "M"}${cx} ${toY(upperVals[i]!)}`);
        dnPts.push(`L${cx} ${toY(lowerVals[i]!)}`);
      });
      flushBB("end");

      bbFills.push(...crossoverFill(closeVals, upperVals, toY, xAt, "rgba(239,68,68,0.18)", ""));
      bbFills.push(...crossoverFill(lowerVals, closeVals, toY, xAt, "rgba(59,130,246,0.18)", ""));
    }

    const elems: ReactElement[] = [];
    let linePath  = '';
    let needsMove = true;
    visibleSlots.forEach((bar, i) => {
      if (!bar) { needsMove = true; return; }
      const cx     = xAt(i);
      const rising = bar.close >= bar.open;
      const color  = rising ? RISING : FALLING;
      linePath += needsMove ? `M ${cx} ${toY(bar.close)}` : ` L ${cx} ${toY(bar.close)}`;
      needsMove = false;
      if (mode === "candle") {
        const bodyT = toY(Math.max(bar.open, bar.close));
        const bodyB = toY(Math.min(bar.open, bar.close));
        elems.push(
          <g key={bar.date}>
            <line x1={cx} y1={toY(bar.high)} x2={cx} y2={toY(bar.low)} stroke={color} strokeWidth="1" />
            <rect x={cx - bw / 2} y={bodyT} width={bw} height={Math.max(1, bodyB - bodyT)} fill={color} />
          </g>
        );
      }
      elems.push(
        <rect key={`v${i}`} x={cx - bw / 2} y={vBot - toVH(bar.volume)} width={bw} height={toVH(bar.volume)} fill={color} opacity={0.4} />
      );
    });

    const overlayLines: ReactElement[] = [];
    for (const type of selectedIndicators.filter(t => INDICATOR_META[t]?.panel === "OVERLAY")) {
      const { color, lineWidth } = itemStyle(type);
      let path = '', nm = true;
      visibleSlots.forEach((s, i) => {
        if (!s) { nm = true; return; }
        const v = indicatorMap.get(s.date)?.[type];
        if (v == null) return;
        path += nm ? `M ${xAt(i)} ${toY(v)}` : ` L ${xAt(i)} ${toY(v)}`;
        nm = false;
      });
      if (path.length > 4)
        overlayLines.push(
          <path key={type} d={path} fill="none" stroke={color} strokeWidth={lineWidth}
                strokeLinejoin="round" strokeLinecap="round" opacity={0.85} />
        );
    }

    let crosshair: ReactElement | null = null;
    if (hoverIdx !== null && hoverIdx >= 0 && hoverIdx < n) {
      const bar = visibleSlots[hoverIdx];
      if (bar) {
        const hx = xAt(hoverIdx);
        const hy = toY(bar.close);
        crosshair = (
          <g style={{ pointerEvents: "none" }}>
            <line x1={hx} y1={cTop} x2={hx} y2={chartBot}
                  stroke="rgba(255,255,255,0.20)" strokeWidth="1" strokeDasharray="3 3" />
            <line x1={plotL} y1={hy} x2={plotR} y2={hy}
                  stroke="rgba(255,255,255,0.18)" strokeWidth="1" strokeDasharray="3 3" />
            <rect x={plotR + 2} y={hy - 8} width={PAD.right - 4} height={16} fill="#334155" rx="2" />
            <text x={plotR + 2 + (PAD.right - 4) / 2} y={hy + 4} textAnchor="middle" fontSize="10" fill="#e2e8f0" fontFamily="ui-monospace, monospace">
              {fmtPrice(bar.close)}
            </text>
          </g>
        );
      }
    }

    const panelDataList = subPanels.map((panelId, pi) => {
      const panelTop   = vBot + SCROLL_H + PANEL_GAP + pi * (PANEL_H + PANEL_GAP);
      const panelBot   = panelTop + PANEL_H;
      const panelTypes = selectedIndicators.filter(t => INDICATOR_META[t]?.panel === panelId);
      const allVals: number[] = [];
      visibleSlots.forEach(s => {
        if (!s) return;
        const v = indicatorMap.get(s.date);
        for (const t of panelTypes) { const val = v?.[t]; if (val != null) allVals.push(val); }
      });
      let pMin = allVals.length ? Math.min(...allVals) : 0;
      let pMax = allVals.length ? Math.max(...allVals) : 1;
      if (pMin === pMax) { pMin -= 1; pMax += 1; }
      const pad2   = (pMax - pMin) * 0.08;
      const yMin   = pMin - pad2;
      const yRange = pMax - pMin + pad2 * 2;
      const toYP   = (v: number) => panelBot - ((v - yMin) / yRange) * PANEL_H;
      return { panelId, panelTop, panelBot, panelTypes, allVals, pMin, pMax, yMin, yRange, toYP };
    });

    const panelBg: ReactElement[]       = [];
    const panelClipDefs: ReactElement[] = [];
    const panelLines: ReactElement[]    = [];

    panelDataList.forEach(({ panelId, panelTop, panelBot, panelTypes, allVals, pMin, pMax, yMin, yRange, toYP }) => {
      panelBg.push(
        <g key={`pbg-${panelId}`}>
          <rect x={plotL} y={panelTop} width={plotW} height={PANEL_H} fill="rgba(255,255,255,0.02)" rx="2" />
          <line x1={plotL} y1={panelTop} x2={plotR} y2={panelTop} stroke="rgba(255,255,255,0.08)" strokeWidth="1" />
          <text x={plotL + 4} y={panelTop + 12} fontSize="10" fill="#64748b">{PANEL_LABELS[panelId]}</text>
          {allVals.length > 0 && <>
            <text x={plotL - 4} y={panelTop + 12} textAnchor="end" fontSize="11" fill="#94a3b8" fontFamily="ui-monospace, monospace">{fmtVal(pMax)}</text>
            <text x={plotL - 4} y={panelBot - 2}  textAnchor="end" fontSize="11" fill="#94a3b8" fontFamily="ui-monospace, monospace">{fmtVal(pMin)}</text>
          </>}
          {!indicatorLoading && !hasIndicatorData && (
            <text x={plotL + plotW / 2} y={panelTop + PANEL_H / 2 + 4} textAnchor="middle" fontSize="11" fill="#475569">데이터 없음</text>
          )}
          {yMin < 0 && yMin + yRange > 0 && allVals.length > 0 && (
            <line x1={plotL} y1={toYP(0)} x2={plotR} y2={toYP(0)} stroke="rgba(255,255,255,0.12)" strokeWidth="1" strokeDasharray="2 2" />
          )}
        </g>
      );

      panelClipDefs.push(
        <clipPath key={`clip-${panelId}`} id={`clip-${panelId}`}>
          <rect x={plotL} y={panelTop} width={plotW} height={PANEL_H} />
        </clipPath>
      );

      const panelFills: ReactElement[] = [];

      if (panelId === "STOCH" && panelTypes.includes("STOCHASTIC_K_14_7") && panelTypes.includes("STOCHASTIC_D_14_7")) {
        const kVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.STOCHASTIC_K_14_7 : undefined);
        const dVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.STOCHASTIC_D_14_7 : undefined);
        panelFills.push(...crossoverFill(kVals, dVals, toYP, xAt, "rgba(239,68,68,0.22)", "rgba(59,130,246,0.22)"));
      }

      if (panelId === "ADX" && panelTypes.includes("PLUS_DI_14") && panelTypes.includes("MINUS_DI_14")) {
        const pVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.PLUS_DI_14 : undefined);
        const mVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.MINUS_DI_14 : undefined);
        panelFills.push(...crossoverFill(pVals, mVals, toYP, xAt, "rgba(74,222,128,0.18)", "rgba(248,113,113,0.18)"));
      }

      panelLines.push(
        <g key={`pl-${panelId}`} clipPath={`url(#clip-${panelId})`}>
          {panelFills}
          {panelTypes.map((type) => {
            const { color, lineWidth } = itemStyle(type);
            if (panelId === "MACD" && type === "MACD_HISTOGRAM") {
              return (
                <g key={type}>
                  {visibleSlots.map((s, i) => {
                    if (!s) return null;
                    const v = indicatorMap.get(s.date)?.[type];
                    if (v == null) return null;
                    const cx = xAt(i);
                    const y0 = toYP(0);
                    const y1 = toYP(v);
                    return (
                      <rect key={i} x={cx - bw / 2} y={Math.min(y0, y1)}
                            width={bw} height={Math.max(1, Math.abs(y0 - y1))}
                            fill={v >= 0 ? "#4ade80" : "#f87171"} opacity={0.6} />
                    );
                  })}
                </g>
              );
            }
            let pPath = '', pnm = true;
            visibleSlots.forEach((s, i) => {
              if (!s) { pnm = true; return; }
              const v = indicatorMap.get(s.date)?.[type];
              if (v == null) return;
              pPath += pnm ? `M ${xAt(i)} ${toYP(v)}` : ` L ${xAt(i)} ${toYP(v)}`;
              pnm = false;
            });
            if (pPath.length < 4) return null;
            return <path key={type} d={pPath} fill="none" stroke={color} strokeWidth={lineWidth} strokeLinejoin="round" strokeLinecap="round" />;
          })}
        </g>
      );
    });

    const panelHoverElems: ReactElement[] = [];
    if (hoverIdx !== null && hoverIdx >= 0 && hoverIdx < n && hoveredBar) {
      const vals = indicatorMap.get(hoveredBar.date);
      panelDataList.forEach(({ panelId, panelTop, panelBot, panelTypes, allVals, toYP }) => {
        if (allVals.length === 0) return;
        const hoverVals = panelTypes
          .map(t => ({ type: t, v: vals?.[t], ...itemStyle(t) }))
          .filter((x): x is { type: IndicatorType; v: number; color: string; lineWidth: number } => x.v != null);
        if (hoverVals.length === 0) return;
        panelHoverElems.push(
          <g key={`phov-${panelId}`} style={{ pointerEvents: "none" }}>
            <text x={plotR - 2} y={panelTop + 11} textAnchor="end"
                  fontSize="10" fill="#64748b" fontFamily="ui-monospace, monospace">
              {hoveredBar.date}
            </text>
            {hoverVals.map(({ type, v, color }, ti) => (
              <text key={`phv-${type}`} x={plotL + 4} y={panelTop + 12 + (ti + 1) * 13}
                    fontSize="10" fill={color} fontFamily="ui-monospace, monospace">
                {INDICATOR_META[type].label}: {fmtVal(v)}
              </text>
            ))}
            {hoverVals.map(({ type, v, color }, ti) => {
              const hy = toYP(v);
              if (hy < panelTop || hy > panelBot) return null;
              return (
                <g key={`phcr-${type}`}>
                  <line x1={plotL} y1={hy} x2={plotR} y2={hy}
                        stroke={color} strokeWidth="1" strokeDasharray="3 3" opacity={0.5} />
                  {ti === 0 && (
                    <>
                      <rect x={plotR + 2} y={hy - 8} width={PAD.right - 4} height={16} fill="#334155" rx="2" />
                      <text x={plotR + 2 + (PAD.right - 4) / 2} y={hy + 4}
                            textAnchor="middle" fontSize="10" fill={color}
                            fontFamily="ui-monospace, monospace">
                        {fmtVal(v)}
                      </text>
                    </>
                  )}
                </g>
              );
            })}
          </g>
        );
      });
    }

    return (
      <svg width={w} height={svgH} style={{ display: "block" }}>
        <defs>
          <clipPath id="plot-clip">
            <rect x={plotL} y={PAD.top} width={plotW} height={cBot - PAD.top + VOL_H + GAP_H} />
          </clipPath>
          {panelClipDefs}
        </defs>
        {grid}
        {volLabels}
        {gapRegions}
        {panelBg}
        <g clipPath="url(#plot-clip)">
          {bbFills}
          {elems}
          {mode === "line" && (
            <path d={linePath} fill="none" stroke={LINE_COLOR} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
          )}
          {overlayLines}
        </g>
        {panelLines}
        {crosshair}
        {panelHoverElems}
      </svg>
    );
  }

  const scrollMin      = visibleCount - 1;
  const scrollMax      = expandedBars.length - 1;
  const scrollDisabled = expandedBars.length <= visibleCount;

  return (
    <div className="select-none">
      {/* 차트 */}
      <div
        ref={containerRef}
        className="w-full relative"
        style={{ height: svgH, touchAction: "none" }}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onPointerMove={handlePointerMove}
        onPointerLeave={handlePointerLeave}
      >
        {loading  && <div className="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">로딩 중...</div>}
        {error    && <div className="absolute inset-0 flex items-center justify-center text-red-400/80 text-sm">{error}</div>}
        {!loading && !error && bars.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center text-slate-600 text-sm">데이터 없음</div>
        )}
        {!loading && !error && width > 0 && renderSVG()}

        {/* 스크롤바 */}
        <div
          className="absolute"
          style={{ top: PRICE_BOT + 2, left: PAD.left, right: PAD.right }}
          onPointerMove={e => e.stopPropagation()}
          onTouchStart={e => e.stopPropagation()}
        >
          <input
            type="range"
            min={scrollMin}
            max={scrollMax > scrollMin ? scrollMax : scrollMin + 1}
            value={rightIndex}
            disabled={scrollDisabled}
            onChange={(e) => {
              const v = Number(e.target.value);
              setRightIndex(v); riRef.current = v;
            }}
            className="w-full h-1 accent-slate-500 cursor-pointer disabled:opacity-20 disabled:cursor-default"
          />
        </div>

        {/* OHLCV 툴팁 */}
        {hoveredBar && hoverIdx !== null && (() => {
          const nv     = visibleSlots.length;
          const onLeft = hoverIdx / nv > 0.55;
          const rising = hoveredBar.close >= hoveredBar.open;
          const vals   = indicatorMap.get(hoveredBar.date);
          const overlayActive = selectedIndicators.filter(t => INDICATOR_META[t]?.panel === "OVERLAY");
          return (
            <div
              className="absolute top-3 pointer-events-none z-10"
              style={onLeft ? { left: PAD.left + 6 } : { right: PAD.right + 6 }}
            >
              <div className="bg-slate-900/90 border border-white/10 rounded-lg px-3 py-2 text-xs font-mono backdrop-blur-sm min-w-[130px]">
                <p className="text-slate-400 mb-1.5 text-center">{hoveredBar.date}</p>
                <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                  <span className="text-slate-500">시가</span>
                  <span className="text-right text-slate-200">{hoveredBar.open.toLocaleString()}</span>
                  <span className="text-slate-500">고가</span>
                  <span className="text-right text-red-400">{hoveredBar.high.toLocaleString()}</span>
                  <span className="text-slate-500">저가</span>
                  <span className="text-right text-blue-400">{hoveredBar.low.toLocaleString()}</span>
                  <span className="text-slate-500">종가</span>
                  <span className={`text-right font-semibold ${rising ? "text-red-400" : "text-blue-400"}`}>{hoveredBar.close.toLocaleString()}</span>
                  <span className="text-slate-500 mt-0.5">거래량</span>
                  <span className="text-right text-slate-300 mt-0.5">{hoveredBar.volume.toLocaleString()}</span>
                  {overlayActive.map(t => {
                    const v = vals?.[t];
                    if (v == null) return null;
                    const { color } = itemStyle(t);
                    return [
                      <span key={`${t}-l`} className="text-slate-500" style={{ color }}>{INDICATOR_META[t].label}</span>,
                      <span key={`${t}-v`} className="text-right text-slate-200">{fmtVal(v)}</span>,
                    ];
                  })}
                </div>
              </div>
            </div>
          );
        })()}
      </div>
    </div>
  );
}
