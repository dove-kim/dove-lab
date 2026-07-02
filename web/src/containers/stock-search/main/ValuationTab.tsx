"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";

interface Valuation {
  tradeDate: string;
  closePrice: number | null;
  marketCap: number | null;
  per: number | null;
  pbr: number | null;
  psr: number | null;
  gpa: number | null;
}

function cap(n: number | null): string {
  if (n == null) return "—";
  const v = n / 1e8;
  if (Math.abs(v) >= 10000) return `${(v / 10000).toFixed(2)}조`;
  return `${Math.round(v).toLocaleString()}억`;
}

function ratio(n: number | null): string {
  return n == null ? "—" : n.toFixed(2);
}

export default function ValuationTab({ code }: { code: string }) {
  const [rows, setRows] = useState<Valuation[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetch(`/api/stocks/${code}/valuations`)
      .then((r) => (r.ok ? r.json() : []))
      .then((d: Valuation[]) => setRows(Array.isArray(d) ? d : []))
      .catch(() => setRows([]))
      .finally(() => setLoading(false));
  }, [code]);

  if (loading) return <div className="p-6 text-center text-sm text-slate-400">불러오는 중...</div>;
  if (rows.length === 0)
    return <div className="p-6 text-center text-sm text-slate-500">밸류에이션 데이터가 없습니다. (수집·계산 후 표시)</div>;

  return (
    <div className="h-full overflow-auto p-4">
      <table className={cx.table.root + " min-w-[560px]"}>
        <thead className={cx.table.head + " sticky top-0"}>
          <tr>
            <th className={cx.table.th}>거래일</th>
            <th className={cx.table.th + " text-right"}>종가</th>
            <th className={cx.table.th + " text-right"}>시가총액</th>
            <th className={cx.table.th + " text-right"}>PER</th>
            <th className={cx.table.th + " text-right"}>PBR</th>
            <th className={cx.table.th + " text-right"}>PSR</th>
            <th className={cx.table.th + " text-right"}>GP/A</th>
          </tr>
        </thead>
        <tbody className={cx.table.body}>
          {rows.map((v) => (
            <tr key={v.tradeDate} className={cx.table.tr}>
              <td className={cx.table.td + " whitespace-nowrap"}>{v.tradeDate}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{v.closePrice != null ? v.closePrice.toLocaleString() : "—"}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{cap(v.marketCap)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{ratio(v.per)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{ratio(v.pbr)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{ratio(v.psr)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{ratio(v.gpa)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
