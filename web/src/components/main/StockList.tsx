"use client";

import { Stock } from "@/types/stock";

interface Props {
  stocks: Stock[];
  selectedCode: string | null;
  onSelect: (stock: Stock) => void;
}

const STATUS_LABEL: Record<Stock["tradingStatus"], string> = {
  ACTIVE: "",
  SUSPENDED: "거래정지",
  DELISTED: "상장폐지",
};

export default function StockList({ stocks, selectedCode, onSelect }: Props) {
  if (stocks.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-40 text-slate-500 text-sm">
        검색 결과가 없습니다
      </div>
    );
  }

  return (
    <ul className="divide-y divide-white/5">
      {stocks.map((stock) => (
        <li key={`${stock.marketType}-${stock.code}`}>
          <button
            onClick={() => onSelect(stock)}
            className={`w-full flex items-center justify-between px-4 py-3 text-left transition cursor-pointer hover:bg-white/5 ${
              selectedCode === stock.code ? "bg-indigo-600/20 border-l-2 border-indigo-500" : ""
            }`}
          >
            <div className="flex flex-col gap-0.5">
              <span className="text-sm font-medium text-white">{stock.name}</span>
              <span className="text-xs text-slate-500">{stock.code}</span>
            </div>
            <div className="flex flex-col items-end gap-0.5">
              <span className="text-xs text-slate-500">{stock.marketType}</span>
              {stock.tradingStatus !== "ACTIVE" && (
                <span className="text-xs text-red-400">{STATUS_LABEL[stock.tradingStatus]}</span>
              )}
            </div>
          </button>
        </li>
      ))}
    </ul>
  );
}
