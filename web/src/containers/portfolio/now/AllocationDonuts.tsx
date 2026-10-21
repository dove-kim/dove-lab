"use client";

import { useMemo } from "react";
import { type PortfolioPosition, type PortfolioSummary } from "@/types/portfolio";
import { won, fxRateOf } from "../usePortfolioData";

// 카테고리 팔레트(다크 서피스 검증 완료, 고정 순서). 식별은 범례로 보강한다.
const DONUT_PALETTE = ["#3987e5", "#199e70", "#c98500", "#008300", "#9085e9", "#e66767", "#d55181", "#d95926"];

/**
 * 자산 배분 도넛 — 이름·비중(%)·평가액을 원그래프와 범례로 보여준다.
 */
function DonutChart({ title, rows, total }: { title: string; rows: [string, number][]; total: number }) {
  if (total <= 0) return null;

  // 팔레트(8) 초과분은 "기타"로 접는다.
  const top = rows.slice(0, 7);
  const restSum = rows.slice(7).reduce((s, [, v]) => s + v, 0);
  const slices: [string, number][] = restSum > 0 ? [...top, ["기타", restSum]] : top;

  const r = 45;
  const circumference = 2 * Math.PI * r;
  const gap = 2;
  let acc = 0;
  const segs = slices.map(([name, value], i) => {
    const frac = value / total;
    const len = Math.max(frac * circumference - gap, 0.5);
    const seg = { name, value, frac, color: DONUT_PALETTE[i % DONUT_PALETTE.length], len, offset: -acc * circumference };
    acc += frac;
    return seg;
  });

  return (
    <div className="rounded-xl bg-white/5 p-4">
      <div className="mb-3 text-sm font-medium text-slate-300">{title}</div>
      <div className="flex items-center gap-4">
        <svg viewBox="0 0 120 120" className="h-28 w-28 shrink-0 -rotate-90" role="img" aria-label={title}>
          <circle cx="60" cy="60" r={r} fill="none" strokeWidth="16" className="stroke-white/5" />
          {segs.map((s) => (
            <circle
              key={s.name}
              cx="60"
              cy="60"
              r={r}
              fill="none"
              stroke={s.color}
              strokeWidth="16"
              strokeDasharray={`${s.len} ${circumference - s.len}`}
              strokeDashoffset={s.offset}
            />
          ))}
        </svg>
        <ul className="flex min-w-0 flex-1 flex-col gap-1.5 text-sm">
          {segs.map((s) => (
            <li key={s.name} className="flex items-center gap-2">
              <span className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ backgroundColor: s.color }} />
              <span className="truncate text-slate-300">{s.name}</span>
              <span className="ml-auto tabular-nums text-slate-400">{Math.round(s.frac * 1000) / 10}%</span>
              <span className="w-24 text-right tabular-nums text-slate-500">{won(s.value)}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

/**
 * 자산 배분 — 종목별·계좌별·통화별 비중을 도넛으로 보여준다.
 *
 * @param positions 보유 포지션 목록
 * @param summary   포트폴리오 요약(통화별 현금 포함)
 */
export default function AllocationDonuts({
  positions,
  summary,
}: {
  positions: PortfolioPosition[];
  summary: PortfolioSummary | null;
}) {
  const derived = useMemo(() => {
    const sumBy = (key: (p: PortfolioPosition) => string) => {
      const m = new Map<string, number>();
      for (const p of positions) m.set(key(p), (m.get(key(p)) ?? 0) + p.evalKrw);
      return Array.from(m.entries()).sort((a, b) => b[1] - a[1]);
    };
    const total = positions.reduce((s, p) => s + p.evalKrw, 0);

    // 통화별 현금(원화 환산). 환율은 보유 포지션에서 역산(KRW=1). 양수 현금만 비중에 반영.
    const fxByCur = new Map<string, number>();
    for (const p of positions) {
      if (p.currency === "KRW") continue;
      const fx = fxRateOf(p);
      if (fx != null && !fxByCur.has(p.currency)) fxByCur.set(p.currency, fx);
    }
    const cashRows: [string, number][] = [];
    for (const [cur, nat] of Object.entries(summary?.cashByCurrency ?? {})) {
      const krw = cur === "KRW" ? nat : nat * (fxByCur.get(cur) ?? 0);
      if (krw > 1) cashRows.push([`현금·${cur}`, Math.round(krw)]);
    }
    const cashTotal = cashRows.reduce((s, [, v]) => s + v, 0);

    return {
      bySymbol: sumBy((p) => p.symbol),
      byAccount: sumBy((p) => p.account),
      byCurrency: [...sumBy((p) => p.currency), ...cashRows].sort((a, b) => b[1] - a[1]),
      total,
      totalWithCash: total + cashTotal,
    };
  }, [positions, summary]);

  if (derived.total <= 0) return null;

  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-sm font-medium text-slate-300">자산 배분</h2>
      <DonutChart title="종목별 비중" rows={derived.bySymbol} total={derived.total} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <DonutChart title="계좌별 비중" rows={derived.byAccount} total={derived.total} />
        <DonutChart title="통화별 비중 (현금 포함)" rows={derived.byCurrency} total={derived.totalWithCash} />
      </div>
    </section>
  );
}
