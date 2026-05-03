"use client";

import { Stock } from "@/types/stock";

interface Props {
  stock: Stock | null;
  onBack?: () => void;
}

export default function StockChart({ stock, onBack }: Props) {
  if (!stock) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-slate-600 gap-2 select-none">
        <svg className="w-12 h-12 opacity-30" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
        <span className="text-sm">종목을 선택하면 차트가 표시됩니다</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center gap-3 px-5 py-4 border-b border-white/10">
        {onBack && (
          <button
            onClick={onBack}
            className="md:hidden text-slate-400 hover:text-white transition cursor-pointer"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
        )}
        <div>
          <h2 className="text-white font-semibold">{stock.name}</h2>
          <p className="text-xs text-slate-500">{stock.code} · {stock.marketType}</p>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center text-slate-600 text-sm">
        차트 영역 (추후 구현)
      </div>
    </div>
  );
}
