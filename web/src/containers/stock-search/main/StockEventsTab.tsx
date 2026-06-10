"use client";

import { useEffect, useState } from "react";

interface StockEvent {
  eventType: string;
  eventTypeLabel: string;
  eventDate: string;
  summary: string | null;
}

const TYPE_COLOR: Record<string, string> = {
  DIVIDEND: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
  RIGHTS_ISSUE: "bg-blue-500/20 text-blue-300 border-blue-500/30",
  BONUS_ISSUE: "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
  MERGER_SPLIT: "bg-purple-500/20 text-purple-300 border-purple-500/30",
  PAR_CHANGE: "bg-amber-500/20 text-amber-300 border-amber-500/30",
  CAP_REDUCTION: "bg-rose-500/20 text-rose-300 border-rose-500/30",
};

/**
 * 권리 이벤트 탭 — 배당·유무상증자·감자·합병/분할·액면교체를 최신순 리스트로.
 */
export default function StockEventsTab({ code }: { code: string }) {
  const [events, setEvents] = useState<StockEvent[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`/api/stocks/${code}/events`)
      .then((res) => (res.ok ? res.json() : Promise.reject()))
      .then((data: StockEvent[]) => setEvents(Array.isArray(data) ? data : []))
      .catch(() => setError("권리 이벤트를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [code]);

  if (loading) return <div className="p-6 text-sm text-slate-500">불러오는 중...</div>;
  if (error) return <div className="p-6 text-sm text-rose-400">{error}</div>;
  if (!events || events.length === 0) {
    return <div className="p-6 text-sm text-slate-500">기록된 권리 이벤트가 없습니다.</div>;
  }

  return (
    <div className="h-full overflow-y-auto px-5 py-4">
      <p className="text-[11px] text-slate-500 mb-3">
        날짜는 <span className="text-slate-300">배정기준일</span> 기준입니다. (배당 등 <span className="text-slate-300">권리락일</span>은 보통 기준일의 직전 거래일)
      </p>
      <div className="flex items-center gap-3 pb-1.5 mb-1 border-b border-white/10 text-[11px] text-slate-500">
        <span className="w-24 flex-shrink-0">기준일</span>
        <span className="w-20 flex-shrink-0">유형</span>
        <span>내용</span>
      </div>
      {events.map((e, i) => (
        <div key={i} className="flex items-start gap-3 py-2 border-b border-white/5">
          <span className="text-xs text-slate-400 font-mono tabular-nums w-24 flex-shrink-0 mt-0.5">
            {e.eventDate}
          </span>
          <span className={`text-xs px-2 py-0.5 rounded border flex-shrink-0 whitespace-nowrap ${TYPE_COLOR[e.eventType] ?? "bg-slate-500/20 text-slate-300 border-slate-500/30"}`}>
            {e.eventTypeLabel}
          </span>
          <span className="text-sm text-slate-200 break-keep">{e.summary ?? "-"}</span>
        </div>
      ))}
    </div>
  );
}
