"use client";

import { useState } from "react";
import { Stock } from "@/types/stock";
import StockList from "./StockList";
import StockChart from "./StockChart";

interface Props {
  stocks: Stock[];
}

export default function MainLayout({ stocks }: Props) {
  const [selectedStock, setSelectedStock] = useState<Stock | null>(null);

  return (
    <div className="flex flex-1 overflow-hidden min-w-0">
      {/* 종목 리스트 */}
      <div className={`w-full sm:w-56 lg:w-80 flex-shrink-0 overflow-y-auto border-r border-white/10 ${
        selectedStock ? "hidden sm:block" : "block"
      }`}>
        <StockList
          stocks={stocks}
          selectedCode={selectedStock?.code ?? null}
          onSelect={setSelectedStock}
        />
      </div>

      {/* 차트 */}
      <div className={`flex-1 overflow-hidden ${
        selectedStock ? "block" : "hidden sm:block"
      }`}>
        <StockChart stock={selectedStock} onBack={() => setSelectedStock(null)} />
      </div>
    </div>
  );
}
