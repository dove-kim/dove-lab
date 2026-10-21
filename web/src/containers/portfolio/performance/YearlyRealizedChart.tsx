"use client";

import { signed, pnlColor } from "../usePortfolioData";
import type { YearlyRealized } from "./symbolPerformance";

const HALF_PX = 44;
const MAX_BAR_PX = 38;
const MIN_BAR_PX = 3;

/** 부호를 붙인 퍼센트 표기. */
function pctText(pct: number): string {
  return `${pct > 0 ? "+" : ""}${pct}%`;
}

/**
 * 연도별 실현 손익 막대 — 0선 위로 이익, 아래로 손실. 매도 시점 기준이라 기간 선택과 무관하다.
 *
 * @param rows 연도별 실현 성과(연도 오름차순)
 */
export default function YearlyRealizedChart({ rows }: { rows: YearlyRealized[] }) {
  if (rows.length === 0) return null;

  const peak = Math.max(...rows.map((r) => Math.abs(r.pnlKrw)), 1);
  const barHeight = (value: number) => Math.max((Math.abs(value) / peak) * MAX_BAR_PX, MIN_BAR_PX);

  return (
    <div className="flex flex-col gap-2">
      <div className="text-xs text-slate-400">연도별 실현 손익 · 전체 기간</div>

      <div className="flex gap-2">
        {rows.map((r) => (
          <div key={r.year} className="flex flex-1 flex-col items-center">
            <span className={"text-xs tabular-nums " + pnlColor(r.pnlKrw)}>{signed(r.pnlKrw)}</span>
            <span className={"text-xs tabular-nums " + pnlColor(r.pnlKrw)}>
              {r.pct == null ? "" : pctText(r.pct)}
            </span>
          </div>
        ))}
      </div>

      <div className="relative flex gap-2" style={{ height: HALF_PX * 2 }}>
        <div className="pointer-events-none absolute inset-x-0 border-t border-white/10" style={{ top: HALF_PX }} />
        {rows.map((r) => (
          <div key={r.year} className="flex flex-1 flex-col">
            <div className="flex items-end" style={{ height: HALF_PX }}>
              {r.pnlKrw >= 0 && (
                <div className="w-full rounded-t-sm bg-rose-500/50" style={{ height: barHeight(r.pnlKrw) }} />
              )}
            </div>
            <div className="flex items-start" style={{ height: HALF_PX }}>
              {r.pnlKrw < 0 && (
                <div className="w-full rounded-b-sm bg-sky-500/50" style={{ height: barHeight(r.pnlKrw) }} />
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="flex gap-2">
        {rows.map((r) => (
          <div key={r.year} className="flex-1 text-center text-xs text-slate-600">
            {r.year}
          </div>
        ))}
      </div>

      <p className="text-xs text-slate-600">
        수익률은 그 해 판 것들의 매입 원가 대비입니다. 자산 전체 대비 연수익률은 과거 평가액이 없어 낼 수 없습니다.
      </p>
    </div>
  );
}
