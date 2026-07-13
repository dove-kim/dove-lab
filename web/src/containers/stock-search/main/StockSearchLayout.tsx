"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { perf } from "@/utils/perf";
import { useIndicatorPresets } from "@/hooks/useIndicatorPresets";
import { cx } from "@/utils/cx";
import { useHasCapability } from "@/states/capabilities";
import { SearchFilter, ExecuteFilterResponse, StockMatchResult } from "@/types/filter";
import StockDetailPanel from "./StockDetailPanel";
import TradingDayCalendar from "@/components/TradingDayCalendar";
import Select from "@/components/Select";
import { prefetchChart } from "@/components/chart/StockChart";
import MiniCandle from "@/components/chart/MiniCandle";

// ── Virtual List 상수 ──────────────────────────────────────────────────────────
const ROW_HEIGHT = 68;
const OVERSCAN = 5;
const NEW_LISTING_DAYS = 7;

interface StockApiItem {
  ticker: string;
  market: string;
  name: string;
  listingDate: string | null;
  tradingHalt: boolean;
  adminItem: boolean;
  openPrice: number | null;
  highPrice: number | null;
  lowPrice: number | null;
  closePrice: number | null;
  volume: number | null;
  prevClose: number | null;
  marketCap: number | null;
}

type StatusFilter = "all" | "halt" | "admin" | "new";
// 리스트 표시 정렬은 단일 키 + 방향으로 충분 — 다중 정렬은 필터 파이프라인 RANK가 담당한다.
type SortField = "default" | "name" | "change" | "volume" | "marketCap" | "modelScore";
type SortDir = "asc" | "desc";
type Mode = "all" | "filter";

// 숫자 정렬 필드 라벨(방향 토글 노출 대상). default·name은 방향 고정이라 제외.
const SORT_FIELD_OPTIONS: { value: SortField; label: string }[] = [
  { value: "default", label: "기본" },
  { value: "name", label: "이름" },
  { value: "change", label: "등락률" },
  { value: "volume", label: "거래량" },
  { value: "marketCap", label: "시가총액" },
  { value: "modelScore", label: "모델 점수" },
];
const NUMERIC_SORT_FIELDS: SortField[] = ["change", "volume", "marketCap", "modelScore"];

/**
 * 종목 한 건의 정렬값을 반환한다(없으면 null → 항상 마지막). 등락률=(종가-전일종가)/전일종가.
 */
function sortValue(s: StockMatchResult, field: SortField): number | null {
  if (field === "change") {
    return s.prevClose != null && s.prevClose > 0 && s.closePrice != null
      ? (s.closePrice - s.prevClose) / s.prevClose : null;
  }
  if (field === "volume") return s.volume ?? null;
  if (field === "marketCap") return s.marketCap ?? null;
  if (field === "modelScore") return s.modelScore ?? null;
  return null;
}

/**
 * 단일 키 + 방향으로 종목 목록을 정렬한다. null 값은 방향과 무관하게 항상 마지막.
 */
function sortStocks(list: StockMatchResult[], field: SortField, dir: SortDir): StockMatchResult[] {
  if (field === "default") return list;
  if (field === "name") return [...list].sort((a, b) => a.name.localeCompare(b.name, "ko"));
  const sign = dir === "asc" ? 1 : -1;
  return [...list].sort((a, b) => {
    const av = sortValue(a, field);
    const bv = sortValue(b, field);
    if (av == null && bv == null) return 0;
    if (av == null) return 1;
    if (bv == null) return -1;
    return (av - bv) * sign;
  });
}

function isNewListing(listingDate: string | null): boolean {
  if (!listingDate) return false;
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - NEW_LISTING_DAYS);
  return new Date(listingDate) >= cutoff;
}

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

/**
 * 종목 화면 — 좌측 종목 리스트(전체 또는 필터 결과) + 우측 상세(차트·지표·정보).
 * "전체" 모드는 검색 조건 없이 전 종목을, "필터" 모드는 검색식 실행 결과를 보여준다.
 */
