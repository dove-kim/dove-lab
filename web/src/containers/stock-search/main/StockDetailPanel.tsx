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
import type { UseIndicatorPresetsReturn } from "@/hooks/useIndicatorPresets";

interface Props {
  result: StockMatchResult;
  onBack: () => void;
  presets: UseIndicatorPresetsReturn;
}

export default function StockDetailPanel({ result, onBack, presets: presetsHook }: Props) {
  const { presets, activePreset, setActivePreset, loading, create, update, remove, reorder } =
    presetsHook;

  const [tab, setTab]                       = useState<"chart" | "info" | "events" | "research" | "investor-flow">("chart");
  const [managerOpen, setManagerOpen]       = useState(false);
  const [panelOrderOpen, setPanelOrderOpen] = useState(false);
  const [mode, setMode]                     = useState<"candle" | "line">("candle");
  const [source, setSource]                 = useState<"KRX" | "NXT" | "CONSOLIDATED">("KRX");
  const [adjusted, setAdjusted]             = useState(true);
  const [latestBar, setLatestBar]           = useState<PriceBar | null>(null);

  // 종목 전환 시 이전 종목의 최신 봉이 잠깐 남지 않게 초기화
  useEffect(() => { setLatestBar(null); }, [result.code]);

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
    });
  }

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
          <div className="flex items-center gap-3 mt-0.5">
            {result.closePrice != null && (
              <span className="text-sm text-white font-mono">{result.closePrice.toLocaleString()}원</span>
            )}
            {result.volume != null && (
              <span className="text-xs text-slate-500">거래량 {result.volume.toLocaleString()}</span>
            )}
          </div>
        </div>

        {/* 최신 일봉 OHLC (그래프 데이터 기준) */}
        {latestBar && latestBar.close != null && (
          <div className="hidden sm:flex items-center gap-2.5 text-xs font-mono flex-shrink-0">
            <span className="text-slate-500">{latestBar.date}</span>
            <span className="text-slate-500">시 <span className="text-slate-200">{latestBar.open?.toLocaleString() ?? "-"}</span></span>
            <span className="text-slate-500">고 <span className="text-red-400">{latestBar.high?.toLocaleString() ?? "-"}</span></span>
            <span className="text-slate-500">저 <span className="text-blue-400">{latestBar.low?.toLocaleString() ?? "-"}</span></span>
            <span className="text-slate-500">종 <span className={latestBar.close >= (latestBar.open ?? latestBar.close) ? "text-red-400" : "text-blue-400"}>{latestBar.close.toLocaleString()}</span></span>
          </div>
        )}
      </div>

      {/* 탭 바 */}
      <div className="flex gap-1 px-4 border-b border-white/10 flex-shrink-0">
        {(["chart", "info", "events", "research", "investor-flow"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-3 py-2 text-sm border-b-2 -mb-px transition ${
              tab === t ? "border-indigo-500 text-white" : "border-transparent text-slate-400 hover:text-white"
            }`}
          >
            {t === "chart" ? "그래프"
              : t === "info" ? "종목 상세"
              : t === "events" ? "권리 이벤트"
              : t === "research" ? "리서치"
              : "투자자 동향"}
          </button>
        ))}
      </div>

      {tab === "info" ? (
        <div className="flex-1 overflow-hidden">
          <StockInfoTab code={result.code} />
        </div>
      ) : tab === "events" ? (
        <div className="flex-1 overflow-hidden">
          <StockEventsTab code={result.code} />
        </div>
      ) : tab === "research" ? (
        <div className="flex-1 overflow-y-auto px-5 py-4 max-w-3xl">
          <StockAnalystSection code={result.code} />
        </div>
      ) : tab === "investor-flow" ? (
        <div className="flex-1 overflow-hidden">
          <InvestorFlowTab code={result.code} />
        </div>
      ) : (
      /* 차트 영역 */
      <div className="flex flex-1 overflow-hidden relative">
        <div className="flex-1 overflow-y-auto">

          {/* 툴바 */}
          <div className="flex items-center justify-between px-3 pt-2 pb-1 gap-2">
            {/* 좌측: 프리셋 + 지표관리 + 패널순서 */}
            <div className="flex items-center gap-1.5 min-w-0">
              {/* 프리셋 선택 */}
              <select
                value={activePreset?.id ?? ""}
                onChange={e => {
                  const p = presets.find(p => p.id === Number(e.target.value));
                  if (p) setActivePreset(p);
                }}
                disabled={loading || presets.length === 0}
                className="bg-slate-800 border border-white/10 rounded px-2 py-1.5 text-xs text-white max-w-[120px] truncate disabled:opacity-40"
              >
                {presets.length === 0 && <option value="">프리셋 없음</option>}
                {presets.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>

              {/* 지표 관리 */}
              <button
                onClick={() => setManagerOpen(v => !v)}
                className={`flex items-center gap-1 px-2.5 py-1.5 rounded text-xs border transition flex-shrink-0 ${
                  managerOpen
                    ? "bg-white/10 border-white/20 text-white"
                    : "border-white/10 text-slate-400 hover:text-white hover:border-white/20"
                }`}
              >
                <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
                지표 관리
              </button>

              {/* 보조지표 순서 */}
              <button
                onClick={() => setPanelOrderOpen(true)}
                disabled={activePanelIds.length < 2}
                className="flex items-center gap-1 px-2.5 py-1.5 rounded text-xs border border-white/10 text-slate-400 hover:text-white hover:border-white/20 transition flex-shrink-0 disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="3" y1="6"  x2="21" y2="6"  />
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                  <polyline points="17 3 21 6 17 9" />
                  <polyline points="7 15 3 18 7 21" />
                </svg>
                보조지표 순서
              </button>
            </div>

            {/* 우측: 거래소·주가타입·캔들/라인 토글 */}
            <div className="flex items-center gap-1.5 flex-shrink-0">
              {/* 거래소 선택 */}
              <div className="flex rounded border border-white/10 overflow-hidden text-xs">
                {(["CONSOLIDATED", "KRX", "NXT"] as const).map(s => (
                  <button
                    key={s}
                    onClick={() => setSource(s)}
                    className={`px-2.5 py-1.5 transition ${source === s ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"}`}
                  >
                    {s === "CONSOLIDATED" ? "통합" : s}
                  </button>
                ))}
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
          </div>

          <StockChart
            code={result.code}
            source={source}
            adjusted={adjusted}
            presetItems={activePreset?.items ?? []}
            panelOrder={activePreset?.panelOrder}
            mode={mode}
            onLatestBar={setLatestBar}
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
        />

        <PanelOrderModal
          open={panelOrderOpen}
          onClose={() => setPanelOrderOpen(false)}
          activePanelIds={activePanelIds}
          onReorder={handlePanelReorder}
        />
      </div>
      )}
    </div>
  );
}

