"use client";

import { useState } from "react";
import ReportTab from "./ReportTab";
import PositionsTab from "./PositionsTab";
import RoundTripsTab from "./RoundTripsTab";
import DividendTab from "./DividendTab";

type Tab = "overview" | "positions" | "roundtrips" | "dividend";

const TABS: { key: Tab; label: string }[] = [
  { key: "overview", label: "개요" },
  { key: "positions", label: "보유종목" },
  { key: "roundtrips", label: "청산 성과" },
  { key: "dividend", label: "배당" },
];

/**
 * 자산 분석 화면 — 개요·보유종목·청산 성과·배당을 상단 탭으로 묶는다.
 */
export default function AnalysisTabs() {
  const [tab, setTab] = useState<Tab>("overview");
  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-1 border-b border-white/10">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={
              "px-4 py-3 text-sm font-medium border-b-2 -mb-px transition cursor-pointer " +
              (tab === t.key
                ? "border-indigo-400 text-indigo-300"
                : "border-transparent text-slate-400 hover:text-white")
            }
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === "overview" && <ReportTab />}
      {tab === "positions" && <PositionsTab />}
      {tab === "roundtrips" && <RoundTripsTab />}
      {tab === "dividend" && <DividendTab />}
    </div>
  );
}
