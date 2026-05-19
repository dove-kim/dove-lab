"use client";

import { memo, useEffect, useMemo, useRef, useState } from "react";
import { perf } from "@/utils/perf";
import {
  PAD, CANDLE_H, GAP_H, VOL_H, SCROLL_H, PANEL_H, PANEL_GAP,
  PRICE_BOT, MAX_VISIBLE, BAR_GAP, totalSvgH,
} from "./chartConstants";
import { useChartInteraction } from "./useChartInteraction";
import { INDICATOR_META, PANEL_LABELS, type PanelId } from "./indicatorMeta";
import type { IndicatorType } from "@/types/filter";
import type { IndicatorPresetItem } from "@/types/indicator-preset";

// ── 클라이언트 캐시 (모듈 레벨 — 페이지 생존 주기 동안 유지) ────────────────────
// 같은 종목 재방문 시 네트워크 없이 즉시 표시한다.
//
// LRU 정책: 최대 CACHE_MAX개까지 보관. 초과 시 가장 오래전에 쓴 항목 삭제.
// Map은 삽입 순서를 유지하므로 첫 번째 키 = 가장 오래된 항목.
//
// In-flight 추적: fetch 진행 중인 키를 Set으로 관리해 중복 요청 방지.
// (캐시 SET 완료 전에 동일 종목을 다시 hover해도 요청이 한 번만 나간다.)

const CACHE_MAX = 50; // 종목당 ~40KB × 50 ≈ 2MB 이하로 유지

interface PriceSnapshot {
  bars:         PriceBar[];
  expandedBars: (PriceBar | null)[];
}

const _priceCache     = new Map<string, PriceSnapshot>(); // key: `${code}|${market}`
const _indicatorCache = new Map<string, IndicatorBar[]>(); // key: `${code}|${market}|${typesKey}`
const _priceInflight  = new Set<string>();                 // fetch 진행 중인 가격 키
const _indicatorInflight = new Set<string>();              // fetch 진행 중인 지표 키

function _cacheKey(code: string, market: string)                    { return `${code}|${market}`; }
function _iCacheKey(code: string, market: string, typesKey: string) { return `${code}|${market}|${typesKey}`; }

/** LRU 삽입: 캐시 최대치 초과 시 첫 번째(가장 오래된) 항목 제거 후 추가 */
function _lruSet<V>(map: Map<string, V>, key: string, value: V) {
  map.delete(key); // 이미 있으면 제거 후 재삽입 → 최신으로 갱신
  if (map.size >= CACHE_MAX) map.delete(map.keys().next().value!);
  map.set(key, value);
}

/**
 * 마우스 hover 시 StockSearchLayout에서 호출해 미리 fetch한다.
 * 캐시 HIT이거나 이미 fetch 중이면 아무것도 하지 않는다.
 */
export function prefetchChart(code: string, market: string, indicatorsKey: string) {
  const pk = _cacheKey(code, market);
  if (!_priceCache.has(pk) && !_priceInflight.has(pk)) {
    _priceInflight.add(pk);
    fetch(`/api/stocks/${code}/prices?market=${market}&limit=120`)
      .then(r => r.ok ? r.json() : null)
      .then((data: PriceBar[] | null) => {
        if (data) {
          const bars = Array.isArray(data) ? data : [];
          _lruSet(_priceCache, pk, { bars, expandedBars: expandBarsWithGaps(bars) });
        }
      })
      .catch(() => {})
      .finally(() => _priceInflight.delete(pk));
  }
  if (indicatorsKey) {
    const ik = _iCacheKey(code, market, indicatorsKey);
    if (!_indicatorCache.has(ik) && !_indicatorInflight.has(ik)) {
      _indicatorInflight.add(ik);
      fetch(`/api/stocks/${code}/indicators?market=${market}&limit=120&types=${indicatorsKey}`)
        .then(r => r.ok ? r.json() : null)
        .then((data: IndicatorBar[] | null) => {
          if (data) _lruSet(_indicatorCache, ik, Array.isArray(data) ? data : []);
        })
        .catch(() => {})
        .finally(() => _indicatorInflight.delete(ik));
    }
  }
}

