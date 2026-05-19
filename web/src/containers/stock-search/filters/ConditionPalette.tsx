"use client";

import { ConditionType } from "@/types/filter";

interface PaletteItem {
  type: ConditionType;
  label: string;
  description: string;
  icon: string;
}

interface PaletteSection {
  title: string;
  items: PaletteItem[];
}

const PALETTE_SECTIONS: PaletteSection[] = [
  {
    title: "기술적 지표",
    items: [
      { type: "INDICATOR_VALUE", label: "지표 값 비교", description: "RSI > 70, SMA20 < 50000 등 지표와 숫자 비교", icon: "📊" },
      { type: "INDICATOR_RANGE", label: "지표 범위", description: "30 ≤ RSI ≤ 70 처럼 지표 값의 범위 지정", icon: "↔️" },
      { type: "INDICATOR_CROSS", label: "지표 간 비교", description: "SMA5 > SMA20 처럼 지표끼리 비교", icon: "⚡" },
    ],
  },
  {
    title: "가격",
    items: [
      { type: "PRICE_VALUE", label: "가격 비교", description: "종가 > 10,000원 처럼 주가와 숫자 비교", icon: "💰" },
      { type: "PRICE_RANGE", label: "가격 범위", description: "5,000 ≤ 종가 ≤ 50,000 처럼 가격 범위 지정", icon: "📏" },
      { type: "PRICE_VS_INDICATOR", label: "주가 vs 지표", description: "종가 > SMA_20, 종가 < 볼린저하단(20) 처럼 주가와 지표 비교", icon: "📐" },
    ],
  },
  {
    title: "거래량",
    items: [
      { type: "VOLUME_VALUE", label: "거래량 비교", description: "거래량 > 1,000,000 처럼 거래량 조건 지정", icon: "📈" },
      { type: "VOLUME_RANGE", label: "거래량 범위", description: "거래량 범위를 최솟값~최댓값으로 지정", icon: "📉" },
    ],
  },
  {
    title: "시장",
    items: [
      { type: "MARKET_FILTER", label: "시장 필터", description: "KOSPI, KOSDAQ, KONEX 시장 선택", icon: "🏢" },
    ],
  },
];

interface Props {
  selectedGroupId: string | null;
  rootId: string | null;
  onAdd: (type: ConditionType) => void;
}

export default function ConditionPalette({ selectedGroupId, rootId, onAdd }: Props) {
  const targetLabel = !selectedGroupId
    ? null
    : selectedGroupId === rootId
      ? "루트"
      : "서브 그룹";

  return (
    <div className="flex flex-col h-full">
      <div className="px-4 py-3 border-b border-white/10">
        <p className="text-sm font-medium text-white">조건 추가</p>
        <p className="text-xs text-slate-400 mt-0.5">
          {targetLabel
            ? <span>추가 대상: <span className={`font-semibold ${targetLabel === "루트" ? "text-slate-300" : "text-indigo-400"}`}>{targetLabel}</span></span>
            : "왼쪽에서 그룹을 선택하세요"}
        </p>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {PALETTE_SECTIONS.map((section) => (
          <div key={section.title}>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-1 mb-2">
              {section.title}
            </p>
            <div className="space-y-1">
              {section.items.map((item) => (
                <button
                  key={item.type}
                  onClick={() => selectedGroupId && onAdd(item.type)}
                  disabled={!selectedGroupId}
                  className={`w-full text-left px-3 py-2.5 rounded-lg transition group ${
                    selectedGroupId
                      ? "hover:bg-indigo-600/20 hover:border-indigo-500/40 border border-transparent cursor-pointer"
                      : "opacity-40 cursor-not-allowed"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span className="text-base leading-none">{item.icon}</span>
                    <span className="text-sm text-white font-medium">{item.label}</span>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">{item.description}</p>
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

