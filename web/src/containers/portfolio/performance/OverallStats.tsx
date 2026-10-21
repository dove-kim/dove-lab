"use client";

import { useMemo } from "react";
import { type PortfolioRoundTrip, type PortfolioSummary } from "@/types/portfolio";
import { signed, pnlColor } from "../usePortfolioData";
import { StatTile } from "../StatTile";
import { type Period, inPeriod } from "./period";

/**
 * 전체 성과 블록 — 기간 안에서 확정된 실현 손익과 매매 통계를 보여주고, 현재 평가 손익을 함께 둔다.
 *
 * @param summary    포트폴리오 요약
 * @param roundTrips 라운드트립 목록
 * @param period     집계 기간
 */
export default function OverallStats({
  summary,
  roundTrips,
  period,
}: {
  summary: PortfolioSummary;
  roundTrips: PortfolioRoundTrip[];
  period: Period;
}) {
  const realized = useMemo(() => {
    const events = roundTrips.flatMap((t) => t.realizations).filter((r) => inPeriod(r.date, period));
    if (events.length === 0) return null;
    const wins = events.filter((r) => r.pnlKrw > 0).length;
    return {
      krw: events.reduce((s, r) => s + r.pnlKrw, 0),
      count: events.length,
      winRate: (wins / events.length) * 100,
    };
  }, [roundTrips, period]);

  const closedInPeriod = useMemo(
    () => roundTrips.filter((t) => !t.open && t.exit != null && inPeriod(t.exit, period)),
    [roundTrips, period]
  );

  const avgDays = closedInPeriod.length
    ? closedInPeriod.reduce((s, t) => s + t.holdingDays, 0) / closedInPeriod.length
    : null;

  const wholePeriod = period.key === "all";

  return (
    <section className="flex flex-col gap-3">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="실현 손익"
          value={realized ? signed(realized.krw) + "원" : "—"}
          sub={realized ? `${period.label} · 매도 ${realized.count}회` : `${period.label} · 매도 없음`}
          tone={realized ? pnlColor(realized.krw) : undefined}
        />
        <StatTile
          label="평가 손익 (보유)"
          value={signed(summary.evalPnlKrw) + "원"}
          sub={wholePeriod ? `${summary.evalPnlPct >= 0 ? "+" : ""}${summary.evalPnlPct}%` : "기간 무관 · 현재 기준"}
          tone={pnlColor(summary.evalPnlKrw)}
        />
        <StatTile
          label="누적 손익"
          value={signed(summary.growthKrw) + "원"}
          sub="총자산 − 순납입 · 전체 기준"
          tone={pnlColor(summary.growthKrw)}
        />
        <StatTile
          label="연환산 수익률 (XIRR)"
          value={`${summary.xirrPct >= 0 ? "+" : ""}${summary.xirrPct}%`}
          sub="전체 현금흐름 기준"
          tone={pnlColor(summary.xirrPct)}
        />
      </div>

      {realized && (
        <div className="flex flex-wrap gap-x-5 gap-y-1 text-xs text-slate-400">
          <span>
            매도 승률 <span className="tabular-nums text-white">{realized.winRate.toFixed(0)}%</span>
          </span>
          {avgDays != null && (
            <span>
              평균 보유 <span className="tabular-nums text-white">{avgDays.toFixed(0)}일</span>
              <span className="ml-1 text-slate-600">· 청산 {closedInPeriod.length}건</span>
            </span>
          )}
        </div>
      )}

      <p className="text-xs text-slate-600">
        실현 손익은 매도 시점에 귀속되므로 기간을 정확히 자른다. 평가 손익과 XIRR은 과거 시점으로 쪼갤 수 없어 현재 기준이다.
      </p>
    </section>
  );
}
