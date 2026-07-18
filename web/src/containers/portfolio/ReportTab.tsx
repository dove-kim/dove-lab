"use client";

import { useMemo } from "react";
import { type PortfolioPosition, type PortfolioSummary } from "@/types/portfolio";
import { usePortfolioData, won, signed, pnlColor } from "./usePortfolioData";
import { useScope, scopeBase, ScopeSelector } from "./scopeView";
import { StatTile } from "./StatTile";

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
    <div className="bg-white/5 rounded-xl p-4">
      <div className="text-sm font-medium text-slate-300 mb-3">{title}</div>
      <div className="flex items-center gap-4">
        <svg viewBox="0 0 120 120" className="w-28 h-28 shrink-0 -rotate-90" role="img" aria-label={title}>
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
        <ul className="flex-1 flex flex-col gap-1.5 text-sm min-w-0">
          {segs.map((s) => (
            <li key={s.name} className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-sm shrink-0" style={{ backgroundColor: s.color }} />
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
 * 리포트 탭 — 성과/개요. 총자산·순납입·누적 손익·XIRR·월 배당 예상 + 자산 배분(종목/계좌/통화). 평가손익·보유 상세는 보유종목에서.
 */
export default function ReportTab() {
  const [scope] = useScope();
  const { data: summary, err } = usePortfolioData<PortfolioSummary>(`${scopeBase(scope)}/summary`);
  const { data: positions } = usePortfolioData<PortfolioPosition[]>(`${scopeBase(scope)}/positions`);

  const derived = useMemo(() => {
    const list = positions ?? [];
    const monthlyDividend = Math.round(
      list.reduce((s, p) => s + (p.evalKrw * (p.annualDividendPct ?? 0)) / 100, 0) / 12
    );
    const sumBy = (key: (p: PortfolioPosition) => string) => {
      const m = new Map<string, number>();
      for (const p of list) m.set(key(p), (m.get(key(p)) ?? 0) + p.evalKrw);
      return Array.from(m.entries()).sort((a, b) => b[1] - a[1]);
    };
    const total = list.reduce((s, p) => s + p.evalKrw, 0);
    return {
      monthlyDividend,
      bySymbol: sumBy((p) => p.symbol),
      byAccount: sumBy((p) => p.account),
      byCurrency: sumBy((p) => p.currency),
      total,
    };
  }, [positions]);

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">리포트를 불러오지 못했습니다.</p>;
  if (!summary) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center gap-2">
        <span className="text-sm text-slate-400">보기 대상</span>
        <ScopeSelector />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatTile label="총 평가자산" value={won(summary.totalKrw)} sub={`현금 ${won(summary.cashKrw)} 포함`} />
        <StatTile label="순납입 (입금−출금)" value={won(summary.netContribKrw)} />
        <StatTile label="누적 손익" value={signed(summary.growthKrw)} sub="실현+평가 (총자산−순납입)" tone={pnlColor(summary.growthKrw)} />
        <StatTile
          label="월 배당 예상"
          value={won(derived.monthlyDividend) + "원"}
          sub={`연 ${won(derived.monthlyDividend * 12)}원`}
          tone="text-amber-300"
        />
      </div>

      <div className="flex items-center gap-2">
        <span className="text-sm text-slate-400">연환산 수익률(XIRR)</span>
        <span className={"text-xl font-semibold tabular-nums " + pnlColor(summary.xirrPct)}>
          {(summary.xirrPct >= 0 ? "+" : "") + summary.xirrPct}%
        </span>
      </div>

      {derived.total > 0 ? (
        <div className="flex flex-col gap-3">
          <DonutChart title="종목별 비중" rows={derived.bySymbol} total={derived.total} />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            <DonutChart title="계좌별 비중" rows={derived.byAccount} total={derived.total} />
            <DonutChart title="통화별 비중" rows={derived.byCurrency} total={derived.total} />
          </div>
        </div>
      ) : (
        <p className="text-sm text-slate-500 py-6 text-center rounded-xl border border-white/10 bg-slate-900/40">
          보유 종목이 없습니다. 상세·배당은 보유종목 탭에서.
        </p>
      )}

      <p className="text-xs text-slate-600">
        보유 종목 상세·배당률 설정은 <span className="text-slate-400">보유종목</span> 탭에서. 월 배당 예상은 보유 종목의 연배당률 합산(현재 환율).
      </p>
    </div>
  );
}
