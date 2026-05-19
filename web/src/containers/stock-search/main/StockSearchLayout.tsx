"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { perf } from "@/utils/perf";
import { useIndicatorPresets } from "@/hooks/useIndicatorPresets";
import { cx } from "@/utils/cx";
import { SearchFilter, ExecuteFilterResponse, StockMatchResult } from "@/types/filter";
import StockDetailPanel from "./StockDetailPanel";
import TradingDayCalendar from "@/components/TradingDayCalendar";
import Select from "@/components/Select";
import { prefetchChart } from "@/components/chart/StockChart";

// ── Virtual List 상수 ──────────────────────────────────────────────────────────
// py-2.5(10px × 2) + text-sm(20px) + text-xs(16px) = 56px
const ROW_HEIGHT = 56;
const OVERSCAN   = 5; // 위아래 여분 행 수

function prevTradingDay(current: string, tradingDays: string[]): string | null {
  const sorted = [...tradingDays].sort();
  const idx = sorted.indexOf(current);
  return idx > 0 ? sorted[idx - 1] : null;
}

function nextTradingDay(current: string, tradingDays: string[]): string | null {
  const sorted = [...tradingDays].sort();
  const idx = sorted.indexOf(current);
  return idx >= 0 && idx < sorted.length - 1 ? sorted[idx + 1] : null;
}

interface Props {
  filters: SearchFilter[];
  tradingDays: string[];
  latestDate: string;
  initialFilterId?: number | null;
}

