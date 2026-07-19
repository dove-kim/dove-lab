"use client";

import { useEffect, useState } from "react";
import { type PortfolioTx, TX_TYPE_LABEL, natMoney } from "@/types/portfolio";

interface PortfolioSummary {
  totalKrw: number;
  cashKrw: number;
  netContribKrw: number;
  growthKrw: number;
  evalPnlKrw: number;
  evalPnlPct: number;
  xirrPct: number;
}

const won = (n: number) => n.toLocaleString("ko-KR");
const signed = (n: number) => (n >= 0 ? "+" : "") + won(n);

function MarketStatusCard() {
  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
        시장 현황
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">시장 데이터 준비 중</p>
      </div>
    </div>
  );
}

function Stat({ label, value, sub, tone }: { label: string; value: React.ReactNode; sub?: string; tone?: "up" | "down" }) {
  const color = tone === "up" ? "text-rose-300" : tone === "down" ? "text-sky-300" : "text-white";
  return (
    <div className="bg-white/5 rounded-lg p-3">
      <div className="text-xs text-slate-400">{label}</div>
      <div className={`text-lg font-semibold tabular-nums ${color}`}>{value}</div>
      {sub && <div className="text-xs text-slate-500 mt-0.5">{sub}</div>}
    </div>
  );
}

function PortfolioSummaryCard({ reloadToken }: { reloadToken: number }) {
  const [s, setS] = useState<PortfolioSummary | null>(null);
  const [err, setErr] = useState(false);
  useEffect(() => {
    let live = true;
    fetch("/api/portfolio/summary", { cache: "no-store" })
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: PortfolioSummary) => live && setS(d))
      .catch(() => live && setErr(true));
    return () => {
      live = false;
    };
  }, [reloadToken]);

  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5 h-full">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-slate-300 text-sm font-medium flex items-center gap-2">
          <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" /><path d="M16 7V5a2 2 0 0 0-4 0v2" /><path d="M12 12v4" /><path d="M10 14h4" />
          </svg>
          포트폴리오 요약
        </h3>
        <a href="/portfolio" className="text-xs text-indigo-300 hover:text-indigo-200 transition">포트폴리오 →</a>
      </div>
      {err && <p className="text-rose-400 text-xs">불러오지 못했습니다.</p>}
      {!err && !s && <p className="text-slate-500 text-xs">불러오는 중…</p>}
      {s && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <Stat label="총 평가자산" value={won(s.totalKrw)} sub={`현금 ${won(s.cashKrw)} 포함`} />
          <Stat label="누적 손익" value={signed(s.growthKrw)} tone={s.growthKrw >= 0 ? "up" : "down"} sub={`순납입 ${won(s.netContribKrw)}`} />
          <Stat label="평가손익" value={`${signed(s.evalPnlKrw)} (${s.evalPnlPct >= 0 ? "+" : ""}${s.evalPnlPct}%)`} tone={s.evalPnlKrw >= 0 ? "up" : "down"} />
          <Stat label="연평균(XIRR)" value={`${s.xirrPct >= 0 ? "+" : ""}${s.xirrPct}%`} tone={s.xirrPct >= 0 ? "up" : "down"} />
        </div>
      )}
    </div>
  );
}

function WatchlistCard() {
  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
        </svg>
        관심 종목
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">관심 종목 기능 준비 중</p>
      </div>
    </div>
  );
}

function RecentTradesCard({ reloadToken }: { reloadToken: number }) {
  const [rows, setRows] = useState<PortfolioTx[] | null>(null);
  useEffect(() => {
    let live = true;
    fetch("/api/portfolio/transactions", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((d: PortfolioTx[]) => live && setRows(d.slice(0, 6)))
      .catch(() => live && setRows([]));
    return () => {
      live = false;
    };
  }, [reloadToken]);

  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5 h-full">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-slate-300 text-sm font-medium flex items-center gap-2">
          <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
          </svg>
          최근 거래
        </h3>
        <a href="/portfolio" className="text-xs text-indigo-300 hover:text-indigo-200 transition">전체 →</a>
      </div>
      {!rows && <p className="text-slate-500 text-xs">불러오는 중…</p>}
      {rows && rows.length === 0 && <p className="text-slate-500 text-xs">거래 내역 없음</p>}
      {rows &&
        rows.map((r) => (
          <div key={r.id} className="flex items-center justify-between py-1.5 text-sm border-b border-white/5 last:border-0">
            <div className="flex items-center gap-2 min-w-0">
              <span className="text-xs text-slate-500 tabular-nums">{r.tradedAt.slice(5)}</span>
              <span className="text-slate-200 truncate">{r.symbol ?? TX_TYPE_LABEL[r.type]}</span>
            </div>
            <span className="text-xs text-slate-400 tabular-nums whitespace-nowrap">
              {TX_TYPE_LABEL[r.type]} {natMoney(r.amount, r.currency)}
            </span>
          </div>
        ))}
    </div>
  );
}