export default function StockSearchLayout({ filters, tradingDays, latestDate, initialFilterId }: Props) {
  const presetsHook = useIndicatorPresets();
  const canSearch = useHasCapability("STOCK_SEARCH");
  const [mode, setMode] = useState<Mode>(initialFilterId && canSearch ? "filter" : "all");

  // ── 전체 모드 상태 ─────────────────────────────────────────────────────────────
  const [rawItems, setRawItems] = useState<StockApiItem[]>([]);
  const [allStocks, setAllStocks] = useState<StockMatchResult[]>([]);
  const [allLoading, setAllLoading] = useState(true);
  const [allError, setAllError] = useState<string | null>(null);
  const [status, setStatus] = useState<StatusFilter>("all");
  const [sortField, setSortField] = useState<SortField>("default");
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  // ── 필터 모드 상태 ─────────────────────────────────────────────────────────────
  const [selectedFilterId, setSelectedFilterId] = useState<number | null>(
    initialFilterId ?? (filters.length > 0 ? filters[0].id : null)
  );
  const [date, setDate] = useState(latestDate);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ExecuteFilterResponse | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);

  // ── 공통 상태 ──────────────────────────────────────────────────────────────────
  const [searchQuery, setSearchQuery] = useState("");
  const [searchOpen, setSearchOpen] = useState(true);
  const [selected, setSelected] = useState<StockMatchResult | null>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [listST, setListST] = useState(0);
  const [listH, setListH] = useState(600);

  // 전 종목 1회 로드
  useEffect(() => {
    fetch("/api/stocks")
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data: StockApiItem[]) => {
        setRawItems(data);
        setAllStocks(data.map((s) => ({
          code: s.ticker, name: s.name, marketType: s.market,
          openPrice: s.openPrice, highPrice: s.highPrice, lowPrice: s.lowPrice,
          closePrice: s.closePrice, volume: s.volume, prevClose: s.prevClose,
          marketCap: s.marketCap ?? null,
          tradingHalt: s.tradingHalt, adminItem: s.adminItem,
        })));
      })
      .catch(() => setAllError("종목 목록을 불러오지 못했습니다."))
      .finally(() => setAllLoading(false));
  }, []);

  const newListingSet = useMemo(() => {
    const set = new Set<string>();
    rawItems.forEach((s) => { if (isNewListing(s.listingDate)) set.add(s.ticker); });
    return set;
  }, [rawItems]);

  const haltCount = useMemo(() => allStocks.filter((s) => s.tradingHalt).length, [allStocks]);
  const adminCount = useMemo(() => allStocks.filter((s) => s.adminItem).length, [allStocks]);
  const newCount = newListingSet.size;

  // ── 표시 리스트 (모드별) ────────────────────────────────────────────────────────
  const displayList = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (mode === "all") {
      const r = allStocks.filter((s) => {
        if (status === "halt" && !s.tradingHalt) return false;
        if (status === "admin" && !s.adminItem) return false;
        if (status === "new" && !newListingSet.has(s.code)) return false;
        if (q && !(s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q))) return false;
        return true;
      });
      return sortStocks(r, sortField, sortDir);
    }
    if (!result?.results) return [];
    const filtered = q
      ? result.results.filter((s) => s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q))
      : result.results;
    return sortStocks(filtered, sortField, sortDir);
  }, [mode, allStocks, status, sortField, sortDir, newListingSet, searchQuery, result]);

  // ── 키보드 종목 이동 (↑↓) ───────────────────────────────────────────────────────
  const keyNavRef = useRef<(e: KeyboardEvent) => void>(() => {});
  keyNavRef.current = (e: KeyboardEvent) => {
    if (e.key !== "ArrowUp" && e.key !== "ArrowDown") return;
    if (!selected || displayList.length === 0) return;
    const tag = (e.target as HTMLElement).tagName;
    if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;

    e.preventDefault();
    const cur = displayList.findIndex((s) => s.code === selected.code && s.marketType === selected.marketType);
    if (cur === -1) return;
    const next = e.key === "ArrowUp" ? Math.max(0, cur - 1) : Math.min(displayList.length - 1, cur + 1);
    if (next === cur) return;

    setSelected(displayList[next]);
    const el = listRef.current;
    if (el) {
      const top = next * ROW_HEIGHT;
      const bottom = top + ROW_HEIGHT;
      if (top < el.scrollTop) el.scrollTop = top;
      else if (bottom > el.scrollTop + el.clientHeight) el.scrollTop = bottom - el.clientHeight;
    }
  };
  useEffect(() => {
    const handler = (e: KeyboardEvent) => keyNavRef.current(e);
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  useEffect(() => {
    const el = listRef.current;
    if (!el) return;
    setListH(el.clientHeight);
    const ro = new ResizeObserver(() => setListH(el.clientHeight));
    ro.observe(el);
    return () => ro.disconnect();
  }, [allLoading]);

  const handleRowMouseEnter = (s: StockMatchResult) => {
    const indicatorsKey = presetsHook.activePreset?.items
      .filter((i) => i.enabled).map((i) => i.type).join(",") ?? "";
    prefetchChart(s.code, "KRX", true, indicatorsKey);
  };

  // ── Virtual List 계산 ────────────────────────────────────────────────────────
  const startIdx = Math.max(0, Math.floor(listST / ROW_HEIGHT) - OVERSCAN);
  const endIdx = Math.min(displayList.length, Math.ceil((listST + listH) / ROW_HEIGHT) + OVERSCAN);
  const visibleItems = displayList.slice(startIdx, endIdx);
  const totalListH = displayList.length * ROW_HEIGHT;

  // ── 필터 모드 — 날짜 이동·실행 ──────────────────────────────────────────────────
  const handlePrev = () => {
    const p = prevTradingDay(date, tradingDays);
    if (p) { setDate(p); setResult(null); setFilterError(null); setSearchQuery(""); }
  };
  const handleNext = () => {
    const n = nextTradingDay(date, tradingDays);
    if (n) { setDate(n); setResult(null); setFilterError(null); setSearchQuery(""); }
  };
  const hasPrev = !!prevTradingDay(date, tradingDays);
  const hasNext = !!nextTradingDay(date, tradingDays);

  async function handleRun() {
    if (!selectedFilterId) return;
    setRunning(true);
    setFilterError(null);
    setResult(null);
    setSelected(null);
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
        setFilterError(
          data?.error === "NO_DATA_FOR_DATE" ? "해당 날짜의 데이터가 없습니다." :
          data?.error === "FILTER_NOT_FOUND" ? "필터를 찾을 수 없습니다." :
          "실행 중 오류가 발생했습니다."
        );
        return;
      }
      setResult(data as ExecuteFilterResponse);
    } catch {
      setFilterError("네트워크 오류가 발생했습니다.");
    } finally {
      setRunning(false);
    }
  }

  function switchMode(m: Mode) {
    if (m === mode) return;
    if (m === "filter" && !canSearch) return;
    setMode(m);
    setSearchQuery("");
    setSelected(null);
  }

  const listError = mode === "all" ? allError : filterError;
  const listEmpty = !listError && displayList.length === 0;

  // 정렬 컨트롤(두 모드 공용) — 단일 키 + 방향. 숫자 필드일 때만 방향 토글 노출.
  const sortControl = (
    <div className="flex flex-col gap-1">
      <label className="text-xs text-slate-400">정렬</label>
      <div className="flex items-center gap-1.5">
        <Select
          value={sortField}
          items={SORT_FIELD_OPTIONS.map((o) => ({ value: o.value, label: o.label }))}
          onChange={(v) => setSortField(v as SortField)}
          className="w-28"
        />
        {NUMERIC_SORT_FIELDS.includes(sortField) && (
          <button type="button" onClick={() => setSortDir((d) => (d === "asc" ? "desc" : "asc"))}
            title={sortDir === "asc" ? "오름차순" : "내림차순"}
            className="flex items-center justify-center w-9 h-9 rounded-lg border border-white/15 text-slate-300 hover:text-white hover:bg-white/5 transition">
            {sortDir === "asc" ? "↑" : "↓"}
          </button>
        )}
      </div>
      {sortField === "marketCap" && (
        <span className="text-[10px] text-slate-500">밸류에이션 있는 종목만</span>
      )}
    </div>
  );

  return (
    <div className="flex flex-col flex-1 overflow-hidden min-w-0">
      {/* ── 검색/필터 영역 토글 바 (모바일·웹 공통) ───────────────────────────── */}
      <div className="flex-shrink-0 flex items-center justify-between px-5 py-1.5 border-b border-white/10 bg-slate-900/40">
        <span className="text-xs text-slate-400">
          {mode === "all" ? "전체 종목" : "필터 검색"}
          {result && mode === "filter" && <span className="text-slate-500"> · {result.matchCount}개 매칭</span>}
        </span>
        <button type="button" onClick={() => setSearchOpen((v) => !v)}
          className="flex items-center gap-1 text-xs text-slate-400 hover:text-white transition">
          {searchOpen ? "검색 접기" : "검색 펼치기"}
          <svg className={`w-3.5 h-3.5 transition-transform ${searchOpen ? "" : "rotate-180"}`}
            viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="18 15 12 9 6 15" />
          </svg>
        </button>
      </div>

      {/* ── 상단 컨트롤 (접기 가능) ───────────────────────────────────────────── */}
      {searchOpen && (
      <div className="flex-shrink-0 border-b border-white/10 px-5 py-3 flex flex-wrap items-end gap-4">
        {/* 모드 토글 */}
        <div className="flex flex-col gap-1">
          <label className="text-xs text-slate-400">보기</label>
          <div className="flex rounded-lg border border-white/15 overflow-hidden text-sm h-9">
            <button type="button" onClick={() => switchMode("all")}
              className={`px-3 ${mode === "all" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>전체</button>
            <button type="button" onClick={() => switchMode("filter")} disabled={!canSearch}
              title={canSearch ? undefined : "검색 권한이 없습니다"}
              className={`px-3 flex items-center gap-1 ${
                !canSearch ? "text-slate-600 cursor-not-allowed"
                : mode === "filter" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>
              필터
              {!canSearch && (
                <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {mode === "all" ? (
          <>
            {/* 상태 필터 */}
            <div className="flex flex-col gap-1">
              <label className="text-xs text-slate-400">상태</label>
              <div className="flex flex-wrap gap-1.5">
                {([
                  { key: "all", label: "전체", activeClass: "bg-indigo-600 border-indigo-500 text-white" },
                  { key: "new", label: `신규상장 ${newCount}`, activeClass: "bg-emerald-700 border-emerald-600 text-white" },
                  { key: "halt", label: `거래정지 ${haltCount}`, activeClass: "bg-rose-700 border-rose-600 text-white" },
                  { key: "admin", label: `관리종목 ${adminCount}`, activeClass: "bg-amber-700 border-amber-600 text-white" },
                ] as { key: StatusFilter; label: string; activeClass: string }[]).map((c) => (
                  <button key={c.key} onClick={() => setStatus(c.key)}
                    className={`px-2 py-1.5 rounded text-xs border transition ${
                      status === c.key ? c.activeClass : "bg-slate-800 border-white/10 text-slate-400 hover:text-white"}`}>
                    {c.label}
                  </button>
                ))}
              </div>
            </div>
            {sortControl}
            <p className="text-xs text-slate-500 ml-auto self-center">
              {allLoading ? "불러오는 중..." : `${displayList.length.toLocaleString()}개 종목`}
            </p>
          </>
        ) : (
          <>
            {/* 검색 필터 */}
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
                  items={filters.map((f) => ({ value: f.id.toString(), label: f.name }))}
                  onChange={(v) => { setSelectedFilterId(Number(v)); setResult(null); setFilterError(null); }}
                />
              )}
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-slate-400">기준일</label>
              <div className="flex items-center gap-1">
                <button onClick={handlePrev} disabled={!hasPrev} title="이전 거래일"
                  className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed">
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
                </button>
                <TradingDayCalendar value={date} tradingDays={tradingDays}
                  onChange={(d) => { setDate(d); setResult(null); setFilterError(null); }} />
                <button onClick={handleNext} disabled={!hasNext} title="다음 거래일"
                  className="flex items-center justify-center w-8 h-9 rounded-lg border border-white/15 text-slate-400 hover:text-white hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed">
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
                </button>
              </div>
            </div>
            <button onClick={handleRun} disabled={running || !selectedFilterId} className={`flex items-center gap-2 ${cx.btnPrimary}`}>
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
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
                  검색
                </>
              )}
            </button>
            {sortControl}
            {result && (
              <div className="ml-auto text-right">
                <p className="text-xs text-slate-400">기준일: <span className="text-white">{result.evaluationDate}</span></p>
                <p className="text-xs text-slate-400">
                  {result.totalCandidates}개 중 <span className="text-indigo-300 font-semibold">{result.matchCount}개</span> 매칭
                </p>
              </div>
            )}
          </>
        )}
      </div>
      )}

      {/* ── 결과 목록 + 상세 영역 ─────────────────────────────────────────────── */}
      <div className="flex flex-1 overflow-hidden">
        <div className={`
          flex-shrink-0 flex flex-col overflow-hidden border-r border-white/10
          md:w-64 lg:w-72 xl:w-80 md:flex-none
          ${selected ? "hidden md:flex" : "flex-1 flex"}
        `}>
          {/* 검색 입력 */}
          <div className="flex-shrink-0 bg-slate-900/95 px-3 py-2 border-b border-white/10">
            <div className="relative">
              <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500 pointer-events-none"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input type="text" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="종목명 · 티커 검색"
                className="w-full bg-white/5 border border-white/10 rounded pl-8 pr-7 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-400/50 transition" />
              {searchQuery && (
                <button onClick={() => setSearchQuery("")}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition">
                  <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              )}
            </div>
          </div>

          {/* 스크롤 영역 (가상 리스트) */}
          <div ref={listRef} className="flex-1 overflow-y-auto" onScroll={(e) => setListST(e.currentTarget.scrollTop)}>
            {listError && <div className="px-4 py-10 text-center text-sm text-rose-400">{listError}</div>}
            {mode === "filter" && !result && !running && !filterError && (
              <div className="flex flex-col items-center justify-center h-full text-slate-600 gap-2 px-4">
                <svg className="w-12 h-12 opacity-20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
                <p className="text-sm text-center">필터와 기준일을 선택하고 검색하세요</p>
              </div>
            )}
            {!listError && (mode === "all" || result) && listEmpty && (
              <div className="px-4 py-10 text-center text-xs text-slate-500">
                {searchQuery ? `"${searchQuery}"와 일치하는 종목 없음` : "종목이 없습니다"}
              </div>
            )}
            {displayList.length > 0 && (
              <div style={{ position: "relative", height: totalListH }}>
                {visibleItems.map((s, i) => {
                  const top = (startIdx + i) * ROW_HEIGHT;
                  const isSelected = selected?.code === s.code && selected?.marketType === s.marketType;
                  const isNew = newListingSet.has(s.code);
                  // 최신 봉이 실제 거래됐을 때(거래량>0)만 등락률·거래량 표시 — 정지·무거래 봉의 "0 / 0.00%" 노이즈 제거
                  const traded = s.volume != null && s.volume > 0;
                  const chgPct = traded && s.prevClose != null && s.prevClose > 0 && s.closePrice != null
                    ? ((s.closePrice - s.prevClose) / s.prevClose) * 100 : null;
                  const chgColor = chgPct == null ? "text-slate-500"
                    : chgPct > 0 ? "text-red-400" : chgPct < 0 ? "text-blue-400" : "text-slate-400";
                  const hasCandle = s.openPrice != null && s.highPrice != null
                    && s.lowPrice != null && s.closePrice != null;
                  return (
                    <div key={`${s.marketType}-${s.code}`}
                      style={{ position: "absolute", top, left: 0, right: 0, height: ROW_HEIGHT }}
                      onMouseEnter={() => handleRowMouseEnter(s)}
                      onClick={() => setSelected(s)}
                      className={`flex items-center px-4 cursor-pointer border-b border-white/5 transition-colors hover:bg-white/3 ${
                        isSelected ? "bg-indigo-600/20 border-l-2 border-l-indigo-500" : ""}`}>
                      <div className="flex-1 min-w-0 pr-2">
                        <div className="flex items-center gap-1.5">
                          <p className="text-white text-sm truncate">{s.name}</p>
                          {mode === "all" && isNew && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-emerald-900/50 text-emerald-300 border border-emerald-700/50">NEW</span>
                          )}
                          {mode === "all" && s.tradingHalt && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-rose-900/40 text-rose-300 border border-rose-700/40">정지</span>
                          )}
                          {mode === "all" && s.adminItem && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-amber-900/40 text-amber-300 border border-amber-700/40">관리</span>
                          )}
                          {s.modelScore != null && (
                            <span title="모델 점수"
                              className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-indigo-900/50 text-indigo-300 border border-indigo-700/50 font-mono">
                              {s.modelScore.toLocaleString("ko-KR", { maximumFractionDigits: 3 })}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-slate-500 font-mono">{s.code} · {s.marketType}</p>
                      </div>
                      {s.closePrice != null && (
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <div className="text-right">
                            <p className="leading-tight font-mono text-sm">
                              <span className="text-white">{s.closePrice.toLocaleString()}</span>
                              {chgPct != null && (
                                <span className={`ml-1.5 text-xs ${chgColor}`}>
                                  {chgPct > 0 ? "+" : ""}{chgPct.toFixed(2)}%
                                </span>
                              )}
                            </p>
                            <p className="leading-tight text-[11px] text-slate-500 font-mono">
                              {traded ? s.volume!.toLocaleString() : ""}
                            </p>
                          </div>
                          {hasCandle && (
                            <MiniCandle open={s.openPrice!} high={s.highPrice!} low={s.lowPrice!} close={s.closePrice} />
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* ── 상세 영역 ──────────────────────────────────────────────────────── */}
        <div className={`flex-1 overflow-hidden ${selected ? "block" : "hidden md:block"}`}>
          {selected ? (
            <StockDetailPanel result={selected} onBack={() => setSelected(null)} presets={presetsHook} />
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-slate-700 gap-2">
              <svg className="w-12 h-12 opacity-20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              <p className="text-sm">종목을 선택하면 차트·지표·정보를 확인할 수 있습니다</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