export default function StockSearchLayout({ filters, tradingDays, latestDate, initialFilterId }: Props) {
  const presetsHook = useIndicatorPresets();

  const [selectedFilterId, setSelectedFilterId] = useState<number | null>(
    initialFilterId ?? (filters.length > 0 ? filters[0].id : null)
  );
  const [date, setDate]                         = useState(latestDate);
  const [running, setRunning]                   = useState(false);
  const [result, setResult]                     = useState<ExecuteFilterResponse | null>(null);
  const [error, setError]                       = useState<string | null>(null);
  const [selectedResult, setSelectedResult]     = useState<StockMatchResult | null>(null);
  const [searchQuery, setSearchQuery]           = useState("");

  // ── Virtual List 상태 ────────────────────────────────────────────────────────
  const listRef             = useRef<HTMLDivElement>(null);
  const [listST, setListST] = useState(0);   // scrollTop
  const [listH,  setListH]  = useState(600); // 스크롤 컨테이너 높이

  useEffect(() => {
    const el = listRef.current;
    if (!el) return;
    setListH(el.clientHeight);
    const ro = new ResizeObserver(() => setListH(el.clientHeight));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // ── Hover Prefetch ───────────────────────────────────────────────────────────
  // 디바운스 없이 즉시 fetch 시작 — 서버 응답(150-280ms)이 클릭 전에 도착할 확률 극대화
  const handleRowMouseEnter = (s: StockMatchResult) => {
    const indicatorsKey = presetsHook.activePreset?.items
      .filter(i => i.enabled).map(i => i.type).join(",") ?? "";
    prefetchChart(s.code, s.marketType, indicatorsKey);
  };

  // ── 검색 필터링 ──────────────────────────────────────────────────────────────
  const filteredResults = useMemo(() => {
    if (!result?.results) return [];
    const q = searchQuery.trim().toLowerCase();
    if (!q) return result.results;
    return result.results.filter(s =>
      s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)
    );
  }, [result, searchQuery]);

  // ── Virtual List 계산 ────────────────────────────────────────────────────────
  const startIdx      = Math.max(0, Math.floor(listST / ROW_HEIGHT) - OVERSCAN);
  const endIdx        = Math.min(filteredResults.length, Math.ceil((listST + listH) / ROW_HEIGHT) + OVERSCAN);
  const visibleItems  = filteredResults.slice(startIdx, endIdx);
  const totalListH    = filteredResults.length * ROW_HEIGHT;

  // ── 날짜 이동 ────────────────────────────────────────────────────────────────
  const handlePrev = () => {
    const p = prevTradingDay(date, tradingDays);
    if (p) { setDate(p); setResult(null); setError(null); setSearchQuery(""); }
  };
  const handleNext = () => {
    const n = nextTradingDay(date, tradingDays);
    if (n) { setDate(n); setResult(null); setError(null); setSearchQuery(""); }
  };

  // ── 검색 실행 ────────────────────────────────────────────────────────────────
  async function handleRun() {
    if (!selectedFilterId) return;
    setRunning(true);
    setError(null);
    setResult(null);
    setSelectedResult(null);
    setSearchQuery("");
    try {
      const res = await perf.measure(
        "API",
        `POST /api/filters/${selectedFilterId}/execute (date=${date})`,
        () => fetch(`/api/filters/${selectedFilterId}/execute`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ referenceDate: date }),
        }),
      );
      const data = await res.json();
      if (!res.ok) {
        setError(
          data?.error === "NO_DATA_FOR_DATE" ? "해당 날짜의 데이터가 없습니다." :
          data?.error === "FILTER_NOT_FOUND"  ? "필터를 찾을 수 없습니다." :
          "실행 중 오류가 발생했습니다."
        );
        return;
      }
      setResult(data as ExecuteFilterResponse);
    } catch {
      setError("네트워크 오류가 발생했습니다.");
    } finally {
      setRunning(false);
    }
  }

  const hasPrev = !!prevTradingDay(date, tradingDays);
  const hasNext = !!nextTradingDay(date, tradingDays);

  return (
    <div className="flex flex-col flex-1 overflow-hidden min-w-0">

      {/* ── 검색 영역 ──────────────────────────────────────────────────────────── */}
      <div className="flex-shrink-0 border-b border-white/10 px-5 py-4 flex flex-wrap items-end gap-4">
        <div className="flex flex-col gap-1 min-w-0 flex-1" style={{ minWidth: "180px", maxWidth: "320px" }}>
          <label className="text-xs text-slate-400">검색 필터</label>
          {filters.length === 0 ? (
            <p className="text-xs text-slate-500 py-2">
              등록된 필터가 없습니다.{" "}
              <a href="/search-filters/new" className="text-indigo-400 hover:underline">새 필터 만들기</a>
            </p>
          ) : (
            <Select
              value={selectedFilterId?.toString() ?? null}
              items={filters.map(f => ({ value: f.id.toString(), label: f.name }))}
              onChange={(v) => { setSelectedFilterId(Number(v)); setResult(null); setError(null); }}
            />
          )}
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-xs text-slate-400">기준일</label>
          <div className="flex items-center gap-1">
            <button onClick={handlePrev} disabled={!hasPrev} title="이전 거래일"
              className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <TradingDayCalendar
              value={date}
              tradingDays={tradingDays}
              onChange={(d) => { setDate(d); setResult(null); setError(null); }}
            />
            <button onClick={handleNext} disabled={!hasNext} title="다음 거래일"
              className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
        </div>

        <button onClick={handleRun} disabled={running || !selectedFilterId}
          className={`flex items-center gap-2 ${cx.btnPrimary}`}>
          {running ? (
            <>
              <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l3-3-3-3V4a10 10 0 00-10 10h2z" />
              </svg>
              검색 중...
            </>
          ) : (
            <>
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              검색
            </>
          )}
        </button>

        {result && (
          <div className="ml-auto text-right">
            <p className="text-xs text-slate-400">기준일: <span className="text-white">{result.evaluationDate}</span></p>
            <p className="text-xs text-slate-400">
              {result.totalCandidates}개 중{" "}
              <span className="text-indigo-300 font-semibold">{result.matchCount}개</span> 매칭
            </p>
          </div>
        )}
      </div>

      {/* ── 오류 ───────────────────────────────────────────────────────────────── */}
      {error && (
        <div className="flex-shrink-0 px-5 py-3 bg-red-900/20 border-b border-red-500/20">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {/* ── 결과 목록 + 상세 영역 ─────────────────────────────────────────────── */}
      <div className="flex flex-1 overflow-hidden">

        {/* ── 결과 목록 패널 (flex-col, overflow-hidden) ────────────────────────
            Virtual List 구조:
              ① 검색 입력 (fixed, flex-shrink-0)
              ② 컬럼 헤더 (fixed, flex-shrink-0)
              ③ 스크롤 영역 (flex-1, overflow-y-auto)
                 └─ position:relative 컨테이너 (height = items * ROW_HEIGHT)
                      └─ position:absolute 행들 (보이는 것만 렌더링)
        ──────────────────────────────────────────────────────────────────────── */}
        <div className={`
          flex-shrink-0 flex flex-col overflow-hidden border-r border-white/10
          md:w-64 lg:w-72 xl:w-80 md:flex-none
          ${selectedResult ? "hidden md:flex" : "flex-1 flex"}
        `}>

          {/* ① 검색 입력 */}
          {result && (
            <div className="flex-shrink-0 bg-slate-900/95 px-3 py-2 border-b border-white/10">
              <div className="relative">
                <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500 pointer-events-none"
                  viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  placeholder="종목명 · 티커 검색"
                  className="w-full bg-white/5 border border-white/10 rounded pl-8 pr-7 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-400/50 transition"
                />
                {searchQuery && (
                  <button onClick={() => setSearchQuery("")}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition">
                    <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                  </button>
                )}
              </div>
              {searchQuery && (
                <p className="text-xs text-slate-500 mt-1.5 px-0.5">{filteredResults.length}개 결과</p>
              )}
            </div>
          )}

          {/* ② 컬럼 헤더 */}
          {result && result.results.length > 0 && (
            <div className="flex-shrink-0 flex px-4 py-2 border-b border-white/10 bg-slate-900/95">
              <span className="flex-1 text-xs text-slate-400 font-medium">종목명</span>
              <span className="text-xs text-slate-400 font-medium">종가</span>
            </div>
          )}

          {/* ③ 스크롤 영역 */}
          <div
            ref={listRef}
            className="flex-1 overflow-y-auto"
            onScroll={e => setListST(e.currentTarget.scrollTop)}
          >
            {/* 빈 결과 */}
            {result && result.results.length === 0 && (
              <div className="flex flex-col items-center justify-center py-20 text-slate-500 text-sm">
                <svg className="w-10 h-10 mb-3 opacity-40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                조건에 맞는 종목이 없습니다
              </div>
            )}

            {/* 검색 결과 없음 */}
            {result && result.results.length > 0 && filteredResults.length === 0 && (
              <div className="px-4 py-10 text-center text-xs text-slate-500">
                &ldquo;{searchQuery}&rdquo;와 일치하는 종목 없음
              </div>
            )}

            {/* Virtual List */}
            {result && filteredResults.length > 0 && (
              <div style={{ position: "relative", height: totalListH }}>
                {visibleItems.map((s, i) => {
                  const top     = (startIdx + i) * ROW_HEIGHT;
                  const isSelected = selectedResult?.code === s.code && selectedResult?.marketType === s.marketType;
                  return (
                    <div
                      key={`${s.marketType}-${s.code}`}
                      style={{ position: "absolute", top, left: 0, right: 0, height: ROW_HEIGHT }}
                      onMouseEnter={() => handleRowMouseEnter(s)}

                      onClick={() => {
                        perf.pipe.start(`종목 선택: ${s.name} (${s.code})`);
                        perf.pipe.mark("① click → setSelectedResult");
                        setSelectedResult(s);
                      }}
                      className={`flex items-center px-4 cursor-pointer border-b border-white/5 transition-colors hover:bg-white/3 ${
                        isSelected ? "bg-indigo-600/20 border-l-2 border-l-indigo-500" : ""
                      }`}
                    >
                      <div className="flex-1 min-w-0 pr-2">
                        <p className="text-white text-sm truncate">{s.name}</p>
                        <p className="text-xs text-slate-500 font-mono">{s.code} · {s.marketType}</p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="text-white text-sm font-mono">
                          {s.closePrice != null ? s.closePrice.toLocaleString() : "-"}
                        </p>
                        <p className="text-xs text-slate-500">
                          {s.volume != null ? s.volume.toLocaleString() : ""}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* 초기 안내 */}
            {!result && !running && !error && (
              <div className="flex flex-col items-center justify-center h-full text-slate-600 gap-2 px-4">
                <svg className="w-12 h-12 opacity-20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                  <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <p className="text-sm text-center">필터와 기준일을 선택하고 검색하세요</p>
              </div>
            )}
          </div>
        </div>

        {/* ── 상세 영역 ──────────────────────────────────────────────────────── */}
        <div className={`flex-1 overflow-hidden ${selectedResult ? "block" : "hidden md:block"}`}>
          {selectedResult ? (
            <StockDetailPanel result={selectedResult} onBack={() => setSelectedResult(null)} presets={presetsHook} />
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-slate-700 gap-2">
              <svg className="w-12 h-12 opacity-20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              <p className="text-sm">종목을 선택하면 상세 정보를 확인할 수 있습니다</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