interface JobStatus {
  name: string;
  state: "RUNNING" | "COMPLETED" | "FAILED";
  total: number;
  processed: number;
  startedAtEpochMs: number;
  updatedAtEpochMs: number;
  message: string | null;
}

const JOB_LABEL: Record<string, string> = {
  STOCK_SYNC: "종목 동기화",
  STOCK_DETAIL: "종목 상세",
  INVESTOR_FLOW: "투자자 동향",
  DAILY_PRICE: "일일 주가",
  INDICATOR: "지표 계산",
  RANK: "순위 계산",
  CUSTOM_METRIC: "커스텀 지표",
  MODEL_SCORING: "모델 채점",
  FUNDAMENTAL_POLL: "재무 수집",
  SHARE_COUNT: "상장주식수",
  VALUATION: "밸류에이션",
  PORTFOLIO_QUOTE: "포트폴리오 시세",
  PORTFOLIO_FX: "포트폴리오 환율",
};

/** 진행 수치 단위 */
const JOB_UNIT: Record<string, string> = {
  STOCK_SYNC: "일×시장",
  STOCK_DETAIL: "종목",
  INVESTOR_FLOW: "종목",
  DAILY_PRICE: "시장",
  INDICATOR: "그룹",
  RANK: "그룹",
  CUSTOM_METRIC: "지표",
  MODEL_SCORING: "종목",
  FUNDAMENTAL_POLL: "종목",
  SHARE_COUNT: "종목",
  VALUATION: "종목",
  PORTFOLIO_QUOTE: "종목",
  PORTFOLIO_FX: "통화",
};

function usePolling<T>(url: string, intervalMs: number, reloadToken = 0) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const res = await fetch(url, { cache: "no-store" });
        if (!res.ok) { if (active) setError("로드 실패"); return; }
        if (active) { setData(await res.json()); setError(null); }
      } catch { if (active) setError("로드 실패"); }
    };
    load();
    const id = setInterval(load, intervalMs);
    return () => { active = false; clearInterval(id); };
  }, [url, intervalMs, reloadToken]);
  return { data, error };
}

