"use client";

import { useMemo, useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cx } from "@/utils/cx";
import { type PortfolioRoundTrip, type PortfolioSummary, type PortfolioTx } from "@/types/portfolio";
import { usePortfolioData } from "../usePortfolioData";
import { useScope, scopeBase } from "../scopeView";
import PortfolioHeader from "../PortfolioHeader";
import OverallStats from "./OverallStats";
import YearlyRealizedChart from "./YearlyRealizedChart";
import SymbolPerformanceTable from "./SymbolPerformanceTable";
import DividendPanel from "./DividendPanel";
import { type PeriodKey, PERIOD_KEYS, resolvePeriod } from "./period";
import { foldSymbolPerformance, realizedByYear } from "./symbolPerformance";

type Panel = "trades" | "dividend";

/**
 * 성과 화면 — 얼마나 벌었는지 본다. 기간을 고르면 실현 손익과 종목별 집계가 그 기간으로 잘린다.
 */
export default function PerformanceView() {
  const [scope] = useScope();
  const [panel, setPanel] = useState<Panel>("trades");
  const [periodKey, setPeriodKey] = useState<PeriodKey>("all");
  const { data: summary, err } = usePortfolioData<PortfolioSummary>(`${scopeBase(scope)}/summary`);
  const { data: roundTrips } = usePortfolioData<PortfolioRoundTrip[]>(`${scopeBase(scope)}/roundtrips`);
  const { data: txns } = usePortfolioData<PortfolioTx[]>(`${scopeBase(scope)}/transactions`);

  const period = useMemo(() => resolvePeriod(periodKey), [periodKey]);
  const asOf = useMemo(() => new Date().toISOString().slice(0, 10), []);

  const symbolRows = useMemo(
    () => foldSymbolPerformance(roundTrips ?? [], txns ?? [], period, asOf),
    [roundTrips, txns, period, asOf]
  );
  const yearly = useMemo(() => realizedByYear(roundTrips ?? []), [roundTrips]);

  if (err) return <p className="py-8 text-center text-sm text-rose-300">성과를 불러오지 못했습니다.</p>;
  if (!summary) return <p className="py-8 text-center text-sm text-slate-500">불러오는 중…</p>;

  return (
    <>
      <PortfolioHeader title="성과" />
      <div className="flex flex-col gap-5">
        <div className="flex flex-wrap gap-2">
          {PERIOD_KEYS.map((k) => (
            <button
              key={k}
              onClick={() => setPeriodKey(k)}
              className={periodKey === k ? cx.btnToggleOn : cx.btnToggleOff}
            >
              {resolvePeriod(k).label}
            </button>
          ))}
        </div>

        <OverallStats summary={summary} roundTrips={roundTrips ?? []} period={period} />

        <Tabs value={panel} onValueChange={(v) => setPanel(v as Panel)} className="flex flex-col gap-4">
          <TabsList variant="line" className={cx.tab.list}>
            <TabsTrigger value="trades" className={cx.tab.trigger}>
              종목별
            </TabsTrigger>
            <TabsTrigger value="dividend" className={cx.tab.trigger}>
              배당
            </TabsTrigger>
          </TabsList>
          <TabsContent value="trades" className={cx.tab.content}>
            <div className="flex flex-col gap-5">
              <YearlyRealizedChart rows={yearly} />
              <SymbolPerformanceTable rows={symbolRows} />
            </div>
          </TabsContent>
          <TabsContent value="dividend" className={cx.tab.content}>
            <DividendPanel />
          </TabsContent>
        </Tabs>
      </div>
    </>
  );
}
