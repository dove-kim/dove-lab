"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";

interface InvestOpinion {
  date: string;
  opinion: string;
  prevOpinion: string;
  broker: string;
  goalPrice: string;
}

interface Estimate {
  available: boolean;
  analyst: string | null;
  opinion: string | null;
  periods: string[];
  income: string[][];
  indicators: string[][];
}

// KIS output2(추정손익)·output3(투자지표) 배열 순서에 대응하는 행 라벨
const INCOME_LABELS = ["매출액", "매출액 증감율", "영업이익", "영업이익 증감율", "순이익", "순이익 증감율"];
const INDICATOR_LABELS = ["EBITDA(십억)", "EPS(원)", "EPS 증감율", "PER", "EV/EBITDA", "ROE", "부채비율", "이자보상배율"];

/**
 * 종목 애널리스트 정보(투자의견·추정실적) — 버튼 눌러야 KIS 조회(무분별 호출 방지).
 */
export default function StockAnalystSection({ code }: { code: string }) {
  const [opinions, setOpinions] = useState<InvestOpinion[] | null>(null);
  const [opinionLoading, setOpinionLoading] = useState(false);
  const [estimate, setEstimate] = useState<Estimate | null>(null);
  const [estimateLoading, setEstimateLoading] = useState(false);

  // 종목 바뀌면 초기화 (다시 버튼 눌러 조회)
  useEffect(() => {
    setOpinions(null);
    setEstimate(null);
  }, [code]);

  async function loadOpinions() {
    setOpinionLoading(true);
    try {
      const res = await fetch(`/api/stocks/${code}/invest-opinion`);
      setOpinions(res.ok ? await res.json() : []);
    } catch {
      setOpinions([]);
    } finally {
      setOpinionLoading(false);
    }
  }

  async function loadEstimate() {
    setEstimateLoading(true);
    try {
      const res = await fetch(`/api/stocks/${code}/estimate`);
      setEstimate(res.ok ? await res.json() : null);
    } finally {
      setEstimateLoading(false);
    }
  }

  return (
    <div className="space-y-6 pt-2">
      {/* 투자의견 */}
      <section className="space-y-2">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-300">투자의견</h3>
          {opinions === null && (
            <button onClick={loadOpinions} disabled={opinionLoading} className={cx.btnSecondary}>
              {opinionLoading ? "조회 중…" : "조회"}
            </button>
          )}
        </div>
        {opinions !== null && (
          opinions.length === 0 ? (
            <p className="text-xs text-slate-500">최근 1년 투자의견이 없습니다.</p>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-white/10">
              <table className={cx.table.root + " min-w-[520px]"}>
                <thead className={cx.table.head}>
                  <tr>
                    <th className={cx.table.th}>일자</th>
                    <th className={cx.table.th}>회원사</th>
                    <th className={cx.table.th}>의견</th>
                    <th className={cx.table.th}>목표가</th>
                  </tr>
                </thead>
                <tbody className={cx.table.body}>
                  {opinions.map((o, i) => (
                    <tr key={i} className={cx.table.tr}>
                      <td className={cx.table.td + " font-mono text-xs whitespace-nowrap"}>{o.date}</td>
                      <td className={cx.table.td + " whitespace-nowrap"}>{o.broker}</td>
                      <td className={cx.table.td + " whitespace-nowrap"}>{o.opinion}</td>
                      <td className={cx.table.td + " text-right tabular-nums"}>
                        {o.goalPrice ? Number(o.goalPrice).toLocaleString() : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </section>

      {/* 추정실적 */}
      <section className="space-y-2">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-300">추정실적</h3>
          {estimate === null && (
            <button onClick={loadEstimate} disabled={estimateLoading} className={cx.btnSecondary}>
              {estimateLoading ? "조회 중…" : "조회"}
            </button>
          )}
        </div>
        {estimate !== null && (
          !estimate.available ? (
            <p className="text-xs text-slate-500">추정실적 데이터가 없습니다. (리서치 커버 종목만 제공)</p>
          ) : (
            <div className="space-y-2">
              <p className="text-xs text-slate-400">
                애널리스트 <span className="text-slate-200">{estimate.analyst || "-"}</span>
                {estimate.opinion && <> · 의견 <span className="text-indigo-300">{estimate.opinion}</span></>}
              </p>
              <div className="overflow-x-auto rounded-lg border border-white/10">
                <table className={cx.table.root + " min-w-[520px]"}>
                  <thead className={cx.table.head}>
                    <tr>
                      <th className={cx.table.th}>항목</th>
                      {estimate.periods.map((p, i) => (
                        <th key={i} className={cx.table.th + " text-right whitespace-nowrap"}>{p}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className={cx.table.body}>
                    {estimate.income.map((row, i) => (
                      <tr key={"inc" + i} className={cx.table.tr}>
                        <td className={cx.table.td + " whitespace-nowrap"}>{INCOME_LABELS[i] ?? `항목${i + 1}`}</td>
                        {row.map((v, j) => (
                          <td key={j} className={cx.table.td + " text-right tabular-nums"}>{v || "-"}</td>
                        ))}
                      </tr>
                    ))}
                    {estimate.indicators.map((row, i) => (
                      <tr key={"ind" + i} className={cx.table.tr}>
                        <td className={cx.table.td + " whitespace-nowrap text-slate-400"}>{INDICATOR_LABELS[i] ?? `지표${i + 1}`}</td>
                        {row.map((v, j) => (
                          <td key={j} className={cx.table.td + " text-right tabular-nums text-slate-400"}>{v || "-"}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )
        )}
      </section>
    </div>
  );
}