function SchedulerStatusCard({ reloadToken }: { reloadToken: number }) {
  const { data: jobs, error } = usePolling<JobStatus[]>("/api/admin/ops/scheduler-status", 15_000, reloadToken);

  function stateChip(state: JobStatus["state"]) {
    if (state === "RUNNING") return <span className="px-1.5 py-0.5 rounded text-xs bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">실행 중</span>;
    if (state === "COMPLETED") return <span className="px-1.5 py-0.5 rounded text-xs bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">완료</span>;
    return <span className="px-1.5 py-0.5 rounded text-xs bg-rose-500/20 text-rose-300 border border-rose-500/30">실패</span>;
  }

  function formatTime(epochMs: number) {
    if (!epochMs) return "-";
    return new Date(epochMs).toLocaleString("ko-KR", { timeZone: "Asia/Seoul", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit" });
  }

  /** 진행률·경과시간 기반 예상 남은 시간. 계산 불가하면 null. */
  function jobEta(job: JobStatus): string | null {
    if (job.state !== "RUNNING" || job.total <= 0 || job.processed <= 0 || job.processed >= job.total) return null;
    const elapsed = job.updatedAtEpochMs - job.startedAtEpochMs;
    if (elapsed <= 0) return null;
    const remainMs = (elapsed / job.processed) * (job.total - job.processed);
    const sec = Math.round(remainMs / 1000);
    if (sec < 60) return `~${sec}초`;
    const min = Math.round(sec / 60);
    if (min < 60) return `~${min}분`;
    return `~${Math.floor(min / 60)}시간 ${min % 60}분`;
  }

  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5 h-full">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
        </svg>
        배치 작업 현황
      </h3>
      {error && <p className="text-rose-400 text-xs">{error}</p>}
      {!error && !jobs && <p className="text-slate-500 text-xs">불러오는 중...</p>}
      {jobs && jobs.length === 0 && <p className="text-slate-500 text-xs">기록된 작업 없음</p>}
      {jobs && jobs.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-slate-500 border-b border-white/10">
                <th className="text-left pb-2 font-medium">작업</th>
                <th className="text-left pb-2 font-medium">상태</th>
                <th className="text-right pb-2 font-medium">진행</th>
                <th className="text-right pb-2 font-medium">예상 종료</th>
                <th className="text-right pb-2 font-medium">갱신</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {jobs.map((job) => {
                const pct = job.total > 0 ? Math.round((job.processed / job.total) * 100) : null;
                // 진행: 수치가 있으면 "처리/전체 단위 (n%)", 없으면 상태별 표현
                const progressText = pct !== null
                  ? `${job.processed.toLocaleString()} / ${job.total.toLocaleString()} ${JOB_UNIT[job.name] ?? ""} (${pct}%)`
                  : job.state === "RUNNING" ? "진행 중…"
                  : job.state === "COMPLETED" ? "완료"
                  : "-";
                return (
                  <tr key={job.name} className="text-slate-300 align-top">
                    <td className="py-2 pr-3 whitespace-nowrap">{JOB_LABEL[job.name] ?? job.name}</td>
                    <td className="py-2 pr-3">{stateChip(job.state)}</td>
                    <td className="py-2 pr-3 text-right tabular-nums">
                      {progressText}
                      {job.state === "RUNNING" && pct !== null && (
                        <div className="mt-1 h-1 w-full min-w-[80px] rounded bg-white/10 overflow-hidden">
                          <div className="h-full bg-indigo-400" style={{ width: `${pct}%` }} />
                        </div>
                      )}
                      {job.state === "FAILED" && job.message && (
                        <div className="mt-0.5 text-left text-rose-400/80 font-normal normal-case whitespace-normal break-words max-w-[220px]">
                          {job.message}
                        </div>
                      )}
                    </td>
                    <td className="py-2 pr-3 text-right text-slate-400 tabular-nums whitespace-nowrap">{jobEta(job) ?? "-"}</td>
                    <td className="py-2 text-right text-slate-400 tabular-nums whitespace-nowrap">{formatTime(job.updatedAtEpochMs)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function Dashboard({ role }: { role: string }) {
  const isRoot = role === "ROOT";
  const [reloadToken, setReloadToken] = useState(0);
  const [spinning, setSpinning] = useState(false);

  function handleRefresh() {
    setReloadToken((t) => t + 1);
    setSpinning(true);
    setTimeout(() => setSpinning(false), 600);
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="flex items-center gap-2 mb-6">
        <h2 className="text-white text-lg font-semibold">대시보드</h2>
        <div className="ml-auto flex items-center gap-3">
          {isRoot && (
            <span className="flex items-center gap-1.5 text-xs text-slate-400">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              자동 새로고침 중
            </span>
          )}
          <button
            type="button"
            onClick={handleRefresh}
            title="새로고침"
            aria-label="새로고침"
            className="w-9 h-9 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition"
          >
            <svg
              className={`w-5 h-5 ${spinning ? "animate-spin" : ""}`}
              viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"
            >
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
          </button>
        </div>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {isRoot && (
          <div className="md:col-span-2 lg:col-span-3">
            <SchedulerStatusCard reloadToken={reloadToken} />
          </div>
        )}
        <div className="md:col-span-2">
          <PortfolioSummaryCard reloadToken={reloadToken} />
        </div>
        <RecentTradesCard reloadToken={reloadToken} />
        {!isRoot && (
          <>
            <div className="md:col-span-2">
              <MarketStatusCard />
            </div>
            <WatchlistCard />
          </>
        )}
      </div>
    </div>
  );
}
