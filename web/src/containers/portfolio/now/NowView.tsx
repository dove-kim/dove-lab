"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  type PortfolioAccount,
  type PortfolioFxConversion,
  type PortfolioPosition,
  type PortfolioRoundTrip,
  type PortfolioSummary,
  type PortfolioTx,
} from "@/types/portfolio";
import { usePortfolioData, won, fxRateOf } from "../usePortfolioData";
import { useScope, scopeBase } from "../scopeView";
import PortfolioHeader from "../PortfolioHeader";
import { StatTile } from "../StatTile";
import AccountCards, { type AccountValue } from "./AccountCards";
import AllocationDonuts from "./AllocationDonuts";
import HoldingsTable from "./HoldingsTable";
import { cashByAccount } from "./accountCash";

/**
 * 현황 화면 — 지금 내 자산이 어떤 상태인지 본다. 총자산·현금·계좌·보유 종목·자산 배분.
 */
export default function NowView() {
  const [scope] = useScope();
  const router = useRouter();
  const { data: summary, err } = usePortfolioData<PortfolioSummary>(`${scopeBase(scope)}/summary`);
  const { data: positions } = usePortfolioData<PortfolioPosition[]>(`${scopeBase(scope)}/positions`);
  const { data: roundTrips } = usePortfolioData<PortfolioRoundTrip[]>(`${scopeBase(scope)}/roundtrips`);
  const [accounts, setAccounts] = useState<PortfolioAccount[]>([]);
  const [txns, setTxns] = useState<PortfolioTx[]>([]);
  const [convs, setConvs] = useState<PortfolioFxConversion[]>([]);

  const loadOwn = useCallback(() => {
    fetch("/api/portfolio/accounts", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioAccount[]) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/transactions", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioTx[]) => setTxns(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/fx-conversions", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioFxConversion[]) => setConvs(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadOwn();
  }, [loadOwn]);

  // 계좌 변경은 요약·배분에도 반영되므로 라우터 캐시까지 무효화한다.
  function reload() {
    loadOwn();
    router.refresh();
  }

  const visibleAccounts = useMemo(
    () => (scope === "own" ? accounts : accounts.filter((a) => String(a.id) === scope)),
    [accounts, scope]
  );

  const accountValues = useMemo(() => {
    const cash = cashByAccount(txns, convs);
    const list = positions ?? [];
    const fxByCur = new Map<string, number>();
    for (const p of list) {
      if (p.currency === "KRW") continue;
      const fx = fxRateOf(p);
      if (fx != null && !fxByCur.has(p.currency)) fxByCur.set(p.currency, fx);
    }
    const fxOf = (cur: string) => (cur === "KRW" ? 1 : fxByCur.get(cur) ?? 1);

    const m = new Map<number, AccountValue>();
    for (const a of visibleAccounts) {
      const nativeByCur: Record<string, number> = { ...(cash.get(a.id) ?? {}) };
      let krw = 0;
      for (const [cur, bal] of Object.entries(nativeByCur)) krw += bal * fxOf(cur);
      for (const p of list) {
        if (p.account !== a.name) continue;
        krw += p.evalKrw;
      }
      m.set(a.id, { nativeByCur, krw });
    }
    return m;
  }, [visibleAccounts, txns, convs, positions]);

  if (err) return <p className="py-8 text-center text-sm text-rose-300">현황을 불러오지 못했습니다.</p>;
  if (!summary) return <p className="py-8 text-center text-sm text-slate-500">불러오는 중…</p>;

  return (
    <>
      <PortfolioHeader title="현황" />
      <div className="flex flex-col gap-6">
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatTile label="총자산" value={won(summary.totalKrw) + "원"} sub="현금 + 평가액" />
          <StatTile label="현금" value={won(summary.cashKrw) + "원"} />
          <StatTile label="순납입 (입금−출금)" value={won(summary.netContribKrw) + "원"} />
          <StatTile label="보유 종목" value={`${(positions ?? []).length}개`} />
        </div>

        {(scope === "own" || visibleAccounts.length > 0) && (
          <AccountCards accounts={visibleAccounts} values={accountValues} onChanged={reload} />
        )}

        <HoldingsTable positions={positions ?? []} roundTrips={roundTrips ?? []} />

        <AllocationDonuts positions={positions ?? []} summary={summary} />

        <p className="text-xs text-slate-600">
          얼마나 벌었는지는 <span className="text-slate-400">성과</span>에서, 거래 기록은{" "}
          <span className="text-slate-400">기록</span>에서 본다.
        </p>
      </div>
    </>
  );
}
