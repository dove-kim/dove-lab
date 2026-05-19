"use client";

import { useState, useCallback } from "react";
import { cx } from "@/utils/cx";
import { useRouter } from "next/navigation";
import {
  GroupNode,
  ConditionType,
  SearchFilter,
  MarketTypeFilter,
  StockSetSummary,
} from "@/types/filter";
import { createEmptyRoot } from "@/utils/filter";
import ExpressionTree from "./ExpressionTree";
import ConditionPalette from "./ConditionPalette";
import Select from "@/components/Select";

const ALL_MARKETS: MarketTypeFilter[] = ["KOSPI", "KOSDAQ", "KONEX"];

interface Props {
  initial?: SearchFilter;
  stockSets: StockSetSummary[];
}

export default function FilterEditorClient({ initial, stockSets }: Props) {
  const router = useRouter();

  const [initialData] = useState(() => {
    const r: GroupNode = (() => {
      if (!initial?.expression) return createEmptyRoot();
      try { return JSON.parse(initial.expression) as GroupNode; }
      catch { return createEmptyRoot(); }
    })();
    return { root: r, id: r.id };
  });

  const [name, setName] = useState(initial?.name ?? "");
  const [selectedMarkets, setSelectedMarkets] = useState<Set<MarketTypeFilter>>(
    new Set((initial?.markets ?? ALL_MARKETS) as MarketTypeFilter[])
  );
  const [includeStockSetId, setIncludeStockSetId] = useState<number | null>(
    initial?.includeStockSetId ?? null
  );
  const [excludeStockSetId, setExcludeStockSetId] = useState<number | null>(
    initial?.excludeStockSetId ?? null
  );
  const [root, setRoot] = useState<GroupNode>(initialData.root);
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(initialData.id);
  const [pendingAddType, setPendingAddType] = useState<ConditionType | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handlePaletteAdd = useCallback((type: ConditionType) => {
    setPendingAddType(type);
  }, []);

  function toggleMarket(m: MarketTypeFilter) {
    setSelectedMarkets((prev) => {
      const next = new Set(prev);
      if (next.has(m)) {
        if (next.size === 1) return prev;
        next.delete(m);
      } else {
        next.add(m);
      }
      return next;
    });
  }

  async function handleSave() {
    if (!name.trim()) { setError("이름을 입력하세요"); return; }
    if (selectedMarkets.size === 0) { setError("시장을 하나 이상 선택하세요"); return; }
    setSaving(true);
    setError(null);

    const body = {
      name: name.trim(),
      dateRule: "LATEST",
      markets: Array.from(selectedMarkets),
      expression: JSON.stringify(root),
      includeStockSetId,
      excludeStockSetId,
    };
    const url = initial ? `/api/filters/${initial.id}` : "/api/filters";
    const method = initial ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (res.status === 409) { setError("같은 이름의 필터가 이미 존재합니다"); return; }
      if (!res.ok) { setError("저장에 실패했습니다."); return; }
      router.push("/search-filters");
      router.refresh();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* 헤더 */}
      <div className="flex items-center gap-4 px-6 py-4 border-b border-white/10 flex-shrink-0">
        <button onClick={() => router.push("/search-filters")} className="text-slate-400 hover:text-white transition">
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="m15 18-6-6 6-6" />
          </svg>
        </button>
        <h1 className="text-lg font-semibold text-white">
          {initial ? "필터 수정" : "새 필터 만들기"}
        </h1>
      </div>

      {/* 기본 설정 */}
      <div className="px-6 py-4 border-b border-white/10 flex-shrink-0">
        <div className="flex flex-wrap gap-6">
          {/* 이름 */}
          <div className="min-w-48 flex-1">
            <label className="text-xs text-slate-400 mb-1 block">필터 이름</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: RSI 과매도 + 이동평균선"
              maxLength={100}
              className={cx.input}
            />
            {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
          </div>

          {/* 시장 */}
          <div>
            <label className="text-xs text-slate-400 mb-1 block">대상 시장</label>
            <div className="flex gap-2">
              {ALL_MARKETS.map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => toggleMarket(m)}
                  className={selectedMarkets.has(m) ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* 종목 세트 포함/제외 */}
        {stockSets.length > 0 && (
          <div className="flex flex-wrap gap-6 mt-4 pt-4 border-t border-white/5">
            <StockSetPicker
              label="포함 종목 필터"
              value={includeStockSetId}
              onChange={setIncludeStockSetId}
              stockSets={stockSets}
              helpText="선택된 세트의 종목만 결과에 포함"
            />
            <StockSetPicker
              label="제외 종목 필터"
              value={excludeStockSetId}
              onChange={setExcludeStockSetId}
              stockSets={stockSets}
              helpText="선택된 세트의 종목을 결과에서 제외 (포함보다 제외 우선)"
            />
          </div>
        )}
        {stockSets.length === 0 && (
          <p className="text-xs text-slate-600 mt-3">
            종목 필터를 만들면 특정 종목만 포함하거나 제외할 수 있어요{" "}
            <a href="/stock-sets/new" className="text-indigo-400 hover:text-indigo-300">새 종목 필터 만들기</a>
          </p>
        )}
      </div>

      {/* 본문 2-패널 */}
      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 overflow-y-auto p-6">
          <div className="max-w-2xl">
            <p className="text-xs text-slate-500 mb-3">
              아래 영역에서 조건을 추가하세요. 그룹을 선택하면 해당 그룹 아래에 조건을 추가합니다.
            </p>
            <ExpressionTree
              root={root}
              onChange={setRoot}
              selectedGroupId={selectedGroupId}
              onSelectGroup={setSelectedGroupId}
              pendingAddType={pendingAddType}
              onPendingAddConsumed={() => setPendingAddType(null)}
            />
          </div>
        </div>

        <div className="w-64 flex-shrink-0 border-l border-white/10 overflow-hidden">
          <ConditionPalette selectedGroupId={selectedGroupId} rootId={initialData.id} onAdd={handlePaletteAdd} />
        </div>
      </div>

      {/* 하단 버튼 */}
      <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/10 flex-shrink-0">
        <button
          onClick={() => router.push("/search-filters")}
          className={cx.btnSecondary}
        >
          취소
        </button>
        <button
          onClick={handleSave}
          disabled={saving}
          className={cx.btnPrimary}
        >
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );
}

function StockSetPicker({
  label,
  value,
  onChange,
  stockSets,
  helpText,
}: {
  label: string;
  value: number | null;
  onChange: (v: number | null) => void;
  stockSets: StockSetSummary[];
  helpText: string;
}) {
  return (
    <div>
      <label className="text-xs text-slate-400 mb-1 block">{label}</label>
      <Select
        value={value?.toString() ?? ""}
        items={[
          { value: "", label: "없음" },
          ...stockSets.map(s => ({ value: s.id.toString(), label: `${s.name} (${s.codeCount}종목)` })),
        ]}
        onChange={(v) => onChange(v ? parseInt(v) : null)}
        className="min-w-44"
      />
      <p className="text-xs text-slate-500 mt-1">{helpText}</p>
    </div>
  );
}

