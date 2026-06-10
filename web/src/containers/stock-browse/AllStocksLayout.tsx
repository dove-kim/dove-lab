"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useIndicatorPresets } from "@/hooks/useIndicatorPresets";
import { StockMatchResult } from "@/types/filter";
import StockDetailPanel from "@/containers/stock-search/main/StockDetailPanel";
import { prefetchChart } from "@/components/chart/StockChart";

// ── Virtual List 상수 ──────────────────────────────────────────────────────────
const ROW_HEIGHT = 56;
const OVERSCAN = 5;
const NEW_LISTING_DAYS = 7;

interface StockApiItem {
  ticker: string;
  market: string;
  name: string;
  listingDate: string | null;
  tradingHalt: boolean;
  adminItem: boolean;
}

type StatusFilter = "all" | "halt" | "admin" | "new";
type SortOrder = "default" | "name";

function isNewListing(listingDate: string | null): boolean {
  if (!listingDate) return false;
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - NEW_LISTING_DAYS);
  return new Date(listingDate) >= cutoff;
}

/**
 * 검색 조건 없이 전 종목을 좌측 리스트로 보여주고, 선택 시 우측에 차트·지표·정보를 띄운다.
 * 일반 유저("모든 종목")·ROOT("주식 종목 리스트") 공용 화면.
 */
