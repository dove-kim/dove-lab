"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";
import { StockFilterResponse } from "@/types/stock-filter";
import StockFilterEditor from "./StockFilterEditor";

interface Props {
  role: string;
}

type Tab = "personal" | "system";

export default function StockFiltersClient({ role }: Props) {
  const isAdmin = role === "ADMIN" || role === "ROOT";
  const isRoot = role === "ROOT";
  // ROOT는 공통(시스템) 필터만 다룸 — 내 필터 없음
  const [tab, setTab] = useState<Tab>(isRoot ? "system" : "personal");
  const [personal, setPersonal] = useState<StockFilterResponse[]>([]);
  const [system, setSystem] = useState<StockFilterResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 편집 모달 상태
  const [editing, setEditing] = useState<{
    mode: "create" | "edit" | "view";
    scope: "system" | "personal";
    initial?: StockFilterResponse;
  } | null>(null);

  async function loadPersonal() {
    setLoading(true);
    try {
      const res = await fetch("/api/stock-filters/personal");
      if (res.ok) setPersonal(await res.json());
      else setError("개인 필터 로드 실패");
    } finally {
      setLoading(false);
    }
  }

  async function loadSystem() {
    setLoading(true);
    try {
      const res = await fetch("/api/stock-filters/system");
      if (res.ok) setSystem(await res.json());
      else setError("시스템 필터 로드 실패");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setError(null);
    if (tab === "personal") loadPersonal();
    else loadSystem();
  }, [tab]);

  async function toggleEnabled(f: StockFilterResponse) {
    if (!isAdmin) return;
    const res = await fetch(`/api/admin/stock-filters/system/${f.id}/enabled`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ enabled: !f.enabled }),
    });
    if (res.ok) {
      const updated: StockFilterResponse = await res.json();
      setSystem((arr) => arr.map((x) => (x.id === updated.id ? updated : x)));
    }
  }

  function openCreate(scope: "system" | "personal") {
    setEditing({ mode: "create", scope });
  }

  function openCardForView(f: StockFilterResponse) {
    if (f.scope === "SYSTEM") {
      setEditing({ mode: isAdmin ? "edit" : "view", scope: "system", initial: f });
    } else {
      setEditing({ mode: "edit", scope: "personal", initial: f });
    }
  }

  function handleSaved(saved: StockFilterResponse) {
    if (saved.scope === "SYSTEM") {
      setSystem((arr) => {
        const idx = arr.findIndex((x) => x.id === saved.id);
        if (idx === -1) return [...arr, saved];
        const next = arr.slice();
        next[idx] = saved;
        return next;
      });
    } else {
      setPersonal((arr) => {
        const idx = arr.findIndex((x) => x.id === saved.id);
        if (idx === -1) return [...arr, saved];
        const next = arr.slice();
        next[idx] = saved;
        return next;
      });
    }
    setEditing(null);
  }

  function handleDeleted(id: number) {
    setPersonal((arr) => arr.filter((x) => x.id !== id));
    setSystem((arr) => arr.filter((x) => x.id !== id));
    setEditing(null);
  }

  const list = tab === "personal" ? personal : system;

  return (
    <div className="p-4 sm:p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold text-white">종목 필터</h1>
        <div className="flex gap-2">
          {tab === "personal" && (
            <button onClick={() => openCreate("personal")} className={cx.btnPrimary}>
              + 새 내 필터
            </button>
          )}
          {tab === "system" && isAdmin && (
            <button onClick={() => openCreate("system")} className={cx.btnPrimary}>
              + 새 시스템 필터
            </button>
          )}
        </div>
      </div>

      {/* 탭 — ROOT는 공통 필터만 */}
      <div className="flex gap-2 mb-4 border-b border-white/10">
        {!isRoot && (
          <TabButton active={tab === "personal"} onClick={() => setTab("personal")}>내 필터</TabButton>
        )}
        <TabButton active={tab === "system"} onClick={() => setTab("system")}>시스템 필터</TabButton>
      </div>

      {error && <div className="text-sm text-rose-300 mb-3">{error}</div>}
      {loading && <div className="text-sm text-slate-400">불러오는 중...</div>}

      {!loading && list.length === 0 && (
        <div className="text-sm text-slate-500 py-12 text-center border border-dashed border-white/10 rounded-lg">
          {tab === "personal" ? "아직 생성한 필터가 없습니다" : "시스템 필터가 없습니다"}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {list.map((f) => (
          <FilterCard
            key={f.id}
            filter={f}
            isAdmin={isAdmin}
            onClick={() => openCardForView(f)}
            onToggleEnabled={() => toggleEnabled(f)}
          />
        ))}
      </div>

      {editing && (
        <StockFilterEditor
          mode={editing.mode}
          scope={editing.scope}
          initial={editing.initial}
          onSaved={handleSaved}
          onClose={() => setEditing(null)}
          canDelete={editing.mode === "edit" && (editing.scope === "personal" || isAdmin)}
          onDeleted={handleDeleted}
        />
      )}
    </div>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 text-sm transition border-b-2 ${
        active ? "border-indigo-400 text-white" : "border-transparent text-slate-400 hover:text-white"
      }`}
    >
      {children}
    </button>
  );
}

function FilterCard({
  filter,
  isAdmin,
  onClick,
  onToggleEnabled,
}: {
  filter: StockFilterResponse;
  isAdmin: boolean;
  onClick: () => void;
  onToggleEnabled: () => void;
}) {
  return (
    <div
      onClick={onClick}
      className="cursor-pointer bg-white/5 border border-white/10 rounded-xl p-4 hover:bg-white/8 hover:border-white/20 transition"
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="min-w-0">
          <div className="text-white font-medium truncate">{filter.name}</div>
          {filter.description && (
            <div className="text-xs text-slate-400 truncate mt-0.5">{filter.description}</div>
          )}
        </div>
        {filter.scope === "SYSTEM" && (
          <span
            onClick={(e) => {
              e.stopPropagation();
              if (isAdmin) onToggleEnabled();
            }}
            className={`text-xs px-2 py-1 rounded ${
              filter.enabled
                ? "bg-emerald-600/20 text-emerald-300 border border-emerald-500/30"
                : "bg-slate-600/20 text-slate-400 border border-slate-500/30"
            } ${isAdmin ? "cursor-pointer hover:opacity-80" : ""}`}
          >
            {filter.enabled ? "활성" : "비활성"}
          </span>
        )}
      </div>

      <div className="flex flex-wrap gap-1 mt-2">
        {filter.tagConditions.slice(0, 4).map((c, i) => (
          <span
            key={i}
            className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] ${
              c.mode === "INCLUDE"
                ? "bg-indigo-600/15 text-indigo-200 border border-indigo-500/30"
                : "bg-rose-600/15 text-rose-200 border border-rose-500/30"
            }`}
            title={`${c.field}: ${c.value}`}
          >
            {c.mode === "INCLUDE" ? "+" : "−"} {c.value}
          </span>
        ))}
        {filter.tagConditions.length > 4 && (
          <span className="text-[10px] text-slate-500">+{filter.tagConditions.length - 4}</span>
        )}
      </div>

      <div className="flex gap-3 mt-3 text-[10px] text-slate-500">
        <span>태그 {filter.tagConditions.length}</span>
        <span>수치 {filter.numericConditions?.length ?? 0}</span>
        <span>EXCLUDE {filter.stockConditions.filter((c) => c.mode === "EXCLUDE").length}</span>
        <span>INCLUDE {filter.stockConditions.filter((c) => c.mode === "INCLUDE").length}</span>
      </div>
    </div>
  );
}
