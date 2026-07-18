"use client";

import { useMemo } from "react";
import { cx } from "@/utils/cx";
import { type PortfolioRoundTrip, CUR_SYMBOL, natSigned } from "@/types/portfolio";
import { usePortfolioData, won, signed, pnlColor } from "./usePortfolioData";
import { useScope, scopeBase, ScopeSelector } from "./scopeView";
import { usePaged, Pagination } from "./Pagination";
import { useDateRange, DateRangeFilter } from "./DateRange";
import { StatTile } from "./StatTile";

function natPrice(v: number | undefined, cur: string): string {
  if (v == null) return "—";
  return cur === "KRW" ? won(v) : (CUR_SYMBOL[cur] ?? "") + won(v);
}

/**
 * 라운드트립 탭 — 청산 완료 매매의 성과(승률·평균수익·평균보유일)와 사이클 목록을 본다.
 */
export default function RoundTripsTab() {
  const [scope] = useScope();
  const dr = useDateRange();
  const { data, err } = usePortfolioData<PortfolioRoundTrip[]>(`${scopeBase(scope)}/roundtrips`);

  // 청산일(exit) 기준 날짜 필터. 요약 KPI도 필터 결과를 반영한다.
  const closed = useMemo(() => (data ?? []).filter((t) => !t.open && dr.inRange(t.exit)), [data, dr]);

  const summary = useMemo(() => {
    if (closed.length === 0) return null;
    const wins = closed.filter((t) => t.pnlKrw > 0).length;
    return {
      count: closed.length,
      winRate: (wins / closed.length) * 100,
      avgPct: closed.reduce((s, t) => s + t.pnlPct, 0) / closed.length,
      avgDays: closed.reduce((s, t) => s + t.holdingDays, 0) / closed.length,
      totalPnl: closed.reduce((s, t) => s + t.pnlKrw, 0),
    };
  }, [closed]);

  const paged = usePaged(closed, `${scope}|${dr.key}`, 10);

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">청산 성과를 불러오지 못했습니다.</p>;
  if (!data) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-4">
      {summary ? (
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <StatTile label="청산 건수" value={`${summary.count}건`} />
          <StatTile label="승률" value={`${summary.winRate.toFixed(0)}%`} tone={summary.winRate >= 50 ? "text-rose-300" : "text-sky-300"} />
          <StatTile label="평균 수익률" value={`${summary.avgPct >= 0 ? "+" : ""}${(Math.round(summary.avgPct * 10) / 10).toFixed(1)}%`} tone={pnlColor(summary.avgPct)} />
          <StatTile label="평균 보유일" value={`${summary.avgDays.toFixed(0)}일`} />
          <StatTile label="실현손익 합계" value={`${signed(summary.totalPnl)}원`} tone={pnlColor(summary.totalPnl)} />
        </div>
      ) : (
        <p className="text-sm text-slate-500">청산 완료된 매매가 아직 없습니다. (보유 중은 보유종목에서 확인)</p>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <ScopeSelector />
        <DateRangeFilter from={dr.from} to={dr.to} setFrom={dr.setFrom} setTo={dr.setTo} />
      </div>

      <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>계좌</th>
              <th className={cx.table.th}>진입</th>
              <th className={cx.table.th}>청산</th>
              <th className={cx.table.th + " text-right"}>보유일</th>
              <th className={cx.table.th + " text-right"}>평단</th>
              <th className={cx.table.th + " text-right"}>매도가</th>
              <th className={cx.table.th + " text-right"}>실현손익</th>
              <th className={cx.table.th + " text-right"}>수익률</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.map((t) => (
              <tr key={t.id} className={cx.table.tr}>
                <td className={cx.table.td + " text-white font-medium"}>
                  {t.symbol}
                  {t.currency !== "KRW" && <span className="ml-1 text-xs text-slate-500">{t.currency}</span>}
                </td>
                <td className={cx.table.td + " whitespace-nowrap"}>{t.group}</td>
                <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{t.entry.slice(5)}</td>
                <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{t.exit?.slice(5)}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{t.holdingDays}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{natPrice(t.avgNat, t.currency)}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{natPrice(t.exitNat, t.currency)}</td>
                <td className={cx.table.td + " text-right tabular-nums " + pnlColor(t.pnlKrw)}>
                  {natSigned(t.pnlNat, t.currency)}
                </td>
                <td className={cx.table.td + " text-right tabular-nums " + pnlColor(t.pnlPct)}>
                  {`${t.pnlPct > 0 ? "+" : ""}${t.pnlPct}%`}
                </td>
              </tr>
            ))}
            {closed.length === 0 && (
              <tr>
                <td colSpan={9} className="px-4 py-8 text-center text-sm text-slate-500">
                  청산 완료된 매매가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={paged.page} pageCount={paged.pageCount} from={paged.from} to={paged.to} total={paged.total} onPage={paged.setPage} />

      <p className="text-xs text-slate-600">
        개별 청산 손익·수익률은 원통화 기준입니다. 상단 &lsquo;실현손익 합계&rsquo;는 통화가 섞여 현재 환율로 원화 환산한 값입니다.
      </p>
    </div>
  );
}