export default function AllStocksLayout() {
  const presetsHook = useIndicatorPresets();

  const [stocks, setStocks] = useState<StockMatchResult[]>([]);
  const [rawItems, setRawItems] = useState<StockApiItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<StockMatchResult | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [sortOrder, setSortOrder] = useState<SortOrder>("default");

  // ── Virtual List 상태 ────────────────────────────────────────────────────────
  const listRef = useRef<HTMLDivElement>(null);
  const [listST, setListST] = useState(0);
  const [listH, setListH] = useState(600);

  // ── 키보드 종목 이동 (↑↓) ───────────────────────────────────────────────────
  const keyNavRef = useRef<(e: KeyboardEvent) => void>(() => {});
  keyNavRef.current = (e: KeyboardEvent) => {
    if (e.key !== "ArrowUp" && e.key !== "ArrowDown") return;
    if (!selected || filtered.length === 0) return;
    const tag = (e.target as HTMLElement).tagName;
    if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;

    e.preventDefault();
    const cur = filtered.findIndex(
      s => s.code === selected.code && s.marketType === selected.marketType
    );
    if (cur === -1) return;
    const next = e.key === "ArrowUp"
      ? Math.max(0, cur - 1)
      : Math.min(filtered.length - 1, cur + 1);
    if (next === cur) return;

    setSelected(filtered[next]);

    const el = listRef.current;
    if (el) {
      const top    = next * ROW_HEIGHT;
      const bottom = top + ROW_HEIGHT;
      if (top < el.scrollTop) {
        el.scrollTop = top;
      } else if (bottom > el.scrollTop + el.clientHeight) {
        el.scrollTop = bottom - el.clientHeight;
      }
    }
  };
  useEffect(() => {
    const handler = (e: KeyboardEvent) => keyNavRef.current(e);
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  // 전 종목 1회 로드
  useEffect(() => {
    fetch("/api/stocks")
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data: StockApiItem[]) => {
        setRawItems(data);
        setStocks(
          data.map((s) => ({
            code: s.ticker,
            name: s.name,
            marketType: s.market,
            closePrice: null,
            volume: null,
            tradingHalt: s.tradingHalt,
            adminItem: s.adminItem,
          }))
        );
      })
      .catch(() => setError("종목 목록을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const el = listRef.current;
    if (!el) return;
    setListH(el.clientHeight);
    const ro = new ResizeObserver(() => setListH(el.clientHeight));
    ro.observe(el);
    return () => ro.disconnect();
  }, [loading]);

  const handleRowMouseEnter = (s: StockMatchResult) => {
    const indicatorsKey = presetsHook.activePreset?.items
      .filter((i) => i.enabled).map((i) => i.type).join(",") ?? "";
    prefetchChart(s.code, "KRX", true, indicatorsKey);
  };

  // 집계
  const haltCount  = useMemo(() => stocks.filter((s) => s.tradingHalt).length, [stocks]);
  const adminCount = useMemo(() => stocks.filter((s) => s.adminItem).length, [stocks]);
  const newCount   = useMemo(() => rawItems.filter((s) => isNewListing(s.listingDate)).length, [rawItems]);

  // newListing 빠른 조회용 맵
  const newListingSet = useMemo(() => {
    const set = new Set<string>();
    rawItems.forEach((s) => { if (isNewListing(s.listingDate)) set.add(s.ticker); });
    return set;
  }, [rawItems]);

  // ── 필터 + 정렬 ──────────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    let result = stocks.filter((s) => {
      if (status === "halt"  && !s.tradingHalt) return false;
      if (status === "admin" && !s.adminItem)   return false;
      if (status === "new"   && !newListingSet.has(s.code)) return false;
      if (q && !(s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q))) return false;
      return true;
    });
    if (sortOrder === "name") {
      result = [...result].sort((a, b) => a.name.localeCompare(b.name, "ko"));
    }
    return result;
  }, [stocks, searchQuery, status, sortOrder, newListingSet]);

  const startIdx     = Math.max(0, Math.floor(listST / ROW_HEIGHT) - OVERSCAN);
  const endIdx       = Math.min(filtered.length, Math.ceil((listST + listH) / ROW_HEIGHT) + OVERSCAN);
  const visibleItems = filtered.slice(startIdx, endIdx);
  const totalListH   = filtered.length * ROW_HEIGHT;

  return (
    <div className="flex flex-col flex-1 overflow-hidden min-w-0">
      <div className="flex flex-1 overflow-hidden">
        {/* ── 좌측: 종목 리스트 ──────────────────────────────────────────────── */}
        <div
          className={`
            flex-shrink-0 flex flex-col overflow-hidden border-r border-white/10
            md:w-64 lg:w-72 xl:w-80 md:flex-none
            ${selected ? "hidden md:flex" : "flex-1 flex"}
          `}
        >
          {/* 검색 + 필터 */}
          <div className="flex-shrink-0 bg-slate-900/95 px-3 py-2 border-b border-white/10">
            {/* 검색 입력 */}
            <div className="relative">
              <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500 pointer-events-none"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
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

            {/* 종목 수 + 정렬 */}
            <div className="flex items-center justify-between mt-1.5 px-0.5">
              <p className="text-xs text-slate-500">
                {loading ? "불러오는 중..." : `${filtered.length.toLocaleString()}개 종목`}
              </p>
              <button
                type="button"
                onClick={() => setSortOrder((v) => v === "name" ? "default" : "name")}
                className={`flex items-center gap-1 text-[11px] px-2 py-0.5 rounded border transition ${
                  sortOrder === "name"
                    ? "bg-indigo-600/30 border-indigo-500/50 text-indigo-300"
                    : "border-white/10 text-slate-500 hover:text-slate-300"
                }`}
              >
                <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="15" y2="12" /><line x1="3" y1="18" x2="9" y2="18" />
                </svg>
                이름순
              </button>
            </div>

            {/* 상태 필터 */}
            <div className="flex flex-wrap gap-1 mt-2">
              {([
                { key: "all",   label: "전체",                   activeClass: "bg-indigo-600 border-indigo-500 text-white" },
                { key: "new",   label: `신규상장 ${newCount}`,    activeClass: "bg-emerald-700 border-emerald-600 text-white" },
                { key: "halt",  label: `거래정지 ${haltCount}`,   activeClass: "bg-rose-700 border-rose-600 text-white" },
                { key: "admin", label: `관리종목 ${adminCount}`,  activeClass: "bg-amber-700 border-amber-600 text-white" },
              ] as { key: StatusFilter; label: string; activeClass: string }[]).map((c) => (
                <button
                  key={c.key}
                  onClick={() => setStatus(c.key)}
                  className={`px-2 py-1 rounded text-xs border transition ${
                    status === c.key
                      ? c.activeClass
                      : "bg-slate-800 border-white/10 text-slate-400 hover:text-white"
                  }`}
                >
                  {c.label}
                </button>
              ))}
            </div>
          </div>

          {/* 스크롤 영역 (가상 리스트) */}
          <div
            ref={listRef}
            className="flex-1 overflow-y-auto"
            onScroll={(e) => setListST(e.currentTarget.scrollTop)}
          >
            {error && (
              <div className="px-4 py-10 text-center text-sm text-rose-400">{error}</div>
            )}
            {!loading && !error && filtered.length === 0 && (
              <div className="px-4 py-10 text-center text-xs text-slate-500">
                {searchQuery ? `"${searchQuery}"와 일치하는 종목 없음` : "종목이 없습니다"}
              </div>
            )}
            {filtered.length > 0 && (
              <div style={{ position: "relative", height: totalListH }}>
                {visibleItems.map((s, i) => {
                  const top = (startIdx + i) * ROW_HEIGHT;
                  const isSelected = selected?.code === s.code && selected?.marketType === s.marketType;
                  const isNew = newListingSet.has(s.code);
                  return (
                    <div
                      key={`${s.marketType}-${s.code}`}
                      style={{ position: "absolute", top, left: 0, right: 0, height: ROW_HEIGHT }}
                      onMouseEnter={() => handleRowMouseEnter(s)}
                      onClick={() => setSelected(s)}
                      className={`flex items-center px-4 cursor-pointer border-b border-white/5 transition-colors hover:bg-white/3 ${
                        isSelected ? "bg-indigo-600/20 border-l-2 border-l-indigo-500" : ""
                      }`}
                    >
                      <div className="flex-1 min-w-0 pr-2">
                        <div className="flex items-center gap-1.5">
                          <p className="text-white text-sm truncate">{s.name}</p>
                          {isNew && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-emerald-900/50 text-emerald-300 border border-emerald-700/50">NEW</span>
                          )}
                          {s.tradingHalt && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-rose-900/40 text-rose-300 border border-rose-700/40">정지</span>
                          )}
                          {s.adminItem && (
                            <span className="flex-shrink-0 text-[10px] px-1 py-0.5 rounded bg-amber-900/40 text-amber-300 border border-amber-700/40">관리</span>
                          )}
                        </div>
                        <p className="text-xs text-slate-500 font-mono">{s.code} · {s.marketType}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* ── 우측: 상세 (차트·지표·정보) ───────────────────────────────────── */}
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