export interface PriceBar {
  date: string;
  status: "TRADING" | "HALTED" | "DELISTED";
  open: number | null;
  high: number | null;
  low: number | null;
  close: number | null;
  volume: number | null;
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
const MONO_FONT  = "11px ui-monospace, monospace";

// ── 포맷 헬퍼 ────────────────────────────────────────────────────────────────

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

function isHaltBar(bar: PriceBar): boolean    { return bar.status === "HALTED"; }
function isDelistedBar(bar: PriceBar): boolean { return bar.status === "DELISTED"; }

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

// ── Canvas 크로스오버 fill ───────────────────────────────────────────────────

function drawCrossoverFill(
  ctx: CanvasRenderingContext2D,
  aVals: (number | undefined)[],
  bVals: (number | undefined)[],
  toYFn: (v: number) => number,
  xFn: (i: number) => number,
  colorAAbove: string,
  colorBAbove: string,
) {
  const n = aVals.length;
  for (let i = 0; i < n - 1; i++) {
    const a0 = aVals[i], a1 = aVals[i + 1];
    const b0 = bVals[i], b1 = bVals[i + 1];
    if (a0 == null || a1 == null || b0 == null || b1 == null) continue;
    const x0 = xFn(i), x1 = xFn(i + 1);
    const ay0 = toYFn(a0), ay1 = toYFn(a1);
    const by0 = toYFn(b0), by1 = toYFn(b1);
    const aAbove0 = a0 > b0, aAbove1 = a1 > b1;
    if (aAbove0 === aAbove1) {
      const c = aAbove0 ? colorAAbove : colorBAbove;
      if (!c) continue;
      ctx.fillStyle = c;
      ctx.beginPath();
      ctx.moveTo(x0, ay0); ctx.lineTo(x1, ay1);
      ctx.lineTo(x1, by1); ctx.lineTo(x0, by0);
      ctx.closePath(); ctx.fill();
    } else {
      const diff0 = a0 - b0, diff1 = a1 - b1;
      const t  = diff0 / (diff0 - diff1);
      const ix = x0 + t * (x1 - x0);
      const iy = toYFn(a0 + t * (a1 - a0));
      const c0 = aAbove0 ? colorAAbove : colorBAbove;
      const c1 = aAbove1 ? colorAAbove : colorBAbove;
      if (c0) {
        ctx.fillStyle = c0;
        ctx.beginPath();
        ctx.moveTo(x0, ay0); ctx.lineTo(ix, iy); ctx.lineTo(x0, by0);
        ctx.closePath(); ctx.fill();
      }
      if (c1) {
        ctx.fillStyle = c1;
        ctx.beginPath();
        ctx.moveTo(ix, iy); ctx.lineTo(x1, ay1); ctx.lineTo(x1, by1);
        ctx.closePath(); ctx.fill();
      }
    }
  }
}

// ── 공통 레이아웃 계산 ────────────────────────────────────────────────────────

interface ChartLayout {
  plotL: number; plotR: number; plotW: number;
  cTop: number;  cBot: number;
  vTop: number;  vBot: number;
  n: number; slot: number; bw: number;
  xAt: (i: number) => number;
  toY:  (p: number) => number;
  toVH: (v: number) => number;
  pBotVal: number; pRange: number;
  maxVol: number;
  chartBot: number;
}

function computeLayout(
  width: number,
  visibleSlots: (PriceBar | null)[],
  bars: PriceBar[],
  subPanelCount: number,
  svgH: number,
): ChartLayout {
  const plotL = PAD.left, plotR = width - PAD.right, plotW = plotR - plotL;
  const cTop  = PAD.top,  cBot  = cTop + CANDLE_H;
  const vTop  = cBot + GAP_H, vBot = vTop + VOL_H;

  const n    = visibleSlots.length;
  const slot = plotW / Math.max(1, n);
  const bw   = Math.max(1, slot - BAR_GAP);
  const xAt  = (i: number) => plotL + i * slot + slot / 2;

  const realBars   = visibleSlots.filter((s): s is PriceBar => s !== null);
  const activeBars = realBars.filter(b => !isHaltBar(b) && !isDelistedBar(b));
  const haltBars   = realBars.filter(isHaltBar);
  const rangeBars  = activeBars.length > 0
    ? activeBars
    : (realBars.filter(b => !isDelistedBar(b)).length > 0 ? realBars.filter(b => !isDelistedBar(b)) : bars);

  const candidates = [
    ...rangeBars.map(b => b.high).filter((v): v is number => v !== null),
    ...haltBars.map(b => b.close).filter((v): v is number => v !== null),
  ];
  const pMaxRaw = candidates.length ? Math.max(...candidates) : 0;
  const pMinRaw = [
    ...rangeBars.map(b => b.low).filter((v): v is number => v !== null),
    ...haltBars.map(b => b.close).filter((v): v is number => v !== null),
  ].reduce((a, b) => Math.min(a, b), pMaxRaw);

  const pSpan   = (pMaxRaw - pMinRaw) || pMaxRaw * 0.01 || 1;
  const pad     = pSpan * 0.5;
  const pBotVal = pMinRaw - pad;
  const pRange  = pSpan + pad * 2;

  const toY   = (p: number) => cBot - ((p - pBotVal) / pRange) * CANDLE_H;
  const maxVol = Math.max(...rangeBars.map(b => b.volume ?? 0)) || 1;
  const toVH   = (v: number) => Math.max(1, (v / maxVol) * VOL_H);

  const chartBot = subPanelCount > 0
    ? vBot + SCROLL_H + subPanelCount * (PANEL_H + PANEL_GAP)
    : vBot;

  return { plotL, plotR, plotW, cTop, cBot, vTop, vBot, n, slot, bw, xAt, toY, toVH, pBotVal, pRange, maxVol, chartBot };
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

function StockChart({ code, market, presetItems, panelOrder, mode }: Props) {
  const selectedIndicatorsKey = useMemo(
    () => presetItems.filter(i => i.enabled).map(i => i.type).join(","),
    [presetItems],
  );
  const selectedIndicators = useMemo(
    () => selectedIndicatorsKey ? (selectedIndicatorsKey.split(",") as IndicatorType[]) : [],
    [selectedIndicatorsKey],
  );

  function itemStyle(type: IndicatorType) {
    const item = presetItems.find(i => i.type === type);
    return {
      color:     item?.color     ?? INDICATOR_META[type]?.color ?? "#94a3b8",
      lineWidth: item?.lineWidth ?? 1.5,
    };
  }

  const [bars, setBars]                         = useState<PriceBar[]>([]);
  const [expandedBars, setExpandedBars]         = useState<(PriceBar | null)[]>([]);
  const [indicatorData, setIndicatorData]       = useState<IndicatorBar[]>([]);
  const [indicatorLoading, setIndicatorLoading] = useState(false);
  const [loading, setLoading]                   = useState(true);
  const [error, setError]                       = useState<string | null>(null);
  const [width, setWidth]                       = useState(0);
  const [visibleCount, setVisibleCount]         = useState(15);
  const [rightIndex, setRightIndex]             = useState(14);
  const [hoverIdx, setHoverIdx]                 = useState<number | null>(null);

  const containerRef         = useRef<HTMLDivElement>(null);
  const staticCanvasRef      = useRef<HTMLCanvasElement>(null);
  const overlayCanvasRef     = useRef<HTMLCanvasElement>(null);
  const staticRafRef         = useRef(0);
  const overlayRafRef        = useRef(0);
  // 스크롤/줌 시 React 리렌더 없이 직접 canvas를 다시 그리기 위한 트리거
  const triggerStaticDrawRef = useRef<() => void>(() => {});

  // 마운트 타이밍
  const mountT0 = useRef(perf.start());
  useEffect(() => { perf.end("Mount", `StockChart[${code}] mount`, mountT0.current); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ③ 컴포넌트가 새 code로 렌더됐을 때 (React commit 완료 시점)
  const prevCodeRef = useRef(code);
  useEffect(() => {
    if (prevCodeRef.current !== code) {
      perf.pipe.mark(`③ StockChart useEffect[code] — React commit 완료 (code=${code})`);
      prevCodeRef.current = code;
    }
  }, [code]);

  // useChartInteraction에서 사용하는 ref 미러
  const vcRef    = useRef(15);
  const riRef    = useRef(14);
  const widthRef = useRef(0);
  const totalRef = useRef(0);

  useEffect(() => { vcRef.current    = visibleCount;        }, [visibleCount]);
  useEffect(() => { riRef.current    = rightIndex;          }, [rightIndex]);
  useEffect(() => { widthRef.current = width;               }, [width]);
  useEffect(() => { totalRef.current = expandedBars.length; }, [expandedBars]);

  // ── 데이터 fetch ────────────────────────────────────────────────────────────

  useEffect(() => {
    const controller = new AbortController();
    perf.pipe.mark("④ prices useEffect 시작");

    function applyPriceData(arr: PriceBar[], fromCache: boolean) {
      const expanded = fromCache ? _priceCache.get(_cacheKey(code, market))!.expandedBars
                                 : expandBarsWithGaps(arr);
      const plotW     = Math.max(1, widthRef.current - PAD.left - PAD.right);
      const defaultVc = Math.max(10, Math.round(plotW / 14));
      const vc        = Math.max(2, Math.min(defaultVc, expanded.length, MAX_VISIBLE));
      setBars(arr);
      setExpandedBars(expanded);
      totalRef.current  = expanded.length;
      setVisibleCount(vc); vcRef.current = vc;
      setRightIndex(expanded.length - 1); riRef.current = expanded.length - 1;
      setLoading(false);
      perf.pipe.mark(`⑥ setState 완료 (${fromCache ? "캐시 HIT ⚡" : "네트워크"})`);
    }

    // 캐시 HIT → 즉시 표시, fetch 없음
    const cached = _priceCache.get(_cacheKey(code, market));
    if (cached) {
      perf.pipe.mark("⑤ 캐시 HIT — 즉시 반환");
      applyPriceData(cached.bars, true);
      return () => {};  // cleanup 불필요
    }

    // 캐시 MISS → 네트워크
    setLoading(true);
    setError(null);
    perf.measure("API", `GET /api/stocks/${code}/prices?market=${market}&limit=120`, () =>
      fetch(`/api/stocks/${code}/prices?market=${market}&limit=120`, { signal: controller.signal })
        .then(r => { if (!r.ok) throw new Error(); return r; })
    ).then(r => r.json())
      .then((data: PriceBar[]) => {
        perf.pipe.mark("⑤ prices fetch 완료");
        const arr      = Array.isArray(data) ? data : [];
        const expanded = expandBarsWithGaps(arr);
        _lruSet(_priceCache, _cacheKey(code, market), { bars: arr, expandedBars: expanded });
        applyPriceData(arr, false);
      })
      .catch((e: unknown) => {
        if (e instanceof Error && e.name === "AbortError") return;
        setError("차트 데이터를 불러올 수 없습니다."); setLoading(false);
      });
    return () => controller.abort();
  }, [code, market]);

  useEffect(() => {
    if (selectedIndicatorsKey === "") { setIndicatorData([]); return; }

    // 캐시 HIT
    const ik     = _iCacheKey(code, market, selectedIndicatorsKey);
    const cached = _indicatorCache.get(ik);
    if (cached) {
      setIndicatorData(cached);
      setIndicatorLoading(false);
      return;
    }

    // 캐시 MISS
    const controller = new AbortController();
    setIndicatorLoading(true);
    perf.measure("API", `GET /api/stocks/${code}/indicators?market=${market}&limit=120&types=${selectedIndicatorsKey}`, () =>
      fetch(`/api/stocks/${code}/indicators?market=${market}&limit=120&types=${selectedIndicatorsKey}`, { signal: controller.signal })
        .then(r => { if (!r.ok) throw new Error(); return r; })
    ).then(r => r.json())
      .then((data: IndicatorBar[]) => {
        const arr = Array.isArray(data) ? data : [];
        _lruSet(_indicatorCache, ik, arr);
        setIndicatorData(arr);
        setIndicatorLoading(false);
      })
      .catch((e: unknown) => {
        if (e instanceof Error && e.name === "AbortError") return;
        setIndicatorData([]); setIndicatorLoading(false);
      });
    return () => controller.abort();
  }, [code, market, selectedIndicatorsKey]);

  // ── ResizeObserver (debounce 50ms) ──────────────────────────────────────────

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    let timer: ReturnType<typeof setTimeout>;
    const ro = new ResizeObserver(([e]) => {
      const w = Math.round(e.contentRect.width);
      // CSS 크기를 즉시 반영해 레이아웃 깜빡임 방지 (bitmap 재계산은 debounce)
      if (staticCanvasRef.current)  staticCanvasRef.current.style.width  = `${w}px`;
      if (overlayCanvasRef.current) overlayCanvasRef.current.style.width = `${w}px`;
      clearTimeout(timer);
      timer = setTimeout(() => {
        widthRef.current = w;
        setWidth(w);
      }, 50);
    });
    ro.observe(el);
    return () => { ro.disconnect(); clearTimeout(timer); };
  }, []);

  // ── 뷰포트 파생 상태 ─────────────────────────────────────────────────────────

  const startIdx     = Math.max(0, rightIndex - visibleCount + 1);
  const visibleSlots = expandedBars.slice(startIdx, rightIndex + 1);
  const hoveredBar   = hoverIdx !== null ? (visibleSlots[hoverIdx] ?? null) : null;

  const indicatorMap = useMemo(() => {
    const m = new Map<string, Partial<Record<IndicatorType, number>>>();
    for (const bar of indicatorData) m.set(bar.date, bar.values);
    return m;
  }, [indicatorData]);

  const activePanels = useMemo(() => {
    const s = new Set<PanelId>();
    for (const t of selectedIndicators) {
      const panel = INDICATOR_META[t]?.panel;
      if (panel && panel !== "OVERLAY") s.add(panel);
    }
    return s;
  }, [selectedIndicators]);

  const subPanels = useMemo(() => {
    const list = Array.from(activePanels);
    return panelOrder
      ? [...panelOrder.filter(p => activePanels.has(p)), ...list.filter(p => !panelOrder!.includes(p))]
      : list;
  }, [activePanels, panelOrder]);

  const svgH = totalSvgH(subPanels.length);

  // ── Canvas draw: 정적 레이어 ─────────────────────────────────────────────────

  useEffect(() => {
    if (width === 0 || loading) return;
    perf.pipe.mark("⑦ draw useEffect 진입 (setState → useEffect 지연)");

    // 스크롤/줌 중에는 React 리렌더 없이 이 함수를 직접 호출한다.
    // riRef / vcRef에서 뷰포트를 읽으므로 항상 최신 상태를 반영한다.
    function trigger() {
      cancelAnimationFrame(staticRafRef.current);
      staticRafRef.current = requestAnimationFrame(() => {
        perf.pipe.mark("⑧ rAF 콜백 진입 (useEffect → rAF 지연)");
        const canvas = staticCanvasRef.current;
        if (!canvas) return;
        const _t0 = perf.start();
        const dpr = Math.min(window.devicePixelRatio || 1, 2); // DPR 2 이상은 cap (모바일 9× 방지)
        const targetW = Math.round(width * dpr);
        const targetH = Math.round(svgH  * dpr);
        // 스크롤 중에는 크기가 바뀌지 않으므로 불필요한 canvas 리셋 방지
        if (canvas.width !== targetW || canvas.height !== targetH) {
          canvas.width  = targetW;
          canvas.height = targetH;
          canvas.style.width  = `${width}px`;
          canvas.style.height = `${svgH}px`;
        }
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

        // refs에서 직접 뷰포트 계산 — React 상태(rightIndex/visibleCount)를 쓰지 않음
        const ri = riRef.current;
        const vc = vcRef.current;
        const visibleSlots = expandedBars.slice(Math.max(0, ri - vc + 1), ri + 1);

        const L = computeLayout(width, visibleSlots, bars, subPanels.length, svgH);
      const { plotL, plotR, plotW, cTop, cBot, vTop, vBot, n, slot, bw, xAt, toY, toVH, pBotVal, pRange, maxVol, chartBot } = L;

      ctx.clearRect(0, 0, width, svgH);

      // ── 그리드 + 가격 라벨 ──────────────────────────────────────────────────
      ctx.font = MONO_FONT;
      ctx.fillStyle = "#94a3b8";
      ctx.textBaseline = "middle";
      for (let i = 0; i <= 4; i++) {
        const p = pBotVal + (pRange * i) / 4;
        const y = toY(p);
        if (y < cTop - 2 || y > cBot + 2) continue;
        ctx.strokeStyle = "rgba(255,255,255,0.05)";
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(plotL, y); ctx.lineTo(plotR, y); ctx.stroke();
        ctx.textAlign = "right";
        ctx.fillText(fmtPrice(p), plotL - 4, y);
      }

      // ── 거래량 라벨 ─────────────────────────────────────────────────────────
      for (let i = 0; i <= 2; i++) {
        const ratio = i / 2;
        const y = vBot - ratio * VOL_H;
        ctx.strokeStyle = "rgba(255,255,255,0.04)";
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(plotL, y); ctx.lineTo(plotR, y); ctx.stroke();
        ctx.textAlign = "left";
        ctx.fillText(fmtVol(maxVol * ratio), plotR + 4, y);
      }

      // ── 갭 구간 (데이터 없음) ───────────────────────────────────────────────
      let gapStart = -1;
      const flushGap = (endIdx: number) => {
        if (gapStart === -1) return;
        const x1  = xAt(gapStart) - slot / 2;
        const x2  = endIdx < n ? xAt(endIdx) - slot / 2 : plotR;
        const gw  = x2 - x1;
        const midX = (x1 + x2) / 2;
        ctx.fillStyle = "rgba(251,191,36,0.04)";
        ctx.fillRect(x1, cTop, gw, chartBot - cTop);
        ctx.strokeStyle = "rgba(251,191,36,0.28)";
        ctx.lineWidth = 1;
        ctx.setLineDash([3, 3]);
        ctx.beginPath(); ctx.moveTo(x1, cTop); ctx.lineTo(x1, chartBot); ctx.stroke();
        ctx.beginPath(); ctx.moveTo(x2, cTop); ctx.lineTo(x2, chartBot); ctx.stroke();
        ctx.setLineDash([]);
        if (gw > 44) {
          ctx.fillStyle = "rgba(15,23,42,0.9)";
          ctx.beginPath();
          ctx.roundRect(midX - 28, cTop + 6, 56, 15, 3);
          ctx.fill();
          ctx.font = "10px sans-serif";
          ctx.fillStyle = "#fbbf24";
          ctx.textAlign = "center";
          ctx.textBaseline = "middle";
          ctx.fillText("데이터 없음", midX, cTop + 13.5);
          ctx.font = MONO_FONT;
        }
        gapStart = -1;
      };
      visibleSlots.forEach((s, i) => {
        if (s === null && gapStart === -1) gapStart = i;
        if (s !== null && gapStart !== -1) flushGap(i);
      });
      flushGap(n);

      // ── 볼린저밴드 fill ──────────────────────────────────────────────────────
      const hasBBOverlay = selectedIndicators.includes("BB_UPPER_20") && selectedIndicators.includes("BB_LOWER_20");
      if (hasBBOverlay) {
        const upperVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.BB_UPPER_20 : undefined);
        const lowerVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.BB_LOWER_20 : undefined);
        const closeVals = visibleSlots.map(s => s?.close ?? undefined);

        // BB 밴드 영역
        let upPts: [number, number][] = [], dnPts: [number, number][] = [];
        const flushBB = () => {
          if (upPts.length > 1) {
            ctx.fillStyle = "rgba(251,146,60,0.07)";
            ctx.beginPath();
            ctx.moveTo(upPts[0][0], upPts[0][1]);
            for (let i = 1; i < upPts.length; i++) ctx.lineTo(upPts[i][0], upPts[i][1]);
            for (let i = dnPts.length - 1; i >= 0; i--) ctx.lineTo(dnPts[i][0], dnPts[i][1]);
            ctx.closePath(); ctx.fill();
          }
          upPts = []; dnPts = [];
        };
        visibleSlots.forEach((s, i) => {
          if (!s || upperVals[i] == null || lowerVals[i] == null) { flushBB(); return; }
          upPts.push([xAt(i), toY(upperVals[i]!)]);
          dnPts.push([xAt(i), toY(lowerVals[i]!)]);
        });
        flushBB();

        drawCrossoverFill(ctx, closeVals, upperVals, toY, xAt, "rgba(239,68,68,0.18)", "");
        drawCrossoverFill(ctx, lowerVals, closeVals, toY, xAt, "rgba(59,130,246,0.18)", "");
      }

      // ── 가격 차트 클립 영역 적용 ─────────────────────────────────────────────
      ctx.save();
      ctx.beginPath();
      ctx.rect(plotL, PAD.top, plotW, cBot - PAD.top + VOL_H + GAP_H);
      ctx.clip();

      // ── 캔들 / 라인 + 거래량 ────────────────────────────────────────────────
      let linePath: [number, number][] = [];
      let needsMove = true;
      visibleSlots.forEach((bar, i) => {
        if (!bar) { needsMove = true; return; }
        const cx = xAt(i);

        if (isDelistedBar(bar)) {
          needsMove = true;
          ctx.strokeStyle = "#ef4444";
          ctx.lineWidth = 1.5;
          ctx.globalAlpha = 0.45;
          ctx.beginPath(); ctx.moveTo(cx, cBot - 6); ctx.lineTo(cx, cBot); ctx.stroke();
          ctx.globalAlpha = 1;
          return;
        }

        const rising = (bar.close ?? 0) >= (bar.open ?? bar.close ?? 0);
        const color  = rising ? RISING : FALLING;

        if (needsMove) { linePath = [[cx, toY(bar.close!)]]; needsMove = false; }
        else { linePath.push([cx, toY(bar.close!)]); }

        if (isHaltBar(bar)) {
          const cy = toY(bar.close!);
          ctx.strokeStyle = "#64748b";
          ctx.lineWidth = 1.5;
          ctx.globalAlpha = 0.7;
          ctx.setLineDash([2, 2]);
          ctx.beginPath(); ctx.moveTo(cx - bw / 2, cy); ctx.lineTo(cx + bw / 2, cy); ctx.stroke();
          ctx.setLineDash([]); ctx.globalAlpha = 1;
        } else {
          if (mode === "candle") {
            const bodyT = toY(Math.max(bar.open!, bar.close!));
            const bodyB = toY(Math.min(bar.open!, bar.close!));
            ctx.strokeStyle = color; ctx.lineWidth = 1;
            ctx.beginPath(); ctx.moveTo(cx, toY(bar.high!)); ctx.lineTo(cx, toY(bar.low!)); ctx.stroke();
            ctx.fillStyle = color;
            ctx.fillRect(cx - bw / 2, bodyT, bw, Math.max(1, bodyB - bodyT));
          }
          // 거래량
          ctx.fillStyle = color;
          ctx.globalAlpha = 0.4;
          const vh = toVH(bar.volume ?? 0);
          ctx.fillRect(cx - bw / 2, vBot - vh, bw, vh);
          ctx.globalAlpha = 1;
        }
      });

      // 라인 차트
      if (mode === "line" && linePath.length > 1) {
        ctx.strokeStyle = LINE_COLOR;
        ctx.lineWidth = 1.5;
        ctx.lineJoin = "round";
        ctx.lineCap  = "round";
        ctx.beginPath();
        ctx.moveTo(linePath[0][0], linePath[0][1]);
        for (let i = 1; i < linePath.length; i++) ctx.lineTo(linePath[i][0], linePath[i][1]);
        ctx.stroke();
      }

      // ── 오버레이 지표선 (이평선, BB 등) ─────────────────────────────────────
      for (const type of selectedIndicators.filter(t => INDICATOR_META[t]?.panel === "OVERLAY")) {
        const { color, lineWidth } = itemStyle(type);
        let path: [number, number][] = []; let nm = true;
        visibleSlots.forEach((s, i) => {
          if (!s) { nm = true; return; }
          const v = indicatorMap.get(s.date)?.[type];
          if (v == null) return;
          if (nm) { path = [[xAt(i), toY(v)]]; nm = false; }
          else { path.push([xAt(i), toY(v)]); }
        });
        if (path.length < 2) continue;
        ctx.strokeStyle = color; ctx.lineWidth = lineWidth;
        ctx.globalAlpha = 0.85;
        ctx.lineJoin = "round"; ctx.lineCap = "round";
        ctx.beginPath();
        ctx.moveTo(path[0][0], path[0][1]);
        for (let i = 1; i < path.length; i++) ctx.lineTo(path[i][0], path[i][1]);
        ctx.stroke();
        ctx.globalAlpha = 1;
      }

      ctx.restore(); // 클립 해제

      // ── 보조지표 패널 ────────────────────────────────────────────────────────
      subPanels.forEach((panelId, pi) => {
        const panelTop  = vBot + SCROLL_H + PANEL_GAP + pi * (PANEL_H + PANEL_GAP);
        const panelBot  = panelTop + PANEL_H;
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

        // 패널 배경
        ctx.fillStyle = "rgba(255,255,255,0.02)";
        ctx.beginPath();
        ctx.roundRect(plotL, panelTop, plotW, PANEL_H, 2);
        ctx.fill();
        ctx.strokeStyle = "rgba(255,255,255,0.08)";
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(plotL, panelTop); ctx.lineTo(plotR, panelTop); ctx.stroke();

        // 패널 라벨
        ctx.font = "10px sans-serif";
        ctx.fillStyle = "#64748b";
        ctx.textAlign = "left"; ctx.textBaseline = "top";
        ctx.fillText(PANEL_LABELS[panelId], plotL + 4, panelTop + 2);

        // 값 범위 라벨
        if (allVals.length > 0) {
          ctx.font = MONO_FONT;
          ctx.fillStyle = "#94a3b8";
          ctx.textAlign = "right"; ctx.textBaseline = "top";
          ctx.fillText(fmtVal(pMax), plotL - 4, panelTop);
          ctx.textBaseline = "bottom";
          ctx.fillText(fmtVal(pMin), plotL - 4, panelBot);
        }

        if (!indicatorLoading && indicatorData.length === 0) {
          ctx.font = "11px sans-serif";
          ctx.fillStyle = "#475569";
          ctx.textAlign = "center"; ctx.textBaseline = "middle";
          ctx.fillText("데이터 없음", plotL + plotW / 2, panelTop + PANEL_H / 2);
        }

        // 제로선
        if (yMin < 0 && yMin + yRange > 0 && allVals.length > 0) {
          ctx.strokeStyle = "rgba(255,255,255,0.12)";
          ctx.lineWidth = 1;
          ctx.setLineDash([2, 2]);
          const y0 = toYP(0);
          ctx.beginPath(); ctx.moveTo(plotL, y0); ctx.lineTo(plotR, y0); ctx.stroke();
          ctx.setLineDash([]);
        }

        // 패널 클립
        ctx.save();
        ctx.beginPath();
        ctx.rect(plotL, panelTop, plotW, PANEL_H);
        ctx.clip();

        // 크로스오버 fill
        if (panelId === "STOCH" && panelTypes.includes("STOCHASTIC_K_14_7") && panelTypes.includes("STOCHASTIC_D_14_7")) {
          const kVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.STOCHASTIC_K_14_7 : undefined);
          const dVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.STOCHASTIC_D_14_7 : undefined);
          drawCrossoverFill(ctx, kVals, dVals, toYP, xAt, "rgba(239,68,68,0.22)", "rgba(59,130,246,0.22)");
        }
        if (panelId === "ADX" && panelTypes.includes("PLUS_DI_14") && panelTypes.includes("MINUS_DI_14")) {
          const pVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.PLUS_DI_14  : undefined);
          const mVals = visibleSlots.map(s => s ? indicatorMap.get(s.date)?.MINUS_DI_14 : undefined);
          drawCrossoverFill(ctx, pVals, mVals, toYP, xAt, "rgba(74,222,128,0.18)", "rgba(248,113,113,0.18)");
        }

        // 패널 선 / 히스토그램
        for (const type of panelTypes) {
          const { color, lineWidth } = itemStyle(type);
          if (panelId === "MACD" && type === "MACD_HISTOGRAM") {
            visibleSlots.forEach((s, i) => {
              if (!s) return;
              const v = indicatorMap.get(s.date)?.[type];
              if (v == null) return;
              const cx = xAt(i), y0 = toYP(0), y1 = toYP(v);
              ctx.fillStyle = v >= 0 ? "#4ade80" : "#f87171";
              ctx.globalAlpha = 0.6;
              ctx.fillRect(cx - bw / 2, Math.min(y0, y1), bw, Math.max(1, Math.abs(y0 - y1)));
              ctx.globalAlpha = 1;
            });
            continue;
          }
          let path: [number, number][] = []; let nm = true;
          visibleSlots.forEach((s, i) => {
            if (!s) { nm = true; return; }
            const v = indicatorMap.get(s.date)?.[type];
            if (v == null) return;
            if (nm) { path = [[xAt(i), toYP(v)]]; nm = false; }
            else { path.push([xAt(i), toYP(v)]); }
          });
          if (path.length < 2) continue;
          ctx.strokeStyle = color; ctx.lineWidth = lineWidth;
          ctx.lineJoin = "round"; ctx.lineCap = "round";
          ctx.beginPath();
          ctx.moveTo(path[0][0], path[0][1]);
          for (let i = 1; i < path.length; i++) ctx.lineTo(path[i][0], path[i][1]);
          ctx.stroke();
        }

        ctx.restore(); // 패널 클립 해제
      });

        perf.end("Render", `StockChart[${code}] static draw`, _t0);
        perf.pipe.end("⑨ canvas draw 완료 → 화면에 표시됨");
      });
    }

    triggerStaticDrawRef.current = trigger;
    trigger();
    return () => cancelAnimationFrame(staticRafRef.current);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  // rightIndex / visibleCount 제거 — refs(riRef/vcRef)로 직접 읽으므로 불필요
  }, [width, expandedBars, indicatorData, mode, presetItems, subPanels, loading]);


  // ── Canvas draw: 오버레이 (크로스헤어만) ─────────────────────────────────────

  useEffect(() => {
    if (width === 0) return;
    cancelAnimationFrame(overlayRafRef.current);
    overlayRafRef.current = requestAnimationFrame(() => {
      const canvas = overlayCanvasRef.current;
      if (!canvas) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2); // DPR cap
      const needResize = canvas.width !== Math.round(width * dpr) || canvas.height !== Math.round(svgH * dpr);
      if (needResize) {
        canvas.width  = Math.round(width * dpr);
        canvas.height = Math.round(svgH  * dpr);
        canvas.style.width  = `${width}px`;
        canvas.style.height = `${svgH}px`;
      }
      const ctx = canvas.getContext("2d");
      if (!ctx) return;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, width, svgH);

      if (hoverIdx === null || hoverIdx < 0) return;
      const L = computeLayout(width, visibleSlots, bars, subPanels.length, svgH);
      const { plotL, plotR, cTop, xAt, toY, chartBot } = L;

      const bar = visibleSlots[hoverIdx];
      if (!bar || bar.close === null) return;

      const hx = xAt(hoverIdx);
      const hy = toY(bar.close);

      // 수직선
      ctx.strokeStyle = "rgba(255,255,255,0.20)";
      ctx.lineWidth = 1;
      ctx.setLineDash([3, 3]);
      ctx.beginPath(); ctx.moveTo(hx, cTop); ctx.lineTo(hx, chartBot); ctx.stroke();
      // 수평선
      ctx.strokeStyle = "rgba(255,255,255,0.18)";
      ctx.beginPath(); ctx.moveTo(plotL, hy); ctx.lineTo(plotR, hy); ctx.stroke();
      ctx.setLineDash([]);

      // 가격 라벨 (우측)
      ctx.fillStyle = "#334155";
      ctx.beginPath(); ctx.roundRect(plotR + 2, hy - 8, PAD.right - 4, 16, 2); ctx.fill();
      ctx.font = "10px ui-monospace, monospace";
      ctx.fillStyle = "#e2e8f0";
      ctx.textAlign = "center"; ctx.textBaseline = "middle";
      ctx.fillText(fmtPrice(bar.close), plotR + 2 + (PAD.right - 4) / 2, hy);

      // 보조지표 패널 크로스헤어
      const vBot = PAD.top + CANDLE_H + GAP_H + VOL_H;
      subPanels.forEach((panelId, pi) => {
        const panelTop  = vBot + SCROLL_H + PANEL_GAP + pi * (PANEL_H + PANEL_GAP);
        const panelBot  = panelTop + PANEL_H;
        const panelTypes = selectedIndicators.filter(t => INDICATOR_META[t]?.panel === panelId);

        const allVals: number[] = [];
        visibleSlots.forEach(s => {
          if (!s) return;
          const v = indicatorMap.get(s.date);
          for (const t of panelTypes) { const val = v?.[t]; if (val != null) allVals.push(val); }
        });
        if (allVals.length === 0) return;

        let pMin = Math.min(...allVals), pMax = Math.max(...allVals);
        if (pMin === pMax) { pMin -= 1; pMax += 1; }
        const pad2   = (pMax - pMin) * 0.08;
        const yMin   = pMin - pad2;
        const yRange = pMax - pMin + pad2 * 2;
        const toYP   = (v: number) => panelBot - ((v - yMin) / yRange) * PANEL_H;

        const vals = indicatorMap.get(bar.date);
        const hoverVals = panelTypes
          .map(t => ({ type: t, v: vals?.[t], ...itemStyle(t) }))
          .filter((x): x is { type: IndicatorType; v: number; color: string; lineWidth: number } => x.v != null);
        if (hoverVals.length === 0) return;

        // 날짜
        ctx.font = "10px ui-monospace, monospace";
        ctx.fillStyle = "#64748b";
        ctx.textAlign = "right"; ctx.textBaseline = "top";
        ctx.fillText(bar.date, plotR - 2, panelTop);

        // 값 라벨
        hoverVals.forEach(({ type, v, color }, ti) => {
          ctx.fillStyle = color;
          ctx.textAlign = "left"; ctx.textBaseline = "top";
          ctx.fillText(`${INDICATOR_META[type].label}: ${fmtVal(v)}`, plotL + 4, panelTop + 12 + ti * 13);
        });

        // 크로스헤어 수평선 (첫 번째 값만 우측 라벨)
        hoverVals.forEach(({ v, color }, ti) => {
          const hy2 = toYP(v);
          if (hy2 < panelTop || hy2 > panelBot) return;
          ctx.strokeStyle = color;
          ctx.lineWidth = 1; ctx.globalAlpha = 0.5;
          ctx.setLineDash([3, 3]);
          ctx.beginPath(); ctx.moveTo(plotL, hy2); ctx.lineTo(plotR, hy2); ctx.stroke();
          ctx.setLineDash([]); ctx.globalAlpha = 1;
          if (ti === 0) {
            ctx.fillStyle = "#334155";
            ctx.beginPath(); ctx.roundRect(plotR + 2, hy2 - 8, PAD.right - 4, 16, 2); ctx.fill();
            ctx.fillStyle = color;
            ctx.textAlign = "center"; ctx.textBaseline = "middle";
            ctx.fillText(fmtVal(v), plotR + 2 + (PAD.right - 4) / 2, hy2);
          }
        });
      });
    });
    return () => cancelAnimationFrame(overlayRafRef.current);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hoverIdx, width, expandedBars, visibleCount, rightIndex, indicatorData, subPanels]);

  // ── 인터랙션 ──────────────────────────────────────────────────────────────────

  const { handleTouchStart, handleTouchMove, handleTouchEnd, handlePointerMove, handlePointerLeave } =
    useChartInteraction({ containerRef, vcRef, riRef, widthRef, totalRef, setVisibleCount, setRightIndex, setHoverIdx, triggerStaticDrawRef });

  const scrollMin      = visibleCount - 1;
  const scrollMax      = expandedBars.length - 1;
  const scrollDisabled = expandedBars.length <= visibleCount;

  return (
    <div className="select-none">
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
        {/* 정적 레이어 */}
        <canvas ref={staticCanvasRef} style={{ display: "block" }} />

        {/* 오버레이 레이어 (크로스헤어) — pointer-events: none */}
        <canvas ref={overlayCanvasRef} style={{ position: "absolute", top: 0, left: 0, pointerEvents: "none" }} />

        {/* 상태 메시지: 반투명 오버레이로 이전 차트 유지 */}
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/50 backdrop-blur-[1px]">
            <div className="flex items-center gap-2 text-slate-300 text-xs bg-slate-900/80 px-3 py-1.5 rounded-full border border-white/10">
              <svg className="w-3.5 h-3.5 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l3-3-3-3V4a10 10 0 00-10 10h2z" />
              </svg>
              로딩 중
            </div>
          </div>
        )}
        {error && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/60">
            <p className="text-red-400/90 text-sm">{error}</p>
          </div>
        )}
        {!loading && !error && bars.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center text-slate-600 text-sm">데이터 없음</div>
        )}

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
            onChange={e => {
              const v = Number(e.target.value);
              setRightIndex(v);
              riRef.current = v;
              // rightIndex는 static draw deps에서 제거됐으므로 직접 트리거
              triggerStaticDrawRef.current();
            }}
            className="w-full h-1 accent-slate-500 cursor-pointer disabled:opacity-20 disabled:cursor-default"
          />
        </div>

        {/* OHLCV 툴팁 (HTML) */}
        {hoveredBar && hoverIdx !== null && (() => {
          const nv     = visibleSlots.length;
          const onLeft = hoverIdx / nv > 0.55;
          const rising = (hoveredBar.close ?? 0) >= (hoveredBar.open ?? hoveredBar.close ?? 0);
          const vals   = indicatorMap.get(hoveredBar.date);
          const overlayActive = selectedIndicators.filter(t => INDICATOR_META[t]?.panel === "OVERLAY");
          const priceColor = rising ? "text-red-400" : "text-blue-400";
          const posStyle = onLeft ? { left: PAD.left + 6 } : { right: PAD.right + 6 };
          return (
            <div className="absolute top-3 pointer-events-none z-10" style={posStyle}>

              {/* ── 모바일: 종가·거래량만 표시 (sm 미만) ─────────────────────── */}
              <div className="sm:hidden bg-slate-900/85 border border-white/10 rounded-md px-2 py-1 text-xs font-mono backdrop-blur-sm">
                <p className="text-slate-500 text-center leading-tight">{hoveredBar.date}</p>
                {isDelistedBar(hoveredBar) ? (
                  <p className="text-red-500/80 text-center leading-tight">상장폐지</p>
                ) : isHaltBar(hoveredBar) ? (
                  <p className="text-yellow-600/80 text-center leading-tight">
                    거래정지 <span className="text-slate-300">{hoveredBar.close?.toLocaleString()}</span>
                  </p>
                ) : (
                  <p className={`font-semibold text-center leading-tight ${priceColor}`}>
                    {hoveredBar.close?.toLocaleString() ?? "-"}
                    <span className="text-slate-500 font-normal text-[10px] ml-1">
                      {fmtVol(hoveredBar.volume ?? 0)}
                    </span>
                  </p>
                )}
              </div>

              {/* ── 태블릿 이상: 전체 OHLCV + 지표 (sm 이상) ─────────────────── */}
              <div className="hidden sm:block bg-slate-900/90 border border-white/10 rounded-lg px-3 py-2 text-xs font-mono backdrop-blur-sm min-w-[130px]">
                <p className="text-slate-400 mb-1.5 text-center">{hoveredBar.date}</p>
                {isDelistedBar(hoveredBar) ? (
                  <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                    <span className="text-red-500/80 col-span-2 text-center">상장폐지</span>
                  </div>
                ) : isHaltBar(hoveredBar) ? (
                  <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                    <span className="text-slate-500 col-span-2 text-center text-yellow-600/80">거래정지</span>
                    <span className="text-slate-500">종가</span>
                    <span className="text-right text-slate-300">{hoveredBar.close?.toLocaleString()}</span>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                    <span className="text-slate-500">시가</span>
                    <span className="text-right text-slate-200">{hoveredBar.open?.toLocaleString() ?? "-"}</span>
                    <span className="text-slate-500">고가</span>
                    <span className="text-right text-red-400">{hoveredBar.high?.toLocaleString() ?? "-"}</span>
                    <span className="text-slate-500">저가</span>
                    <span className="text-right text-blue-400">{hoveredBar.low?.toLocaleString() ?? "-"}</span>
                    <span className="text-slate-500">종가</span>
                    <span className={`text-right font-semibold ${priceColor}`}>
                      {hoveredBar.close?.toLocaleString() ?? "-"}
                    </span>
                    <span className="text-slate-500 mt-0.5">거래량</span>
                    <span className="text-right text-slate-300 mt-0.5">{(hoveredBar.volume ?? 0).toLocaleString()}</span>
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
                )}
              </div>

            </div>
          );
        })()}
      </div>
    </div>
  );
}

// IndicatorManager 열고 닫을 때 등 부모 re-render 시 불필요한 chart re-render 방지
export default memo(StockChart);
