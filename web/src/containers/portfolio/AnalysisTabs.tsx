"use client";

import { useState } from "react";
import ReportTab from "./ReportTab";
import PositionsTab from "./PositionsTab";
import RoundTripsTab from "./RoundTripsTab";
import DividendTab from "./DividendTab";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cx } from "@/utils/cx";

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
    <Tabs value={tab} onValueChange={(v) => setTab(v as Tab)} className="flex flex-col gap-4">
      <TabsList variant="line" className={cx.tab.list}>
        {TABS.map((t) => (
          <TabsTrigger key={t.key} value={t.key} className={cx.tab.trigger}>
            {t.label}
          </TabsTrigger>
        ))}
      </TabsList>
      <TabsContent value="overview" className={cx.tab.content}><ReportTab /></TabsContent>
      <TabsContent value="positions" className={cx.tab.content}><PositionsTab /></TabsContent>
      <TabsContent value="roundtrips" className={cx.tab.content}><RoundTripsTab /></TabsContent>
      <TabsContent value="dividend" className={cx.tab.content}><DividendTab /></TabsContent>
    </Tabs>
  );
}
