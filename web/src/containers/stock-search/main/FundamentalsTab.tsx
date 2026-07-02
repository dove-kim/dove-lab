"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";

interface Statement {
  fiscalYear: number | null;
  reportCode: string;
  fsDiv: string;
  rceptDt: string;
  amendment: boolean;
  revenue: number | null;
  grossProfit: number | null;
  operatingIncome: number | null;
  netIncome: number | null;
  totalAsset: number | null;
  totalLiability: number | null;
  totalEquity: number | null;
  cashFlowOperating: number | null;
}

const REPORT_LABEL: Record<string, string> = {
  "11011": "사업", "11012": "반기", "11013": "1분기", "11014": "3분기",
};

function eok(n: number | null): string {
  if (n == null) return "—";
  const v = n / 1e8;
  if (Math.abs(v) >= 10000) return `${(v / 10000).toFixed(1)}조`;
  return `${Math.round(v).toLocaleString()}억`;
}

export default function FundamentalsTab({ code }: { code: string }) {
  const [rows, setRows] = useState<Statement[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetch(`/api/stocks/${code}/fundamentals`)
      .then((r) => (r.ok ? r.json() : []))
      .then((d: Statement[]) => setRows(Array.isArray(d) ? d : []))
      .catch(() => setRows([]))
      .finally(() => setLoading(false));
  }, [code]);

  if (loading) return <div className="p-6 text-center text-sm text-slate-400">불러오는 중...</div>;
  if (rows.length === 0)
    return <div className="p-6 text-center text-sm text-slate-500">재무제표 데이터가 없습니다. (DART 수집 후 표시)</div>;

  return (
    <div className="h-full overflow-auto p-4">
      <table className={cx.table.root + " min-w-[720px]"}>
        <thead className={cx.table.head + " sticky top-0"}>
          <tr>
            <th className={cx.table.th}>연도</th>
            <th className={cx.table.th}>보고서</th>
            <th className={cx.table.th}>구분</th>
            <th className={cx.table.th + " text-right"}>매출</th>
            <th className={cx.table.th + " text-right"}>매출총이익</th>
            <th className={cx.table.th + " text-right"}>영업이익</th>
            <th className={cx.table.th + " text-right"}>순이익</th>
            <th className={cx.table.th + " text-right"}>자산</th>
            <th className={cx.table.th + " text-right"}>자본</th>
            <th className={cx.table.th + " text-right"}>영업CF</th>
          </tr>
        </thead>
        <tbody className={cx.table.body}>
          {rows.map((s, i) => (
            <tr key={i} className={cx.table.tr}>
              <td className={cx.table.td + " whitespace-nowrap"}>{s.fiscalYear ?? "—"}</td>
              <td className={cx.table.td + " whitespace-nowrap"}>
                {REPORT_LABEL[s.reportCode] ?? s.reportCode}
                {s.amendment && <span className="ml-1 text-[10px] text-amber-400">정정</span>}
              </td>
              <td className={cx.table.td + " text-xs text-slate-400"}>{s.fsDiv === "CFS" ? "연결" : "별도"}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.revenue)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.grossProfit)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.operatingIncome)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.netIncome)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.totalAsset)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.totalEquity)}</td>
              <td className={cx.table.td + " text-right tabular-nums"}>{eok(s.cashFlowOperating)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
