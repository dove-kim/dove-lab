"use client";

import { useMemo, useState } from "react";
import { cx } from "@/utils/cx";
import { type PortfolioPosition, natMoney, natSigned } from "@/types/portfolio";
import { usePortfolioData, won, signed, pnlColor } from "./usePortfolioData";
import { useScope, scopeBase, ScopeSelector } from "./scopeView";
import { usePaged, Pagination } from "@/components/Pagination";

type Sort = "eval" | "pnl" | "weight";

/**
 * 보유 탭 — 계좌·종목으로 걸러 보유 포지션·평단·평가손익을 본다.
 */
export default function PositionsTab() {
  const [scope] = useScope();
  const { data, err } = usePortfolioData<PortfolioPosition[]>(`${scopeBase(scope)}/positions`);
  const [sym, setSym] = useState("");
  const [acct, setAcct] = useState("");
  const [sort, setSort] = useState<Sort>("eval");

  const accounts = useMemo(() => (data ? Array.from(new Set(data.map((p) => p.account))) : []), [data]);
  const symbols = useMemo(() => (data ? data.map((p) => p.symbol) : []), [data]);

  const q = sym.trim().toLowerCase();
  const rows = useMemo(() => {
    const filtered = (data ?? []).filter(
      (p) => (!q || p.symbol.toLowerCase().includes(q)) && (!acct || p.account === acct)
    );
    const key = sort === "eval" ? "evalKrw" : sort === "pnl" ? "pnlKrw" : "weightPct";
    return [...filtered].sort((a, b) => (b[key] as number) - (a[key] as number));
  }, [data, q, acct, sort]);

  const totalEval = useMemo(() => rows.reduce((s, p) => s + p.evalKrw, 0), [rows]);
  const totalPnl = useMemo(() => rows.reduce((s, p) => s + p.pnlKrw, 0), [rows]);
  const paged = usePaged(rows, `${q}|${acct}|${sort}|${scope}`, 10);

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">보유 내역을 불러오지 못했습니다.</p>;
  if (!data) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <ScopeSelector />
        <input
          list="portfolio-pos-symbols"
          value={sym}
          onChange={(e) => setSym(e.target.value)}
          placeholder="종목 검색"
          className={cx.input + " sm:w-48"}
        />
        <datalist id="portfolio-pos-symbols">
          {symbols.map((s) => (
            <option key={s} value={s} />
          ))}
        </datalist>
        <select value={acct} onChange={(e) => setAcct(e.target.value)} className={cx.select}>
          <option value="">전체 계좌</option>
          {accounts.map((a) => (
            <option key={a} value={a}>
              {a}
            </option>
          ))}
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value as Sort)} className={cx.select}>
          <option value="eval">평가액 순</option>
          <option value="pnl">손익 순</option>
          <option value="weight">비중 순</option>
        </select>
        <div className="ml-auto text-sm text-slate-400">
          평가액 <span className="text-white tabular-nums">{won(totalEval)}</span>원
          <span className={"ml-2 tabular-nums " + pnlColor(totalPnl)}>{signed(totalPnl)}</span>
        </div>
      </div>

      <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>계좌</th>
              <th className={cx.table.th}>태그</th>
              <th className={cx.table.th + " text-right"}>수량</th>
              <th className={cx.table.th + " text-right"}>평단</th>
              <th className={cx.table.th + " text-right"}>현재가</th>
              <th className={cx.table.th + " text-right"}>평가액</th>
              <th className={cx.table.th + " text-right"}>평가손익</th>
              <th className={cx.table.th + " text-right"}>수익률</th>
              <th className={cx.table.th + " text-right"}>비중</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.map((p) => (
              <tr key={p.symbol + p.account} className={cx.table.tr}>
                <td className={cx.table.td + " text-white font-medium"}>
                  {p.symbol}
                  {p.currency !== "KRW" && <span className="ml-1 text-xs text-slate-500">{p.currency}</span>}
                </td>
                <td className={cx.table.td + " whitespace-nowrap"}>{p.account}</td>
                <td className={cx.table.td}>
                  {p.tag ? (
                    <span className="inline-block rounded-full bg-white/5 px-2 py-0.5 text-xs text-slate-300">{p.tag}</span>
                  ) : (
                    "—"
                  )}
                </td>
                <td className={cx.table.td + " text-right tabular-nums"}>{p.quantity}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{natMoney(p.avgPriceNat, p.currency)}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{natMoney(p.curPriceNat, p.currency)}</td>
                <td className={cx.table.td + " text-right tabular-nums text-white"}>
                  {natMoney(p.curPriceNat * p.quantity, p.currency)}
                </td>
                <td className={cx.table.td + " text-right tabular-nums " + pnlColor(p.pnlKrw)}>
                  {natSigned((p.curPriceNat - p.avgPriceNat) * p.quantity, p.currency)}
                </td>
                <td className={cx.table.td + " text-right tabular-nums " + pnlColor(p.pnlPct)}>
                  {(p.pnlPct >= 0 ? "+" : "") + p.pnlPct}%
                </td>
                <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>{p.weightPct}%</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-sm text-slate-500">
                  조건에 맞는 보유 종목이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={paged.page} pageCount={paged.pageCount} from={paged.from} to={paged.to} total={paged.total} onPage={paged.setPage} />
    </div>
  );
}
