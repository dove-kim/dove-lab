"use client";

import { useState, useEffect, useCallback } from "react";
import { cx } from "@/utils/cx";

interface CollectionTask {
  id: number;
  type: string;
  scope: string;
  status: "PENDING" | "RUNNING" | "DONE" | "FAILED";
  total: number;
  done: number;
  progressPercent: number;
  adjustedTotal: number;
  adjustedDone: number;
  errorCode: string | null;
  errorDetail: string | null;
  createdAt: string;
  startedAt: string | null;
  progressAt: string | null;
  finishedAt: string | null;
}

const ALL_PRICE_EXCHANGES = ["KOSPI", "KOSDAQ", "KONEX", "NXT", "INTEGRATED"];

const EXCHANGES = [
  { value: "ALL", label: "전체 (모든 거래소)" },
  { value: "KOSPI", label: "KOSPI" },
  { value: "KOSDAQ", label: "KOSDAQ" },
  { value: "KONEX", label: "KONEX" },
  { value: "NXT", label: "NXT" },
  { value: "INTEGRATED", label: "통합 (KRX+NXT)" },
];

const PAGE_SIZE = 20;   // 재조회 이력 페이지 크기

const currentYear = new Date().getFullYear();
// KRX 기반(종목) — 시장 데이터 기산 2010년부터
const KRX_YEARS = Array.from({ length: currentYear - 2010 + 1 }, (_, i) => currentYear - i);
// KIS 기반(주가·이벤트) — KIS 데이터 기산 1985년부터
const ADJ_YEARS = Array.from({ length: currentYear - 1985 + 1 }, (_, i) => currentYear - i);

/** 에러 코드 → 한국어 메시지 */
const ERROR_LABEL: Record<string, string> = {
  KRX_ACCESS_BLOCKED: "KRX 접근 차단 (과도한 요청 — 잠시 후 재시도)",
  KRX_API_ERROR: "KRX API 오류",
  KIS_API_ERROR: "KIS API 오류",
  EXTERNAL_API_ERROR: "외부 API 오류",
  COLLECTION_FAILED: "수집 실패",
  INVALID_BACKFILL_RANGE: "잘못된 재조회 범위",
};

/** 범위에서 유형 접두어(PRICE/STOCK)를 떼고 정제. 예: "PRICE/KOSPI/2026-01-01~2026-06-02" → "KOSPI · 2026-01-01~2026-06-02", "STOCK/2026" → "2026" */
function formatScope(t: CollectionTask): string {
  const segs = t.scope.split("/");
  const rest = segs[0] === t.type ? segs.slice(1) : segs;
  return rest.join(" · ");
}

/** 진행률·경과시간 기반 예상 남은 시간. progressAt 기준으로 계산 (현재 시각 사용 안 함). */
function etaText(t: CollectionTask): string | null {
  if (t.status !== "RUNNING" || !t.startedAt || !t.progressAt || t.done <= 0 || t.total <= 0 || t.done >= t.total) return null;
  // speed = done / (progressAt - startedAt), ETA = remaining / speed
  const elapsed = new Date(t.progressAt).getTime() - new Date(t.startedAt).getTime();
  if (elapsed <= 0) return null;
  const remainMs = (elapsed / t.done) * (t.total - t.done);
  const sec = Math.round(remainMs / 1000);
  if (sec < 60) return `~${sec}초`;
  const min = Math.round(sec / 60);
  if (min < 60) return `~${min}분`;
  return `~${Math.floor(min / 60)}시간 ${min % 60}분`;
}

function statusChip(status: CollectionTask["status"]) {
  const map = {
    PENDING: "bg-slate-500/20 text-slate-300 border-slate-500/30",
    RUNNING: "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
    DONE: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
    FAILED: "bg-rose-500/20 text-rose-300 border-rose-500/30",
  };
  const label = { PENDING: "대기", RUNNING: "실행 중", DONE: "완료", FAILED: "실패" };
  return (
    <span className={`px-2 py-0.5 rounded text-xs border ${map[status]}`}>
      {label[status]}
    </span>
  );
}

