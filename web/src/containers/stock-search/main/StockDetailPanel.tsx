"use client";

import { useEffect, useMemo, useState } from "react";
import { StockMatchResult } from "@/types/filter";
import StockChart, { type PriceBar } from "@/components/chart/StockChart";
import { INDICATOR_META, type PanelId } from "@/components/chart/indicatorMeta";
import IndicatorManager from "./IndicatorManager";
import PanelOrderModal from "./PanelOrderModal";
import StockInfoTab from "./StockInfoTab";
import StockEventsTab from "./StockEventsTab";
import StockAnalystSection from "./StockAnalystSection";
import InvestorFlowTab from "./InvestorFlowTab";
import FundamentalsTab from "./FundamentalsTab";
import ValuationTab from "./ValuationTab";
import { useHasCapability } from "@/states/capabilities";
import type { UseIndicatorPresetsReturn } from "@/hooks/useIndicatorPresets";
import type { ModelSummary, CustomMetricSummary } from "@/types/chart-overlay";
import { DEFAULT_CHART_OVERLAY } from "@/types/chart-overlay";

interface Props {
  result: StockMatchResult;
  onBack: () => void;
  presets: UseIndicatorPresetsReturn;
}

export default function StockDetailPanel({ result, onBack, presets: presetsHook }: Props) {
  const { presets, activePreset, setActivePreset, loading, create, update, remove, reorder } =
    presetsHook;

  const canModelScore      = useHasCapability("MODEL_SCORE");
  const canCustomIndicator = useHasCapability("CUSTOM_INDICATOR");

  const [tab, setTab]                       = useState<"chart" | "info" | "fundamentals" | "valuation" | "events" | "research" | "investor-flow">("chart");
  const [managerOpen, setManagerOpen]       = useState(false);
  const [panelOrderOpen, setPanelOrderOpen] = useState(false);
  const [mode, setMode]                     = useState<"candle" | "line">("candle");
  const [source, setSource]                 = useState<"KRX" | "NXT" | "INTEGRATED">("INTEGRATED");
  const [availableSources, setAvailableSources] = useState<("KRX" | "NXT" | "INTEGRATED")[]>(["INTEGRATED", "KRX", "NXT"]);
  const [adjusted, setAdjusted]             = useState(true);
  const [latestBar, setLatestBar]           = useState<PriceBar | null>(null);
  const [prevClose, setPrevClose]           = useState<number | null>(null);

  // 차트 오버레이(모델 시그널·커스텀 지표) — 저장된 프리셋의 값(activePreset.overlay)에서 파생. 저장해야 반영.
  const overlay = activePreset?.overlay ?? DEFAULT_CHART_OVERLAY;
  const [signalModels, setSignalModels]     = useState<ModelSummary[]>([]);
  const [customMetrics, setCustomMetrics]   = useState<CustomMetricSummary[]>([]);

  // 모델 목록(grant 필터) — MODEL_SCORE 권한 있을 때만. 실패 시 빈 배열 폴백.
  useEffect(() => {
    if (!canModelScore) { setSignalModels([]); return; }
    let cancelled = false;
    fetch("/api/stocks/models")
      .then(r => (r.ok ? r.json() : []))
      .then((rows: ModelSummary[]) => { if (!cancelled) setSignalModels(Array.isArray(rows) ? rows : []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [canModelScore]);

  // 커스텀 지표 목록(grant 필터) — CUSTOM_INDICATOR 권한 있을 때만. 실패 시 빈 배열 폴백.
  useEffect(() => {
    if (!canCustomIndicator) { setCustomMetrics([]); return; }
    let cancelled = false;
    fetch("/api/stocks/custom-metrics")
      .then(r => (r.ok ? r.json() : []))
      .then((rows: CustomMetricSummary[]) => { if (!cancelled) setCustomMetrics(Array.isArray(rows) ? rows : []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [canCustomIndicator]);

  const signalModel = signalModels.find(m => m.id === overlay.signalModelId) ?? null;
  // 선택된 커스텀 지표들(overlay 순서 유지) — 각각 하단 서브패널로 표시
  const selectedMetrics = useMemo(
    () => overlay.seriesMetricIds
      .map(id => customMetrics.find(m => m.id === id))
      .filter((m): m is CustomMetricSummary => m != null),
    [overlay.seriesMetricIds, customMetrics],
  );

  // 종목 전환 시 데이터가 있는 가격 소스만 활성화하고, 기본 소스를 항상 통합 우선(없으면 KRX)으로 리셋한다.
  // 이전 종목에서 KRX/NXT를 골랐어도, 새 종목에선 다시 통합을 우선한다.
  useEffect(() => {
    let cancelled = false;
    fetch(`/api/stocks/${result.code}/price-sources`)
      .then(r => (r.ok ? r.json() : []))
      .then((srcs: ("KRX" | "NXT" | "INTEGRATED")[]) => {
        if (cancelled) return;
        const list: ("KRX" | "NXT" | "INTEGRATED")[] =
          Array.isArray(srcs) && srcs.length ? srcs : ["KRX"];
        setAvailableSources(list);
        setSource(list.includes("INTEGRATED") ? "INTEGRATED"
          : list.includes("KRX") ? "KRX" : (list[0] ?? "KRX"));
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [result.code]);

  // 현재 활성화된 서브패널 목록 (panelOrder 기준 정렬)
  const activePanelIds = useMemo((): PanelId[] => {
    if (!activePreset) return [];
    const enabledTypes = activePreset.items.filter(i => i.enabled).map(i => i.type);
    const activePanels = new Set<PanelId>();
    for (const t of enabledTypes) {
      const panel = INDICATOR_META[t]?.panel;
      if (panel && panel !== "OVERLAY") activePanels.add(panel);
    }
    const order = activePreset.panelOrder as PanelId[];
    return [
      ...order.filter(p => activePanels.has(p)),
      ...[...activePanels].filter(p => !order.includes(p)),
    ];
  }, [activePreset]);

  async function handlePanelReorder(newOrder: PanelId[]) {
    if (!activePreset) return;
    await update(activePreset.id, activePreset.name, {
      items: activePreset.items,
      panelOrder: newOrder,
      overlay: activePreset.overlay ?? DEFAULT_CHART_OVERLAY,
    });
  }

  // 헤더 전일대비 — 최신 종가와 직전 봉 종가로 계산
  const headerClose = latestBar?.close ?? null;
  const headerDiff = headerClose != null && prevClose != null && prevClose > 0 ? headerClose - prevClose : null;
  const headerPct = headerDiff != null && prevClose ? (headerDiff / prevClose) * 100 : null;

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* 종목 헤더 */}
      <div className="flex items-center gap-3 px-5 py-3 border-b border-white/10 flex-shrink-0">
        <button
          onClick={onBack}
          className="md:hidden flex-shrink-0 text-slate-400 hover:text-white transition"
          title="목록으로"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div className="flex-1 min-w-0">
          <div className="flex items-baseline gap-2 flex-wrap">
            <h2 className="text-white font-semibold truncate">{result.name}</h2>
            <span className="text-xs text-slate-500 font-mono flex-shrink-0">{result.code}</span>
            <span className="px-1.5 py-0.5 rounded text-xs bg-white/5 text-slate-400 flex-shrink-0">{result.marketType}</span>
          </div>

          {/* 가격 + 전일대비 + OHLC — 모든 화면 크기에서 표시, 좁으면 줄바꿈 */}
          {latestBar && latestBar.close != null ? (
            <div className="flex flex-wrap items-baseline gap-x-2.5 mt-0.5 text-xs font-mono">
              <span className="text-sm text-white">{latestBar.close.toLocaleString()}원</span>
              {headerDiff != null && headerPct != null && (
                <span className={headerDiff > 0 ? "text-red-400" : headerDiff < 0 ? "text-blue-400" : "text-slate-400"}>
                  {headerDiff > 0 ? "+" : ""}{headerDiff.toLocaleString()} ({headerDiff > 0 ? "+" : ""}{headerPct.toFixed(2)}%)
                </span>
              )}
              <span className="text-slate-500">시 <span className="text-slate-200">{latestBar.open?.toLocaleString() ?? "-"}</span></span>
              <span className="text-slate-500">고 <span className="text-red-400">{latestBar.high?.toLocaleString() ?? "-"}</span></span>
              <span className="text-slate-500">저 <span className="text-blue-400">{latestBar.low?.toLocaleString() ?? "-"}</span></span>
              <span className="text-slate-500">종 <span className={latestBar.close >= (latestBar.open ?? latestBar.close) ? "text-red-400" : "text-blue-400"}>{latestBar.close.toLocaleString()}</span></span>
            </div>
          ) : result.closePrice != null ? (
            <span className="block text-sm text-white font-mono mt-0.5">{result.closePrice.toLocaleString()}원</span>
          ) : null}
        </div>
      </div>

      {/* 탭 바 — 모바일: 내용 크기 + 가로 스크롤, 데스크톱: 전체 폭 균등 분배. 모델 점수는 별도 탭 대신 그래프의 지표 설정(차트 오버레이)에서 시그널로 표시. */}
      <div className="flex border-b border-white/10 flex-shrink-0 overflow-x-auto overflow-y-hidden [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
        {(["chart", "info", "fundamentals", "valuation", "events", "research", "investor-flow"] as const)
          .map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-shrink-0 whitespace-nowrap px-4 py-2 md:flex-1 md:px-0 text-sm border-b-2 -mb-px transition text-center ${
              tab === t ? "border-indigo-500 text-white" : "border-transparent text-slate-400 hover:text-white"
            }`}
          >
            {t === "chart" ? "그래프"
              : t === "info" ? "종목 상세"
              : t === "fundamentals" ? "재무제표"
              : t === "valuation" ? "밸류에이션"
              : t === "events" ? "권리 이벤트"
              : t === "research" ? "리서치"
              : "투자자 동향"}
          </button>
        ))}
      </div>

      {/* 非차트 탭 콘텐츠 */}
      {tab === "info" && (
        <div className="flex-1 overflow-hidden">
          <StockInfoTab code={result.code} />
        </div>
      )}
      {tab === "fundamentals" && (
        <div className="flex-1 overflow-hidden">
          <FundamentalsTab code={result.code} />
        </div>
      )}
      {tab === "valuation" && (
        <div className="flex-1 overflow-hidden">
          <ValuationTab code={result.code} />
        </div>
      )}
      {tab === "events" && (
        <div className="flex-1 overflow-hidden">
          <StockEventsTab code={result.code} />
        </div>
      )}
      {tab === "research" && (
        <div className="flex-1 overflow-y-auto px-5 py-4">
          <StockAnalystSection code={result.code} />
        </div>
      )}
      {tab === "investor-flow" && (
        <div className="flex-1 overflow-hidden">
          <InvestorFlowTab code={result.code} />
        </div>
      )}

      {/* 차트 영역 — 항상 마운트해서 OHLC를 즉시 로드, 비활성 탭에서는 CSS로 숨김 */}
      <div className={`flex flex-1 overflow-hidden relative ${tab !== "chart" ? "hidden" : ""}`}>
        <div className="flex-1 overflow-y-auto">

          {/* 툴바 — 모바일: 2줄, 데스크톱: 1줄 */}
          <div className="flex flex-wrap items-center px-3 pt-2 pb-1 gap-1.5">
            {/* 프리셋 선택 */}
            <select
              value={activePreset?.id ?? ""}
              onChange={e => {
                const p = presets.find(p => p.id === Number(e.target.value));
                if (p) setActivePreset(p);
              }}
              disabled={loading || presets.length === 0}
              className="bg-slate-800 border border-white/10 rounded px-2 py-1.5 text-xs text-white max-w-[110px] truncate disabled:opacity-40"
            >
              {presets.length === 0 && <option value="">프리셋 없음</option>}
              {presets.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>

            {/* 지표 관리 */}
            <button
              onClick={() => setManagerOpen(v => !v)}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded text-xs border transition ${
                managerOpen
                  ? "bg-white/10 border-white/20 text-white"
                  : "border-white/10 text-slate-400 hover:text-white hover:border-white/20"
              }`}
            >
              <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              지표
            </button>

            {/* 보조지표 순서 */}
            <button
              onClick={() => setPanelOrderOpen(true)}
              disabled={activePanelIds.length < 2}
              className="flex items-center gap-1 px-2.5 py-1.5 rounded text-xs border border-white/10 text-slate-400 hover:text-white hover:border-white/20 transition disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="3" y1="6"  x2="21" y2="6"  />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
                <polyline points="17 3 21 6 17 9" />
                <polyline points="7 15 3 18 7 21" />
              </svg>
              순서
            </button>

            {/* 구분선 — 데스크톱만 표시 */}
            <span className="hidden sm:block w-px h-4 bg-white/10" />

            {/* 거래소 선택 — 데이터 없는 소스는 비활성 */}
            <div className="flex rounded border border-white/10 overflow-hidden text-xs">
              {(["INTEGRATED", "KRX", "NXT"] as const).map(s => {
                const enabled = availableSources.includes(s);
                return (
                  <button
                    key={s}
                    onClick={() => enabled && setSource(s)}
                    disabled={!enabled}
                    title={enabled ? undefined : "데이터 없음"}
                    className={`px-2 py-1.5 transition ${
                      source === s ? "bg-white/10 text-white"
                        : enabled ? "text-slate-500 hover:text-slate-300"
                        : "text-slate-700 cursor-not-allowed"
                    }`}
                  >
                    {s === "INTEGRATED" ? "통합" : s}
                  </button>
                );
              })}
            </div>

            {/* 수정주가 토글 */}
            <div className="flex rounded border border-white/10 overflow-hidden text-xs">
              <button
                onClick={() => setAdjusted(true)}
                className={`px-2.5 py-1.5 transition ${adjusted ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"}`}
              >수정</button>
              <button
                onClick={() => setAdjusted(false)}
                className={`px-2.5 py-1.5 transition ${!adjusted ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"}`}
              >비수정</button>
            </div>

            {/* 캔들/라인 토글 */}
            <div className="flex rounded border border-white/10 overflow-hidden text-xs">
              <button
                onClick={() => setMode("candle")}
                className={`px-2.5 py-1.5 transition ${mode === "candle" ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"}`}
              >캔들</button>
              <button
                onClick={() => setMode("line")}
                className={`px-2.5 py-1.5 transition ${mode === "line" ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"}`}
              >라인</button>
            </div>
          </div>

          <StockChart
            code={result.code}
            source={source}
            adjusted={adjusted}
            presetItems={activePreset?.items ?? []}
            panelOrder={activePreset?.panelOrder}
            mode={mode}
            signalModelId={overlay.signalModelId}
            signalModelName={signalModel?.name ?? null}
            signalThreshold={overlay.signalThreshold}
            seriesMetrics={selectedMetrics}
            onLatestBar={(bar, prev) => { setLatestBar(bar); setPrevClose(prev ?? null); }}
          />
        </div>

        <IndicatorManager
          open={managerOpen}
          onClose={() => setManagerOpen(false)}
          presets={presets}
          activePreset={activePreset}
          loading={loading}
          create={create}
          update={update}
          remove={remove}
          reorder={reorder}
          signalModels={signalModels}
          customMetrics={customMetrics}
        />

        <PanelOrderModal
          open={panelOrderOpen}
          onClose={() => setPanelOrderOpen(false)}
          activePanelIds={activePanelIds}
          onReorder={handlePanelReorder}
        />
      </div>
    </div>
  );
}

