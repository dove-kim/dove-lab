"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { cx } from "@/utils/cx";
import Select from "@/components/Select";
import { StockSet } from "@/types/filter";
import { Stock } from "@/types/stock";

interface Props {
  initial?: StockSet;
  stocks: Stock[];
}

export default function StockSetEditorClient({ initial, stocks }: Props) {
  const router = useRouter();
  const [name, setName] = useState(initial?.name ?? "");
  const [codes, setCodes] = useState<string[]>(initial?.codes ?? []);
  const [query, setQuery] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hideAdded, setHideAdded] = useState(false);
  const [highlightNew, setHighlightNew] = useState(false);

  type SortKey = "listing_desc" | "listing_asc" | "code_asc" | "code_desc" | "name_asc" | "name_desc";
  const [sortKey, setSortKey] = useState<SortKey>("listing_desc");

  const oneMonthAgo = (() => {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    return d.toISOString().slice(0, 10);
  })();

  const codeSet = new Set(codes);
  const stockMap = new Map(stocks.map((s) => [s.code, s]));

  const SORT_OPTIONS: { value: SortKey; label: string }[] = [
    { value: "listing_desc", label: "최신순" },
    { value: "listing_asc",  label: "오래된순" },
    { value: "code_asc",     label: "코드 ↑" },
    { value: "code_desc",    label: "코드 ↓" },
    { value: "name_asc",     label: "이름 ↑" },
    { value: "name_desc",    label: "이름 ↓" },
  ];

  function sortStocks(list: Stock[]): Stock[] {
    return [...list].sort((a, b) => {
      const ld = (s: Stock) => s.listingDate ?? "";
      switch (sortKey) {
        case "listing_desc": return ld(b).localeCompare(ld(a));
        case "listing_asc":  return ld(a).localeCompare(ld(b));
        case "code_asc":     return a.code.localeCompare(b.code);
        case "code_desc":    return b.code.localeCompare(a.code);
        case "name_asc":     return a.name.localeCompare(b.name, "ko");
        case "name_desc":    return b.name.localeCompare(a.name, "ko");
      }
    });
  }

  const filtered = sortStocks(
    stocks.filter((s) => {
      if (hideAdded && codeSet.has(s.code)) return false;
      if (!query.trim()) return true;
      const q = query.trim().toUpperCase();
      return s.code.includes(q) || s.name.toUpperCase().includes(q);
    })
  );

  function addStock(stock: Stock) {
    if (codeSet.has(stock.code)) return;
    setCodes((prev) => [...prev, stock.code]);
  }

  function removeCode(code: string) {
    setCodes((prev) => prev.filter((c) => c !== code));
  }

  async function handleSave() {
    if (!name.trim()) { setError("이름을 입력하세요"); return; }
    setSaving(true);
    setError(null);

    const url = initial ? `/api/stock-sets/${initial.id}` : "/api/stock-sets";
    const method = initial ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim(), codes }),
      });

      if (res.status === 409) { setError("같은 이름의 종목 필터가 이미 존재합니다"); return; }
      if (!res.ok) { setError("저장에 실패했습니다."); return; }
      router.push("/stock-sets");
      router.refresh();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* 헤더 */}
      <div className="flex items-center gap-4 px-6 py-4 border-b border-white/10 flex-shrink-0">
        <button onClick={() => router.push("/stock-sets")} className="text-slate-400 hover:text-white transition">
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="m15 18-6-6 6-6" />
          </svg>
        </button>
        <h1 className="text-lg font-semibold text-white">
          {initial ? "종목 필터 수정" : "새 종목 필터"}
        </h1>
      </div>

      {/* 필터 이름 */}
      <div className="px-6 py-4 border-b border-white/10 flex-shrink-0">
        <label className="text-xs text-slate-400 mb-1 block">필터 이름</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="예: 삼성전자 계열"
          maxLength={100}
          className={`max-w-sm ${cx.input}`}
        />
        {error && <p className="text-xs text-red-400 mt-1">{error}</p>}
      </div>

      {/* 본문 영역 */}
      <div className="flex flex-1 min-h-0 divide-x divide-white/10">
        {/* 좌측: 전체 종목 */}
        <div className="flex flex-col w-1/2 min-h-0">
          <div className="px-4 py-3 border-b border-white/10 flex-shrink-0 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <p className="text-xs text-slate-400 flex-shrink-0">전체 종목 ({filtered.length}개)</p>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setHideAdded((v) => !v)}
                  className={`px-2 py-1 rounded text-xs transition ${hideAdded ? "bg-indigo-600 text-white" : "text-slate-500 border border-white/10 hover:text-slate-300"}`}
                >
                  추가된 것 숨기기
                </button>
                <button
                  type="button"
                  onClick={() => setHighlightNew((v) => !v)}
                  className={`px-2 py-1 rounded text-xs transition ${highlightNew ? "bg-amber-600 text-white" : "text-slate-500 border border-white/10 hover:text-slate-300"}`}
                >
                  최근 1개월
                </button>
                <Select
                  value={sortKey}
                  items={SORT_OPTIONS}
                  onChange={(v) => setSortKey(v as SortKey)}
                />
              </div>
            </div>
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="종목명 또는 종목코드 검색"
              className={cx.input}
            />
          </div>
          <div className="flex-1 overflow-y-auto divide-y divide-white/5">
            {filtered.map((stock) => {
              const added = codeSet.has(stock.code);
              const isNew = highlightNew && stock.listingDate >= oneMonthAgo;
              return (
                <button
                  key={stock.code}
                  type="button"
                  onClick={() => addStock(stock)}
                  disabled={added}
                  className={`w-full flex items-center justify-between px-4 py-2.5 text-left group transition ${
                    added
                      ? "opacity-30 cursor-default"
                      : isNew
                        ? "bg-amber-500/5 hover:bg-amber-500/15 cursor-pointer"
                        : "hover:bg-indigo-600/15 cursor-pointer"
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-xs font-mono text-slate-400 w-16 flex-shrink-0">{stock.code}</span>
                    <span className={`text-sm truncate ${isNew ? "text-amber-200" : "text-white"}`}>{stock.name}</span>
                    {isNew && <span className="text-xs text-amber-500 flex-shrink-0">NEW</span>}
                  </div>
                  <div className="relative w-14 flex-shrink-0 ml-2 flex items-center justify-end">
                    <span className="text-xs text-slate-600 transition group-hover:opacity-0">{stock.marketType}</span>
                    <svg className="w-3.5 h-3.5 text-indigo-400 absolute opacity-0 group-hover:opacity-100 transition" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M12 5v14M5 12h14" />
                    </svg>
                  </div>
                </button>
              );
            })}
            {filtered.length === 0 && (
              <div className="px-4 py-8 text-center text-slate-600 text-sm">검색 결과가 없습니다</div>
            )}
          </div>
        </div>

        {/* 우측: 선택된 종목 */}
        <div className="flex flex-col w-1/2 min-h-0">
          <div className="px-4 py-3 border-b border-white/10 flex-shrink-0 flex items-center justify-between">
            <p className="text-xs text-slate-400">선택된 종목 ({codes.length}개)</p>
            {codes.length > 0 && (
              <button
                type="button"
                onClick={() => setCodes([])}
                className="text-xs text-slate-500 hover:text-red-400 transition"
              >
                전체 제거
              </button>
            )}
          </div>
          <div className="flex-1 overflow-y-auto divide-y divide-white/5">
            {codes.length === 0 ? (
              <div className="px-4 py-8 text-center text-slate-600 text-sm">
                좌측 목록에서 종목을 선택하세요
              </div>
            ) : (
              codes.map((code) => {
                const stock = stockMap.get(code);
                return (
                  <button
                    key={code}
                    type="button"
                    onClick={() => removeCode(code)}
                    className="w-full flex items-center justify-between px-4 py-2.5 text-left group hover:bg-red-500/10 transition"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="text-xs font-mono text-slate-400 w-16 flex-shrink-0 group-hover:text-red-400 transition">{code}</span>
                      <span className="text-sm text-white truncate group-hover:text-red-200 transition">
                        {stock?.name ?? <span className="italic">이름 없음</span>}
                      </span>
                      {stock && (
                        <span className="text-xs text-slate-600 flex-shrink-0 group-hover:text-red-500 transition">{stock.marketType}</span>
                      )}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* 하단 버튼 */}
      <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-white/10 flex-shrink-0">
        <button
          onClick={() => router.push("/stock-sets")}
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