function formatDt(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("ko-KR", { timeZone: "Asia/Seoul",
    month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export default function BackfillClient() {
  const [tasks, setTasks] = useState<CollectionTask[]>([]);
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // 주가 재조회 폼
  const [priceExchange, setPriceExchange] = useState("KOSPI");
  const [priceMode, setPriceMode] = useState<"year" | "range">("year");
  const [priceYear, setPriceYear] = useState(currentYear);
  const [priceFromYear, setPriceFromYear] = useState(currentYear);
  const [priceToYear, setPriceToYear] = useState(currentYear);
  const [priceLoading, setPriceLoading] = useState(false);
  // 수정주가 재조회 여부 + 시작 연도 (여러 해 백필 시 중간은 해제, 마지막에만 체크)
  const [refetchAdjusted, setRefetchAdjusted] = useState(false);
  const [adjustedFromYear, setAdjustedFromYear] = useState(currentYear);

  // 종목 재조회 폼
  const [stockMode, setStockMode] = useState<"year" | "range">("year");
  const [stockYear, setStockYear] = useState(currentYear);
  const [stockFromYear, setStockFromYear] = useState(currentYear);
  const [stockToYear, setStockToYear] = useState(currentYear);
  const [stockLoading, setStockLoading] = useState(false);

  // 권리 이벤트 재조회 폼
  const [eventMode, setEventMode] = useState<"year" | "range">("year");
  const [eventYear, setEventYear] = useState(currentYear);
  const [eventFromYear, setEventFromYear] = useState(currentYear);
  const [eventToYear, setEventToYear] = useState(currentYear);
  const [eventLoading, setEventLoading] = useState(false);

  // 재조회 폼 펼침/접힘
  const [formOpen, setFormOpen] = useState(true);

  const [refreshing, setRefreshing] = useState(false);

  const fetchTasks = useCallback(async () => {
    const res = await fetch(`/api/admin/ops/collection/tasks?page=${page}&size=${PAGE_SIZE}`, { cache: "no-store" });
    if (res.ok) {
      const data = await res.json();
      setTasks(data.content ?? []);
      setTotalPages(Math.max(1, data.totalPages ?? 1));
      setTotalElements(data.totalElements ?? 0);
    }
    setLoadingTasks(false);
  }, [page]);

  async function handleRefresh() {
    setRefreshing(true);
    try {
      await fetchTasks();
    } finally {
      setTimeout(() => setRefreshing(false), 300);
    }
  }

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  // 실행 중 작업 있으면 3초마다 폴링
  useEffect(() => {
    const hasRunning = tasks.some(t => t.status === "PENDING" || t.status === "RUNNING");
    if (!hasRunning) return;
    const id = setInterval(fetchTasks, 3000);
    return () => clearInterval(id);
  }, [tasks, fetchTasks]);

  async function startPriceCollection() {
    if (priceMode === "range" && priceFromYear > priceToYear) return;  // 역순 차단
    setPage(0);   // 새 작업은 최신 페이지에서 보이게
    setPriceLoading(true);
    try {
      const fromY = priceMode === "range" ? priceFromYear : priceYear;
      const toY = priceMode === "range" ? priceToYear : priceYear;
      const from = `${fromY}-01-01`;
      const to = `${toY}-12-31`;
      // "전체" 선택 시 거래소별로 백필 작업을 각각 시작 (거래소당 진행률 행 분리)
      const targets = priceExchange === "ALL" ? ALL_PRICE_EXCHANGES : [priceExchange];
      for (const exchange of targets) {
        await fetch("/api/admin/ops/collection/price", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            exchange, from, to,
            adjustedFromYear: refetchAdjusted ? adjustedFromYear : null,
          }),
        });
      }
      await fetchTasks();
    } finally {
      setPriceLoading(false);
    }
  }

  async function startStockCollection() {
    if (stockMode === "range" && stockFromYear > stockToYear) return;  // 역순 차단
    setPage(0);
    setStockLoading(true);
    try {
      const fromY = stockMode === "range" ? stockFromYear : stockYear;
      const toY = stockMode === "range" ? stockToYear : stockYear;
      const res = await fetch("/api/admin/ops/collection/stock", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ from: `${fromY}-01-01`, to: `${toY}-12-31` }),
      });
      if (res.ok) await fetchTasks();
    } finally {
      setStockLoading(false);
    }
  }

  async function startEventCollection() {
    if (eventMode === "range" && eventFromYear > eventToYear) return;  // 역순 차단
    setPage(0);
    setEventLoading(true);
    try {
      const fromY = eventMode === "range" ? eventFromYear : eventYear;
      const toY = eventMode === "range" ? eventToYear : eventYear;
      const res = await fetch("/api/admin/ops/collection/event", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ from: `${fromY}-01-01`, to: `${toY}-12-31` }),
      });
      if (res.ok) await fetchTasks();
    } finally {
      setEventLoading(false);
    }
  }

  async function retryTask(id: number) {
    await fetch(`/api/admin/ops/collection/tasks/${id}`, { method: "POST" });
    await fetchTasks();
  }

  // 범위 모드에서 시작 > 종료면 잘못된 범위
  const priceRangeInvalid = priceMode === "range" && priceFromYear > priceToYear;
  const stockRangeInvalid = stockMode === "range" && stockFromYear > stockToYear;
  const eventRangeInvalid = eventMode === "range" && eventFromYear > eventToYear;

  return (
    <div className="flex flex-col min-h-full p-6 gap-6">
      <button
        type="button"
        onClick={() => setFormOpen((v) => !v)}
        className="flex items-center gap-2 flex-shrink-0 text-left group"
      >
        <h1 className="text-xl font-bold text-white">데이터 재조회</h1>
        <svg
          className={`w-5 h-5 text-slate-400 group-hover:text-white transition-transform ${formOpen ? "" : "-rotate-90"}`}
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
        <span className="text-xs text-slate-500">{formOpen ? "접기" : "펼치기"}</span>
      </button>

      {/* 폼 영역 — 컴팩트, 접기 가능 */}
      {formOpen && (
      <div className="flex-shrink-0 space-y-2">
        {/* 주가 재조회 */}
        <div className="flex flex-wrap items-end gap-3 bg-white/5 border border-white/10 rounded-xl px-4 py-3">
          <span className="text-sm font-semibold text-slate-200 w-10 self-center">주가</span>
          <label className="block">
            <span className="text-[11px] text-slate-500 mb-0.5 block">거래소</span>
            <select value={priceExchange} onChange={e => setPriceExchange(e.target.value)} className={cx.select + " w-40"}>
              {EXCHANGES.map(e => <option key={e.value} value={e.value}>{e.label}</option>)}
            </select>
          </label>
          <div className="block">
            <span className="text-[11px] text-slate-500 mb-0.5 block">기간</span>
            <div className="flex items-center gap-2">
              <div className="flex rounded-lg border border-white/15 overflow-hidden text-xs h-9">
                <button type="button" onClick={() => setPriceMode("year")}
                  className={`px-2.5 ${priceMode === "year" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>연도</button>
                <button type="button" onClick={() => setPriceMode("range")}
                  className={`px-2.5 ${priceMode === "range" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>범위</button>
              </div>
              {priceMode === "year" ? (
                <select value={priceYear} onChange={e => setPriceYear(Number(e.target.value))} className={cx.select + " w-24"}>
                  {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                </select>
              ) : (
                <div className="flex items-center gap-1.5">
                  <select value={priceFromYear} onChange={e => setPriceFromYear(Number(e.target.value))} className={cx.select + " w-24"}>
                    {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                  </select>
                  <span className="text-slate-500">~</span>
                  <select value={priceToYear} onChange={e => setPriceToYear(Number(e.target.value))} className={cx.select + " w-24"}>
                    {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                  </select>
                </div>
              )}
            </div>
            {priceRangeInvalid && (
              <span className="text-[11px] text-rose-400 mt-1 block">시작 연도가 종료보다 큽니다</span>
            )}
          </div>
          <label className="flex items-center gap-1.5 self-center text-xs text-slate-400 cursor-pointer h-9"
                 title="조정 이벤트 종목의 ADJUSTED를 시작 연도부터 재구축. 여러 해 백필 시 마지막에만 체크.">
            <input type="checkbox" checked={refetchAdjusted} onChange={e => setRefetchAdjusted(e.target.checked)} className="w-4 h-4" />
            수정주가 재조회
          </label>
          {refetchAdjusted && (
            <label className="block">
              <span className="text-[11px] text-slate-500 mb-0.5 block">시작 연도</span>
              <select value={adjustedFromYear} onChange={e => setAdjustedFromYear(Number(e.target.value))} className={cx.select + " w-32"}>
                {ADJ_YEARS.map(y => <option key={y} value={y}>{y === 1985 ? "전체(1985~)" : `${y}년~`}</option>)}
              </select>
            </label>
          )}
          <button onClick={startPriceCollection} disabled={priceLoading || priceRangeInvalid} className={cx.btnPrimary + " ml-auto"}>
            {priceLoading ? "요청 중..." : "재조회 시작"}
          </button>
        </div>

        {/* 종목 재조회 */}
        <div className="flex flex-wrap items-end gap-3 bg-white/5 border border-white/10 rounded-xl px-4 py-3">
          <span className="text-sm font-semibold text-slate-200 w-10 self-center">종목</span>
          <div className="flex items-center gap-2">
            <div className="flex rounded-lg border border-white/15 overflow-hidden text-xs h-9">
              <button type="button" onClick={() => setStockMode("year")}
                className={`px-2.5 ${stockMode === "year" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>연도</button>
              <button type="button" onClick={() => setStockMode("range")}
                className={`px-2.5 ${stockMode === "range" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>범위</button>
            </div>
            {stockMode === "year" ? (
              <select value={stockYear} onChange={e => setStockYear(Number(e.target.value))} className={cx.select + " w-24"}>
                {KRX_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
              </select>
            ) : (
              <div className="flex items-center gap-1.5">
                <select value={stockFromYear} onChange={e => setStockFromYear(Number(e.target.value))} className={cx.select + " w-24"}>
                  {KRX_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                </select>
                <span className="text-slate-500">~</span>
                <select value={stockToYear} onChange={e => setStockToYear(Number(e.target.value))} className={cx.select + " w-24"}>
                  {KRX_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                </select>
              </div>
            )}
          </div>
          {stockRangeInvalid && (
            <span className="self-center text-[11px] text-rose-400">시작 연도가 종료보다 큽니다</span>
          )}
          <span className="self-center text-[11px] text-slate-500">KRX 상장 종목 재수집 (신규 종목 반영)</span>
          <button onClick={startStockCollection} disabled={stockLoading || stockRangeInvalid} className={cx.btnPrimary + " ml-auto"}>
            {stockLoading ? "요청 중..." : "재조회 시작"}
          </button>
        </div>

        {/* 권리 이벤트 재조회 */}
        <div className="flex flex-wrap items-end gap-3 bg-white/5 border border-white/10 rounded-xl px-4 py-3">
          <span className="text-sm font-semibold text-slate-200 w-10 self-center">이벤트</span>
          <div className="flex items-center gap-2">
            <div className="flex rounded-lg border border-white/15 overflow-hidden text-xs h-9">
              <button type="button" onClick={() => setEventMode("year")}
                className={`px-2.5 ${eventMode === "year" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>연도</button>
              <button type="button" onClick={() => setEventMode("range")}
                className={`px-2.5 ${eventMode === "range" ? "bg-indigo-600 text-white" : "text-slate-400 hover:text-white"}`}>범위</button>
            </div>
            {eventMode === "year" ? (
              <select value={eventYear} onChange={e => setEventYear(Number(e.target.value))} className={cx.select + " w-24"}>
                {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
              </select>
            ) : (
              <div className="flex items-center gap-1.5">
                <select value={eventFromYear} onChange={e => setEventFromYear(Number(e.target.value))} className={cx.select + " w-24"}>
                  {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                </select>
                <span className="text-slate-500">~</span>
                <select value={eventToYear} onChange={e => setEventToYear(Number(e.target.value))} className={cx.select + " w-24"}>
                  {ADJ_YEARS.map(y => <option key={y} value={y}>{y}년</option>)}
                </select>
              </div>
            )}
          </div>
          {eventRangeInvalid && (
            <span className="self-center text-[11px] text-rose-400">시작 연도가 종료보다 큽니다</span>
          )}
          <span className="self-center text-[11px] text-slate-500">배당·유무상증자·감자·합병/분할·액면교체 (KIS 예탁원정보)</span>
          <button onClick={startEventCollection} disabled={eventLoading || eventRangeInvalid} className={cx.btnPrimary + " ml-auto"}>
            {eventLoading ? "요청 중..." : "재조회 시작"}
          </button>
        </div>
      </div>
      )}

      {/* 이력 영역 (헤더 + 테이블) */}
      <div className="flex flex-col gap-2">
      {/* 이력 헤더 — 자동 폴링 표시 + 수동 새로고침 */}
      <div className="flex items-center justify-between flex-shrink-0">
        <h2 className="text-sm font-semibold text-slate-300">
          재조회 이력 <span className="text-xs font-normal text-slate-500">({totalElements.toLocaleString()}건)</span>
        </h2>
        <div className="flex items-center gap-3">
          {tasks.some(t => t.status === "RUNNING" || t.status === "PENDING") && (
            <span className="flex items-center gap-1.5 text-xs text-slate-400">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              자동 새로고침 중 (3초)
            </span>
          )}
          <button
            onClick={handleRefresh}
            title="새로고침"
            aria-label="새로고침"
            className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition"
          >
            <svg className={`w-4 h-4 ${refreshing ? "animate-spin" : ""}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
          </button>
        </div>
      </div>

      {/* 이력 테이블 */}
      <div className="overflow-x-auto rounded-lg border border-white/10 min-h-[200px]">
        <table className={cx.table.root + " min-w-[900px]"}>
          <thead className={cx.table.head + " sticky top-0 z-10"}>
            <tr>
              <th className={cx.table.th}>유형</th>
              <th className={cx.table.th}>범위</th>
              <th className={cx.table.th}>상태</th>
              <th className={cx.table.th}>진행</th>
              <th className={cx.table.th}>시작</th>
              <th className={cx.table.th}>종료</th>
              <th className={cx.table.th}></th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {loadingTasks && (
              <tr><td colSpan={7} className="py-10 text-center text-slate-400">불러오는 중...</td></tr>
            )}
            {!loadingTasks && tasks.length === 0 && (
              <tr><td colSpan={7} className="py-10 text-center text-slate-400">이력이 없습니다.</td></tr>
            )}
            {tasks.map(t => (
              <tr key={t.id} className={cx.table.tr}>
                <td className={cx.table.td + " whitespace-nowrap"}>
                  <span className={`text-xs px-2 py-0.5 rounded border ${
                    t.type === "PRICE" ? "bg-blue-500/20 text-blue-300 border-blue-500/30"
                    : t.type === "STOCK" ? "bg-purple-500/20 text-purple-300 border-purple-500/30"
                    : "bg-amber-500/20 text-amber-300 border-amber-500/30"
                  }`}>
                    {t.type === "PRICE" ? "주가" : t.type === "STOCK" ? "종목" : "이벤트"}
                  </span>
                </td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>{formatScope(t)}</td>
                <td className={cx.table.td + " whitespace-nowrap"}>{statusChip(t.status)}</td>
                <td className={cx.table.td}>
                  {t.status === "RUNNING" || t.status === "PENDING" ? (
                    <div className="flex flex-col gap-0.5 min-w-[140px]">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-1.5 rounded-full bg-white/10 overflow-hidden">
                          <div className="h-full bg-indigo-500 transition-all" style={{ width: `${t.progressPercent}%` }} />
                        </div>
                        <span className="text-xs text-slate-400 tabular-nums">{t.progressPercent}%</span>
                      </div>
                      {etaText(t) && (
                        <span className="text-[10px] text-slate-500 tabular-nums">남은 시간 {etaText(t)}</span>
                      )}
                      {t.adjustedTotal > 0 && (
                        <span className="text-[10px] text-amber-400 tabular-nums">
                          수정주가 재조회 {t.adjustedDone.toLocaleString()} / {t.adjustedTotal.toLocaleString()}
                        </span>
                      )}
                    </div>
                  ) : t.total > 0 ? (
                    <div className="flex flex-col gap-0.5">
                      <span className="text-xs text-slate-400 tabular-nums">{t.done.toLocaleString()} / {t.total.toLocaleString()}</span>
                      {t.adjustedTotal > 0 && (
                        <span className="text-[10px] text-amber-400 tabular-nums">수정주가 재조회 {t.adjustedTotal.toLocaleString()}종목</span>
                      )}
                    </div>
                  ) : "—"}
                </td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>{formatDt(t.startedAt)}</td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>{formatDt(t.finishedAt)}</td>
                <td className={cx.table.td + " min-w-[240px]"}>
                  <div className="flex items-center gap-2">
                    {t.errorCode && (
                      <span className="text-xs text-rose-400 break-keep" title={t.errorDetail ?? ""}>
                        {ERROR_LABEL[t.errorCode] ?? t.errorCode}
                      </span>
                    )}
                    {t.status === "FAILED" && (
                      <button onClick={() => retryTask(t.id)}
                        className="text-xs px-2 py-1 rounded border border-white/15 text-slate-400 hover:text-white transition flex-shrink-0">
                        재시도
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 페이지 컨트롤 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 flex-shrink-0 pt-1">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page <= 0}
            className="px-3 py-1.5 text-xs rounded-lg border border-white/15 text-slate-300 hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed"
          >이전</button>
          <span className="text-xs text-slate-400 tabular-nums px-2">{page + 1} / {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-3 py-1.5 text-xs rounded-lg border border-white/15 text-slate-300 hover:bg-white/5 transition disabled:opacity-30 disabled:cursor-not-allowed"
          >다음</button>
        </div>
      )}
      </div>
    </div>
  );
}
