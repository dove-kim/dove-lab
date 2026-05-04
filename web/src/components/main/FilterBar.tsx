"use client";

import { FilterState, MarketType } from "@/types/stock";

const MARKETS: { label: string; value: FilterState["market"] }[] = [
  { label: "전체", value: "ALL" },
  { label: "KOSPI", value: "KOSPI" },
  { label: "KOSDAQ", value: "KOSDAQ" },
  { label: "KONEX", value: "KONEX" },
];

interface Props {
  filter: FilterState;
  onChange: (filter: FilterState) => void;
}

export default function FilterBar({ filter, onChange }: Props) {
  return (
    <div className="px-4 py-3 border-b border-white/10 flex flex-col sm:flex-row gap-3">
      <div className="relative flex-1">
        <svg
          className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500"
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
          strokeLinecap="round" strokeLinejoin="round"
        >
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          type="text"
          value={filter.query}
          onChange={(e) => onChange({ ...filter, query: e.target.value })}
          placeholder="종목명 또는 코드 검색"
          className="w-full pl-9 pr-4 py-2 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
        />
      </div>

      <div className="flex gap-1.5">
        {MARKETS.map((m) => (
          <button
            key={m.value}
            onClick={() => onChange({ ...filter, market: m.value })}
            className={`px-3 py-2 rounded-lg text-xs font-medium transition cursor-pointer ${
              filter.market === m.value
                ? "bg-indigo-600 text-white"
                : "bg-white/8 border border-white/15 text-slate-400 hover:text-white hover:border-white/30"
            }`}
          >
            {m.label}
          </button>
        ))}
      </div>
    </div>
  );
}
