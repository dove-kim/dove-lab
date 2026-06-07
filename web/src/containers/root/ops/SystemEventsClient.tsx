"use client";

import { useState } from "react";
import type { SystemEvent, SystemEventPage } from "@/types/ops";
import { cx } from "@/utils/cx";

const EVENT_TYPE_LABEL: Record<string, string> = {
  KRX_API_FAILURE: "KRX API 실패",
  KIS_API_FAILURE: "KIS API 실패",
  KRX_RATE_LIMIT_EXCEEDED: "KRX 한도 초과",
};

const EVENT_TYPE_COLOR: Record<string, string> = {
  KRX_API_FAILURE: "bg-rose-500/20 text-rose-300 border border-rose-500/30",
  KIS_API_FAILURE: "bg-amber-500/20 text-amber-300 border border-amber-500/30",
  KRX_RATE_LIMIT_EXCEEDED: "bg-orange-500/20 text-orange-300 border border-orange-500/30",
};

function formatKst(iso: string) {
  return new Date(iso).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" });
}

const PAGE_SIZES = [10, 20, 30, 40, 50];

interface Props {
  initialPage: SystemEventPage;
}

export default function SystemEventsClient({ initialPage }: Props) {
  const [page, setPage] = useState<SystemEventPage>(initialPage);
  const [size, setSize] = useState(10);
  const [loading, setLoading] = useState(false);

  async function loadPage(pageNum: number, pageSize = size) {
    setLoading(true);
    try {
      const res = await fetch(`/api/admin/ops/system-events?page=${pageNum}&size=${pageSize}`);
      if (res.ok) setPage(await res.json());
    } finally {
      setLoading(false);
    }
  }

  function changeSize(newSize: number) {
    setSize(newSize);
    loadPage(0, newSize);
  }

  // 현재 페이지 주변 번호 최대 5개
  function pageNumbers() {
    const total = page.totalPages;
    const cur = page.number;
    const delta = 2;
    const start = Math.max(0, cur - delta);
    const end = Math.min(total - 1, cur + delta);
    const nums: number[] = [];
    for (let i = start; i <= end; i++) nums.push(i);
    return nums;
  }

  return (
    <div className="flex flex-col min-h-full p-6 gap-4">
      {/* 헤더 + 페이지네이션 */}
      <div className="flex items-center justify-between flex-shrink-0 gap-4">
        <div>
          <h1 className="text-xl font-bold text-white">시스템 이벤트</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            총 <strong className="text-white">{page.totalElements}</strong>건
          </p>
        </div>
        <div className="flex items-center gap-2">
          {/* 페이지네이션 */}
          {page.totalPages > 1 && (
            <div className="flex items-center gap-1">
              <button onClick={() => loadPage(0)} disabled={page.number === 0 || loading}
                className="px-2 py-1.5 rounded text-sm text-slate-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
              <button onClick={() => loadPage(page.number - 1)} disabled={page.number === 0 || loading}
                className="px-2 py-1.5 rounded text-sm text-slate-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
              {pageNumbers().map((n) => (
                <button key={n} onClick={() => loadPage(n)} disabled={loading}
                  className={`px-3 py-1.5 rounded text-sm transition ${n === page.number ? "bg-indigo-600/25 text-indigo-300 border border-indigo-500/30" : "text-slate-400 hover:text-white hover:bg-white/5"}`}>
                  {n + 1}
                </button>
              ))}
              <button onClick={() => loadPage(page.number + 1)} disabled={page.number >= page.totalPages - 1 || loading}
                className="px-2 py-1.5 rounded text-sm text-slate-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
              <button onClick={() => loadPage(page.totalPages - 1)} disabled={page.number >= page.totalPages - 1 || loading}
                className="px-2 py-1.5 rounded text-sm text-slate-400 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
              <span className="ml-1 text-xs text-slate-500">{page.number + 1} / {page.totalPages}</span>
            </div>
          )}
          {/* 페이지 크기 */}
          <select value={size} onChange={(e) => changeSize(Number(e.target.value))}
            className="bg-slate-800 border border-white/10 text-slate-300 text-sm rounded-lg px-3 py-1.5 cursor-pointer">
            {PAGE_SIZES.map((s) => <option key={s} value={s}>{s}개씩</option>)}
          </select>
        </div>
      </div>

      {/* 테이블 — 페이지 스크롤, 가로만 내부 스크롤 */}
      <div className="overflow-x-auto rounded-lg border border-white/10 min-h-[200px]">
        <table className={cx.table.root + " min-w-[640px]"}>
          <thead className={cx.table.head + " sticky top-0 z-10"}>
            <tr>
              <th className={cx.table.th + " whitespace-nowrap"}>발생 일시</th>
              <th className={cx.table.th + " whitespace-nowrap"}>종류</th>
              <th className={cx.table.th + " whitespace-nowrap"}>시장/소스</th>
              <th className={cx.table.th}>상세</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {page.content.length === 0 && (
              <tr>
                <td colSpan={4} className="py-10 text-center text-slate-400">
                  이벤트가 없습니다.
                </td>
              </tr>
            )}
            {page.content.map((ev: SystemEvent) => (
              <tr key={ev.id} className={cx.table.tr}>
                <td className={cx.table.td + " whitespace-nowrap"}>{formatKst(ev.occurredAt)}</td>
                <td className={cx.table.td + " whitespace-nowrap"}>
                  <span className={`rounded px-2 py-0.5 text-xs font-medium ${EVENT_TYPE_COLOR[ev.eventType] ?? "bg-slate-500/20 text-slate-300"}`}>
                    {EVENT_TYPE_LABEL[ev.eventType] ?? ev.eventType}
                  </span>
                </td>
                <td className={cx.table.td + " whitespace-nowrap"}>
                  {ev.marketType ?? ev.detail?.source ?? "—"}
                </td>
                <td className={cx.table.td}>
                  <pre className="whitespace-pre-wrap break-all text-xs text-slate-400 max-w-lg">
                    {JSON.stringify(ev.detail, null, 2)}
                  </pre>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

    </div>
  );
}
