"use client";

import { Fragment, useState } from "react";
import Panel from "@/components/Panel";
import { cx } from "@/utils/cx";
import { type PortfolioRoundTrip, natMoney, natSigned } from "@/types/portfolio";
import { signed, pnlColor } from "../usePortfolioData";
import type { SymbolPerformance } from "./symbolPerformance";

/** 사이클 한 건의 기간 표기 — 보유 중이면 끝을 열어 둔다. */
function cycleRange(cycle: PortfolioRoundTrip): string {
  return `${cycle.entry} → ${cycle.exit ?? "진행중"}`;
}

/**
 * 종목별 성과 표 — 종목 한 줄로 누적 성과를 보고, 펼치면 사이클과 매도 내역이 나온다.
 *
 * @param rows 종목별 집계 결과
 */
export default function SymbolPerformanceTable({ rows }: { rows: SymbolPerformance[] }) {
  const [opened, setOpened] = useState<string | null>(null);

  return (
    <div className="flex flex-col gap-3">
      <Panel className="overflow-x-auto rounded-xl bg-slate-900/40">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>보유기간</th>
              <th className={cx.table.th + " text-right"}>매도</th>
              <th className={cx.table.th + " text-right"}>실현손익</th>
              <th className={cx.table.th + " text-right"}>수익률</th>
              <th className={cx.table.th + " text-right"}>배당</th>
              <th className={cx.table.th + " text-right"}>상태</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {rows.map((r) => {
              const isOpen = opened === r.symbol;
              return (
                <Fragment key={r.symbol}>
                  <tr
                    className={cx.table.tr + " cursor-pointer"}
                    onClick={() => setOpened(isOpen ? null : r.symbol)}
                  >
                    <td className={cx.table.td + " font-medium text-white"}>
                      <span className="mr-1 text-slate-500">{isOpen ? "▾" : "▸"}</span>
                      {r.symbol}
                      {r.currency !== "KRW" && <span className="ml-1 text-xs text-slate-500">{r.currency}</span>}
                    </td>
                    <td className={cx.table.td + " whitespace-nowrap"}>
                      <span className="tabular-nums">{r.heldDays}일</span>
                      <span className="ml-1 text-xs text-slate-600">· {r.cycles.length}회</span>
                    </td>
                    <td className={cx.table.td + " text-right tabular-nums"}>{r.sellCount}회</td>
                    <td className={cx.table.td + " text-right tabular-nums " + pnlColor(r.realizedKrw)}>
                      {r.sellCount === 0 ? <span className="text-slate-600">—</span> : signed(r.realizedKrw)}
                    </td>
                    <td className={cx.table.td + " text-right tabular-nums " + pnlColor(r.realizedKrw)}>
                      {r.realizedPct == null ? (
                        <span className="text-slate-600">—</span>
                      ) : (
                        `${r.realizedPct > 0 ? "+" : ""}${r.realizedPct}%`
                      )}
                    </td>
                    <td className={cx.table.td + " text-right tabular-nums"}>
                      {r.dividendNat > 0 ? natMoney(r.dividendNat, r.currency) : <span className="text-slate-600">—</span>}
                    </td>
                    <td className={cx.table.td + " text-right"}>
                      <span
                        className={
                          "inline-block rounded-full px-2 py-0.5 text-xs " +
                          (r.open ? "bg-indigo-500/20 text-indigo-200" : "bg-white/5 text-slate-400")
                        }
                      >
                        {r.open ? "보유중" : "청산"}
                      </span>
                    </td>
                  </tr>

                  {isOpen && (
                    <tr className="bg-white/3">
                      <td colSpan={7} className="px-4 py-4">
                        <div className="flex flex-col gap-3">
                          <table className={cx.table.root}>
                            <thead className={cx.table.head}>
                              <tr>
                                <th className={cx.table.th}>사이클</th>
                                <th className={cx.table.th}>계좌</th>
                                <th className={cx.table.th + " text-right"}>보유일</th>
                                <th className={cx.table.th + " text-right"}>평단</th>
                                <th className={cx.table.th + " text-right"}>실현손익</th>
                                <th className={cx.table.th + " text-right"}>수익률</th>
                              </tr>
                            </thead>
                            <tbody className={cx.table.body}>
                              {r.cycles.map((c) => (
                                <Fragment key={c.id}>
                                  <tr className={cx.table.tr}>
                                    <td className={cx.table.td + " whitespace-nowrap text-slate-300"}>
                                      {cycleRange(c)}
                                    </td>
                                    <td className={cx.table.td + " whitespace-nowrap"}>{c.group}</td>
                                    <td className={cx.table.td + " text-right tabular-nums"}>{c.holdingDays}</td>
                                    <td className={cx.table.td + " text-right tabular-nums"}>
                                      {natMoney(c.avgNat, c.currency)}
                                    </td>
                                    <td className={cx.table.td + " text-right tabular-nums " + pnlColor(c.pnlKrw)}>
                                      {natSigned(c.pnlNat, c.currency)}
                                    </td>
                                    <td className={cx.table.td + " text-right tabular-nums " + pnlColor(c.pnlPct)}>
                                      {`${c.pnlPct > 0 ? "+" : ""}${c.pnlPct}%`}
                                    </td>
                                  </tr>
                                  {c.realizations.map((rel) => (
                                    <tr key={`${c.id}-${rel.date}-${rel.quantity}`}>
                                      <td className={cx.table.td + " pl-8 text-xs text-slate-500"}>
                                        {rel.date} 매도 {rel.quantity}
                                      </td>
                                      <td className={cx.table.td} />
                                      <td className={cx.table.td} />
                                      <td className={cx.table.td} />
                                      <td className={cx.table.td + " text-right text-xs tabular-nums " + pnlColor(rel.pnlKrw)}>
                                        {natSigned(rel.pnlNat, c.currency)}
                                      </td>
                                      <td className={cx.table.td + " text-right text-xs tabular-nums " + pnlColor(rel.pnlPct)}>
                                        {`${rel.pnlPct > 0 ? "+" : ""}${rel.pnlPct}%`}
                                      </td>
                                    </tr>
                                  ))}
                                </Fragment>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
            {rows.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-sm text-slate-500">
                  이 기간에 사고팔거나 들고 있던 종목이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Panel>

      <p className="text-xs text-slate-600">
        보유기간은 달력에서 실제로 들고 있던 날만 셉니다. 전량 판 뒤 다시 사기까지의 공백은 빠지고, 여러 계좌가 같은 날
        들고 있어도 하루는 하루입니다.
      </p>
    </div>
  );
}
