"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { cx } from "@/utils/cx";
import { StockSet } from "@/types/filter";

interface Props {
  stockSets: StockSet[];
}

export default function StockSetListClient({ stockSets }: Props) {
  const router = useRouter();
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await fetch(`/api/stock-sets/${id}`, { method: "DELETE" });
      router.refresh();
    } finally {
      setDeletingId(null);
      setConfirmId(null);
    }
  }

  return (
    <>
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-6 py-5 border-b border-white/10 flex-shrink-0">
        <div>
          <h1 className="text-xl font-semibold text-white">종목 필터</h1>
          <p className="text-sm text-slate-400 mt-0.5">검색 필터에 포함·제외 조건으로 사용할 종목 목록을 관리하세요</p>
        </div>
        <button
          onClick={() => router.push("/stock-sets/new")}
          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white transition"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          새 종목 필터
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5">
        {stockSets.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center py-20">
            <div className="w-14 h-14 rounded-full bg-slate-800 flex items-center justify-center mb-4">
              <svg className="w-7 h-7 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <line x1="9" y1="9" x2="15" y2="9" />
                <line x1="9" y1="12" x2="15" y2="12" />
                <line x1="9" y1="15" x2="12" y2="15" />
              </svg>
            </div>
            <p className="text-slate-400 font-medium">등록된 종목 필터가 없습니다</p>
            <p className="text-slate-500 text-sm mt-1">포함·제외에 쓸 종목 목록을 만들어보세요</p>
            <button
              onClick={() => router.push("/stock-sets/new")}
              className="mt-5 px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white transition"
            >
              첫 종목 필터 만들기
            </button>
          </div>
        ) : (
          <div className="grid gap-3 max-w-2xl">
            {stockSets.map((s) => (
              <div
                key={s.id}
                className="bg-slate-800/60 border border-white/10 rounded-xl px-5 py-4 flex items-start gap-4 hover:border-white/20 transition"
              >
                <div className="w-10 h-10 rounded-lg bg-emerald-900/40 border border-emerald-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg className="w-5 h-5 text-emerald-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <line x1="9" y1="9" x2="15" y2="9" />
                    <line x1="9" y1="12" x2="15" y2="12" />
                    <line x1="9" y1="15" x2="12" y2="15" />
                  </svg>
                </div>

                <div className="flex-1 min-w-0">
                  <p className="text-white font-medium truncate">{s.name}</p>
                  <p className="text-xs text-slate-500 mt-1">
                    {s.codes.length}개 종목 ·{" "}
                    <span className="font-mono text-slate-400">
                      {s.codes.slice(0, 5).join(", ")}
                      {s.codes.length > 5 ? ` +${s.codes.length - 5}` : ""}
                    </span>
                  </p>
                  <p className="text-xs text-slate-600 mt-0.5">
                    {new Date(s.updatedAt).toLocaleDateString("ko-KR")} 수정
                  </p>
                </div>

                <div className="flex items-center gap-2 flex-shrink-0">
                  <button
                    onClick={() => router.push(`/stock-sets/${s.id}/edit`)}
                    className="px-3 py-1.5 rounded-lg text-xs text-slate-400 border border-white/15 hover:text-white hover:border-white/30 transition"
                  >
                    수정
                  </button>
                  <button
                    onClick={() => setConfirmId(s.id)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-red-400 hover:bg-red-900/20 transition"
                    title="삭제"
                  >
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="3 6 5 6 21 6" />
                      <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                      <path d="M10 11v6M14 11v6" />
                      <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>

    {confirmId !== null && (() => {
      const target = stockSets.find((s) => s.id === confirmId);
      return (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setConfirmId(null)}
        >
          <div
            className="bg-slate-800 border border-white/15 rounded-xl p-6 w-80 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-white font-semibold mb-2">종목 필터 삭제</h3>
            <p className="text-sm text-slate-400 mb-5">
              <span className="text-white font-medium">{target?.name}</span>을 삭제합니다.
              <br />
              <span className="text-slate-500">이 작업은 되돌릴 수 없습니다.</span>
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setConfirmId(null)}
                className={`flex-1 ${cx.btnSecondary}`}
              >
                취소
              </button>
              <button
                onClick={() => handleDelete(confirmId)}
                disabled={deletingId === confirmId}
                className="flex-1 px-5 py-2 rounded-lg text-sm font-medium bg-red-700 hover:bg-red-600 text-white transition disabled:opacity-50"
              >
                {deletingId === confirmId ? "삭제 중..." : "삭제"}
              </button>
            </div>
          </div>
        </div>
      );
    })()}
    </>
  );
}

