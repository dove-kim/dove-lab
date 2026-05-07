"use client";

import { useState } from "react";
import { cx } from "@/utils/cx";
import { SearchFilter, ExecuteFilterResponse, StockMatchResult } from "@/types/filter";
import StockDetailPanel from "./StockDetailPanel";
import TradingDayCalendar from "@/components/TradingDayCalendar";
import Select from "@/components/Select";

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
  const [selectedFilterId, setSelectedFilterId] = useState<number | null>(
    initialFilterId ?? (filters.length > 0 ? filters[0].id : null)
  );

  const [date, setDate] = useState(latestDate);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ExecuteFilterResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedResult, setSelectedResult] = useState<StockMatchResult | null>(null);

  const handlePrev = () => {
    const p = prevTradingDay(date, tradingDays);
    if (p) { setDate(p); setResult(null); setError(null); }
  };

  const handleNext = () => {
    const n = nextTradingDay(date, tradingDays);
    if (n) { setDate(n); setResult(null); setError(null); }
  };

  async function handleRun() {
    if (!selectedFilterId) return;
    setRunning(true);
    setError(null);
    setResult(null);
    setSelectedResult(null);
    try {
      const res = await fetch(`/api/filters/${selectedFilterId}/execute`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ referenceDate: date }),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(
          data?.error === "NO_DATA_FOR_DATE" ? "해당 날짜의 데이터가 없습니다." :
          data?.error === "FILTER_NOT_FOUND" ? "필터를 찾을 수 없습니다." :
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
      {/* 검색 영역 */}
      <div className="flex-shrink-0 border-b border-white/10 px-5 py-4 flex flex-wrap items-end gap-4">
        {/* 필터 선택 */}
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

        {/* 기준일 선택 */}
        <div className="flex flex-col gap-1">
          <label className="text-xs text-slate-400">기준일</label>
          <div className="flex items-center gap-1">
            <button
              onClick={handlePrev}
              disabled={!hasPrev}
              title="이전 거래일"
              className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <TradingDayCalendar
              value={date}
              tradingDays={tradingDays}
              onChange={(d) => { setDate(d); setResult(null); setError(null); }}
            />
            <button
              onClick={handleNext}
              disabled={!hasNext}
              title="다음 거래일"
              className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
        </div>

        {/* 실행 버튼 */}
        <button
          onClick={handleRun}
          disabled={running || !selectedFilterId}
          className={`flex items-center gap-2 ${cx.btnPrimary}`}
        >
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

      {/* 오류 */}
      {error && (
        <div className="flex-shrink-0 px-5 py-3 bg-red-900/20 border-b border-red-500/20">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {/* 결과 목록 + 상세 영역 */}
      <div className="flex flex-1 overflow-hidden">
        {/* 결과 목록: 모바일 전체, md+ 고정폭 */}
        <div className={`
          flex-shrink-0 overflow-y-auto border-r border-white/10
          md:w-64 lg:w-72 xl:w-80 md:flex-none
          ${selectedResult ? "hidden md:block" : "flex-1"}
        `}>
          {result && result.results.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 text-slate-500 text-sm">
              <svg className="w-10 h-10 mb-3 opacity-40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              조건에 맞는 종목이 없습니다
            </div>
          )}
          {result && result.results.length > 0 && (
            <table className={cx.table.root}>
              <thead className={`sticky top-0 bg-slate-900/95 ${cx.table.head}`}>
                <tr>
                  <th className={cx.table.th}>종목명</th>
                  <th className={`${cx.table.th} text-right`}>종가</th>
                </tr>
              </thead>
              <tbody className={cx.table.body}>
                {result.results.map((s: StockMatchResult) => (
                  <tr
                    key={`${s.marketType}-${s.code}`}
                    onClick={() => setSelectedResult(s)}
                    className={`${cx.table.tr} cursor-pointer ${
                      selectedResult?.code === s.code && selectedResult?.marketType === s.marketType
                        ? "bg-indigo-600/20 border-l-2 border-indigo-500"
                        : ""
                    }`}
                  >
                    <td className={cx.table.td}>
                      <p className="text-white text-sm">{s.name}</p>
                      <p className="text-xs text-slate-500 font-mono">{s.code} · {s.marketType}</p>
                    </td>
                    <td className={`${cx.table.td} text-right`}>
                      <p className="text-white text-sm font-mono">
                        {s.closePrice != null ? s.closePrice.toLocaleString() : "-"}
                      </p>
                      <p className="text-xs text-slate-500">
                        {s.volume != null ? s.volume.toLocaleString() : ""}
                      </p>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {!result && !running && !error && (
            <div className="flex flex-col items-center justify-center h-full text-slate-600 gap-2 px-4">
              <svg className="w-12 h-12 opacity-20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <p className="text-sm text-center">필터와 기준일을 선택하고 검색하세요</p>
            </div>
          )}
        </div>

        {/* 상세 영역: 모바일 전체, md+ 우측 고정 */}
        <div className={`flex-1 overflow-hidden ${selectedResult ? "block" : "hidden md:block"}`}>
          {selectedResult ? (
            <StockDetailPanel result={selectedResult} onBack={() => setSelectedResult(null)} />
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
