"use client";

import { useMemo, useState } from "react";
import Select from "@/components/Select";
import Panel from "@/components/Panel";
import { cx } from "@/utils/cx";
import { type PortfolioPosition, type PortfolioRoundTrip, natMoney, natSigned } from "@/types/portfolio";
import { won, signed, pnlColor } from "../usePortfolioData";
import { usePaged, Pagination } from "@/components/Pagination";

type Sort = "eval" | "pnl" | "weight" | "held";

/** 미청산 사이클의 진입일·보유일. */
interface HoldingSince {
  entry: string;
  days: number;
}

/**
 * 보유 종목 표 — 계좌·종목으로 걸러 수량·평단·평가손익과 보유 기간을 본다.
 *
 * @param positions  보유 포지션 목록
 * @param roundTrips 라운드트립 목록(미청산 사이클에서 보유 기간을 얻는다)
 */
export default function HoldingsTable({
  positions,
  roundTrips,
}: {
  positions: PortfolioPosition[];
  roundTrips: PortfolioRoundTrip[];
}) {
  const [sym, setSym] = useState("");
  const [acct, setAcct] = useState("");
  const [sort, setSort] = useState<Sort>("eval");

  const accounts = useMemo(() => Array.from(new Set(positions.map((p) => p.account))), [positions]);
  const q = sym.trim().toLowerCase();

  // 보유 중인 사이클만 (계좌, 종목) 키로 모은다. 전량 매도 후 재매수면 마지막 진입일이 들어온다.
  const heldSince = useMemo(() => {
    const m = new Map<string, HoldingSince>();
    for (const t of roundTrips) {
      if (!t.open) continue;
      m.set(`${t.group}|${t.symbol}`, { entry: t.entry, days: t.holdingDays });
    }
    return m;
  }, [roundTrips]);

  const sinceOf = (p: PortfolioPosition) => heldSince.get(`${p.account}|${p.symbol}`) ?? null;

  const rows = useMemo(() => {
    const filtered = positions.filter(
      (p) => (!q || p.symbol.toLowerCase().includes(q)) && (!acct || p.account === acct)
    );
    if (sort === "held") {
      return [...filtered].sort((a, b) => (sinceOf(b)?.days ?? -1) - (sinceOf(a)?.days ?? -1));
    }
    const key = sort === "eval" ? "evalKrw" : sort === "pnl" ? "pnlKrw" : "weightPct";
    return [...filtered].sort((a, b) => (b[key] as number) - (a[key] as number));
  }, [positions, q, acct, sort, heldSince]); // eslint-disable-line react-hooks/exhaustive-deps

  const totalEval = useMemo(() => rows.reduce((s, p) => s + p.evalKrw, 0), [rows]);
  const totalPnl = useMemo(() => rows.reduce((s, p) => s + p.pnlKrw, 0), [rows]);
  const paged = usePaged(rows, `${q}|${acct}|${sort}`, 10);

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-sm font-medium text-slate-300">보유 종목</h2>
        <input
          list="portfolio-now-symbols"
          value={sym}
          onChange={(e) => setSym(e.target.value)}
          placeholder="종목 검색"
          className={cx.input + " sm:w-48"}
        />
        <datalist id="portfolio-now-symbols">
          {positions.map((p) => (
            <option key={p.symbol + p.account} value={p.symbol} />
          ))}
        </datalist>
        <Select
          value={acct}
          onChange={setAcct}
          className="w-auto min-w-36"
          items={[{ value: "", label: "전체 계좌" }, ...accounts.map((a) => ({ value: a, label: a }))]}
        />
        <Select
          value={sort}
          onChange={(v) => setSort(v as Sort)}
          className="w-auto min-w-32"
          items={[
            { value: "eval", label: "평가액 순" },
            { value: "pnl", label: "손익 순" },
            { value: "weight", label: "비중 순" },
            { value: "held", label: "보유 기간 순" },
          ]}
        />
        <div className="ml-auto text-sm text-slate-400">
          평가액 <span className="tabular-nums text-white">{won(totalEval)}</span>원
          <span className={"ml-2 tabular-nums " + pnlColor(totalPnl)}>{signed(totalPnl)}</span>
        </div>
      </div>

      <Panel className="overflow-x-auto rounded-xl bg-slate-900/40">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>계좌</th>
              <th className={cx.table.th}>태그</th>
              <th className={cx.table.th + " text-right"}>보유</th>
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
                <td className={cx.table.td + " font-medium text-white"}>
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
                <td className={cx.table.td + " whitespace-nowrap text-right tabular-nums"}>
                  {(() => {
                    const since = sinceOf(p);
                    if (!since) return <span className="text-slate-600">—</span>;
                    return (
                      <>
                        <div className="text-slate-300">{since.days}일</div>
                        <div className="text-xs text-slate-600">{since.entry} 부터</div>
                      </>
                    );
                  })()}
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
                <td colSpan={11} className="px-4 py-8 text-center text-sm text-slate-500">
                  조건에 맞는 보유 종목이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Panel>

      <Pagination
        page={paged.page}
        pageCount={paged.pageCount}
        from={paged.from}
        to={paged.to}
        total={paged.total}
        onPage={paged.setPage}
      />
    </section>
  );
}
