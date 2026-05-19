"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { clientFetch } from "@/services/client";
import {
  DndContext, closestCenter, PointerSensor, useSensor, useSensors, type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext, useSortable, verticalListSortingStrategy, arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { SearchFilter, GroupNode, DATE_RULE_LABELS, DateRule, StockSetSummary } from "@/types/filter";

interface Props {
  filters: SearchFilter[];
  stockSets: StockSetSummary[];
}

function countNodes(node: GroupNode): number {
  return node.children.reduce((acc, c) => {
    if (c.nodeType === "GROUP") return acc + countNodes(c);
    return acc + 1;
  }, 0);
}

function parseRoot(expression: string): GroupNode | null {
  try { return JSON.parse(expression) as GroupNode; }
  catch { return null; }
}

function FilterCard({
  filter,
  setNameMap,
  confirmId,
  deletingId,
  onConfirm,
  onCancelConfirm,
  onDelete,
  onEdit,
  onSearch,
}: {
  filter: SearchFilter;
  setNameMap: Map<number, string>;
  confirmId: number | null;
  deletingId: number | null;
  onConfirm: (id: number) => void;
  onCancelConfirm: () => void;
  onDelete: (id: number) => void;
  onEdit: (id: number) => void;
  onSearch: (id: number) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: filter.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const root = parseRoot(filter.expression);
  const condCount = root ? countNodes(root) : 0;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="bg-slate-800/60 border border-white/10 rounded-xl px-5 py-4 flex items-start gap-3 hover:border-white/20 transition"
    >
      {/* 드래그 핸들 */}
      <button
        className="flex-shrink-0 pt-1.5 text-slate-600 hover:text-slate-400 cursor-grab active:cursor-grabbing transition"
        {...attributes}
        {...listeners}
        title="드래그로 순서 변경"
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <rect x="4" y="6"  width="16" height="2" rx="1"/>
          <rect x="4" y="11" width="16" height="2" rx="1"/>
          <rect x="4" y="16" width="16" height="2" rx="1"/>
        </svg>
      </button>

      {/* 아이콘 */}
      <div className="w-10 h-10 rounded-lg bg-slate-700/40 border border-indigo-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
        <svg className="w-5 h-5 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
          <line x1="4" y1="6" x2="20" y2="6" />
          <line x1="8" y1="12" x2="16" y2="12" />
          <line x1="11" y1="18" x2="13" y2="18" />
        </svg>
      </div>

      {/* 정보 */}
      <div className="flex-1 min-w-0">
        <p className="text-white font-medium truncate">{filter.name}</p>
        <p className="text-xs text-slate-500 mt-1">
          조건 {condCount}개 · {DATE_RULE_LABELS[filter.dateRule as DateRule]} · {filter.markets.join(", ")}
          {filter.includeStockSetId && <span className="text-emerald-500"> · 포함: {setNameMap.get(filter.includeStockSetId) ?? "?"}</span>}
          {filter.excludeStockSetId && <span className="text-red-500"> · 제외: {setNameMap.get(filter.excludeStockSetId) ?? "?"}</span>}
        </p>
        <p className="text-xs text-slate-600 mt-0.5">
          {new Date(filter.updatedAt).toLocaleDateString("ko-KR")} 수정
        </p>
      </div>

      {/* 버튼 */}
      <div className="flex items-center gap-2 flex-shrink-0">
        <button
          onClick={() => onSearch(filter.id)}
          className="px-3 py-1.5 rounded-lg text-xs font-medium text-indigo-300 border border-indigo-500/40 hover:bg-indigo-600/20 transition flex items-center gap-1"
        >
          <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          검색
        </button>
        <button
          onClick={() => onEdit(filter.id)}
          className="px-3 py-1.5 rounded-lg text-xs text-slate-400 border border-white/15 hover:text-white hover:border-white/30 transition"
        >
          수정
        </button>
        {confirmId === filter.id ? (
          <div className="flex items-center gap-1">
            <span className="text-xs text-red-400">삭제?</span>
            <button
              onClick={() => onDelete(filter.id)}
              disabled={deletingId === filter.id}
              className="px-2 py-1 rounded text-xs bg-red-700 hover:bg-red-600 text-white transition disabled:opacity-50"
            >
              {deletingId === filter.id ? "..." : "확인"}
            </button>
            <button
              onClick={onCancelConfirm}
              className="px-2 py-1 rounded text-xs text-slate-400 hover:text-white transition"
            >
              취소
            </button>
          </div>
        ) : (
          <button
            onClick={() => onConfirm(filter.id)}
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
        )}
      </div>
    </div>
  );
}

export default function FilterListClient({ filters: initialFilters, stockSets }: Props) {
  const setNameMap = new Map(stockSets.map((s) => [s.id, s.name]));
  const router = useRouter();

  const [filters, setFilters] = useState<SearchFilter[]>(initialFilters);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIdx = filters.findIndex(f => f.id === active.id);
    const newIdx = filters.findIndex(f => f.id === over.id);
    const next = arrayMove(filters, oldIdx, newIdx);
    setFilters(next);
    await clientFetch("/api/filters/reorder", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ids: next.map(f => f.id) }),
    });
  }

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await clientFetch(`/api/filters/${id}`, { method: "DELETE" });
      setFilters(prev => prev.filter(f => f.id !== id));
    } finally {
      setDeletingId(null);
      setConfirmId(null);
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* 헤더 */}
      <div className="flex items-center justify-between px-6 py-5 border-b border-white/10 flex-shrink-0">
        <div>
          <h1 className="text-xl font-semibold text-white">검색 필터</h1>
          <p className="text-sm text-slate-400 mt-0.5">나만의 조건식으로 종목을 검색하세요</p>
        </div>
        <button
          onClick={() => router.push("/search-filters/new")}
          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white transition"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          새 필터
        </button>
      </div>

      {/* 목록 */}
      <div className="flex-1 overflow-y-auto px-6 py-5">
        {filters.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center py-20">
            <div className="w-14 h-14 rounded-full bg-slate-800 flex items-center justify-center mb-4">
              <svg className="w-7 h-7 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <line x1="4" y1="6" x2="20" y2="6" />
                <line x1="8" y1="12" x2="16" y2="12" />
                <line x1="11" y1="18" x2="13" y2="18" />
              </svg>
            </div>
            <p className="text-slate-400 font-medium">저장된 필터가 없습니다</p>
            <p className="text-slate-500 text-sm mt-1">새 필터를 만들어 종목 검색 조건을 저장하세요</p>
            <button
              onClick={() => router.push("/search-filters/new")}
              className="mt-5 px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white transition"
            >
              첫 필터 만들기
            </button>
          </div>
        ) : (
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
            <SortableContext items={filters.map(f => f.id)} strategy={verticalListSortingStrategy}>
              <div className="grid gap-3 max-w-3xl">
                {filters.map(f => (
                  <FilterCard
                    key={f.id}
                    filter={f}
                    setNameMap={setNameMap}
                    confirmId={confirmId}
                    deletingId={deletingId}
                    onConfirm={setConfirmId}
                    onCancelConfirm={() => setConfirmId(null)}
                    onDelete={handleDelete}
                    onEdit={id => router.push(`/search-filters/${id}/edit`)}
                    onSearch={id => router.push(`/stock-search?filterId=${id}`)}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        )}
      </div>
    </div>
  );
}

