"use client";

import { useMemo, useState } from "react";
import Panel from "@/components/Panel";
import type { FxRateCoverage, FxRateOverview, FxRateRow } from "@/types/ops";
import { cn } from "@/utils/cn";
import { cx } from "@/utils/cx";

/** 고시일이 오늘로부터 며칠 밀렸는지 계산한다(일 단위). */
function staleDays(latestDate: string | null): number | null {
  if (!latestDate) return null;
  const today = new Date();
  const latest = new Date(`${latestDate}T00:00:00`);
  const diffMs = today.setHours(0, 0, 0, 0) - latest.getTime();
  return Math.max(0, Math.round(diffMs / 86_400_000));
}

/** 신선도 배지의 문구와 색을 정한다. 주말을 감안해 3일까지는 정상으로 본다. */
function freshness(coverage: FxRateCoverage): { label: string; className: string } {
  const days = staleDays(coverage.latestDate);
  if (days === null) {
    return { label: "이력 없음", className: "bg-slate-500/15 text-slate-300" };
  }
  if (days <= 3) {
    return { label: "최신", className: "bg-emerald-500/15 text-emerald-300" };
  }
  return { label: `${days}일 지연`, className: "bg-amber-500/15 text-amber-300" };
}

function formatRate(rate: string | null): string {
  if (!rate) return "—";
  return Number(rate).toLocaleString("ko-KR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

interface Props {
  initial: FxRateOverview;
}

export default function FxRatesClient({ initial }: Props) {
  const [overview, setOverview] = useState<FxRateOverview>(initial);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const currencies = useMemo(() => overview.coverages.map((c) => c.currency), [overview.coverages]);

  // 평평한 이력을 고시일 기준으로 접어 통화를 열로 세운다.
  const rowsByDate = useMemo(() => {
    const byDate = new Map<string, Record<string, string>>();
    overview.recent.forEach((r: FxRateRow) => {
      const row = byDate.get(r.rateDate) ?? {};
      row[r.currency] = r.rate;
      byDate.set(r.rateDate, row);
    });
    return Array.from(byDate.entries()).sort((a, b) => b[0].localeCompare(a[0]));
  }, [overview.recent]);

  async function reload() {
    setLoading(true);
    try {
      const res = await fetch("/api/admin/ops/fx?days=30");
      if (res.ok) setOverview(await res.json());
    } finally {
      setLoading(false);
    }
  }

  async function refresh() {
    setRefreshing(true);
    setMessage(null);
    try {
      const res = await fetch("/api/admin/ops/fx/refresh", { method: "POST" });
      if (res.ok) {
        const data = await res.json();
        setMessage(data.saved > 0 ? `${data.saved}행을 새로 받았습니다.` : "새로 받을 환율이 없습니다.");
        await reload();
      } else {
        setMessage("채우기에 실패했습니다.");
      }
    } finally {
      setRefreshing(false);
    }
  }

  return (
    <div className="flex min-h-full flex-col gap-5 p-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-xl font-bold text-white">환율 이력</h1>
          <p className="mt-1 text-sm leading-relaxed text-slate-400">
            입금일과 매도일 환율로 손익을 환산하기 위해 원통화별 고시 환율을 일자 단위로 쌓는다.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className={cx.btnSecondary} onClick={reload} disabled={loading}>
            {loading ? "불러오는 중" : "새로고침"}
          </button>
          <button type="button" className={cx.btnPrimary} onClick={refresh} disabled={refreshing}>
            {refreshing ? "채우는 중" : "지금 채우기"}
          </button>
        </div>
      </header>

      {message && (
        <p className="rounded-lg border border-indigo-500/25 bg-indigo-500/10 px-4 py-3 text-sm text-indigo-200">
          {message}
        </p>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-slate-300">통화별 보유 현황</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {overview.coverages.map((c) => {
            const badge = freshness(c);
            return (
              <Panel key={c.currency} className="flex flex-col gap-4 p-5">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-base font-semibold text-slate-100">{c.currency}</span>
                  <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${badge.className}`}>
                    {badge.label}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <span className="text-2xl font-semibold tracking-tight text-white">{formatRate(c.latestRate)}</span>
                  <span className="text-xs text-slate-400">
                    {c.latestDate ? `${c.latestDate} 고시` : "보유 종목이 생기면 채운다"}
                  </span>
                </div>
                <dl className="flex flex-col gap-2 border-t border-white/5 pt-3 text-xs">
                  <div className="flex justify-between gap-2">
                    <dt className="text-slate-400">보유 구간</dt>
                    <dd className="text-slate-300">{c.earliestDate ? `${c.earliestDate} 부터` : "없음"}</dd>
                  </div>
                  <div className="flex justify-between gap-2">
                    <dt className="text-slate-400">행 수</dt>
                    <dd className="text-slate-300">{c.rowCount.toLocaleString("ko-KR")}</dd>
                  </div>
                </dl>
              </Panel>
            );
          })}
        </div>
      </section>

      <div className="flex flex-wrap items-start gap-5">
        <section className="min-w-0 flex-[999_1_560px]">
          <Panel className="overflow-hidden">
            <div className="border-b border-white/10 px-5 py-4">
              <h2 className="text-sm font-semibold text-slate-100">최근 고시</h2>
              <p className="mt-1 text-xs text-slate-400">
                휴장일은 행이 없다. 환산할 때는 그 날 이하 가장 최근 고시를 쓴다.
              </p>
            </div>
            {rowsByDate.length === 0 ? (
              <p className={cx.emptyState}>아직 받아온 환율이 없습니다.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className={cx.table.root}>
                  <thead className={cx.table.head}>
                    <tr>
                      <th scope="col" className={cx.table.th}>
                        고시일
                      </th>
                      {currencies.map((currency) => (
                        <th key={currency} scope="col" className={cn(cx.table.th, "text-right")}>
                          {currency}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rowsByDate.map(([date, rates]) => (
                      <tr key={date} className="border-b border-white/5">
                        <td className="px-4 py-3 text-slate-200">{date}</td>
                        {currencies.map((currency) => (
                          <td key={currency} className="px-4 py-3 text-right tabular-nums text-slate-200">
                            {rates[currency] ? formatRate(rates[currency]) : <span className="text-slate-600">—</span>}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Panel>
        </section>

        <aside className="flex min-w-0 flex-[1_1_320px] flex-col gap-5">
          <Panel className="flex flex-col gap-4 p-5">
            <h2 className="text-sm font-semibold text-slate-100">채우기 기준</h2>
            <dl className="flex flex-col gap-3 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-slate-400">백필 시작점</dt>
                <dd className="text-right text-slate-200">
                  {overview.backfillStartDate}
                  <span className="block text-xs text-slate-500">가장 오래된 거래일</span>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-400">자동 실행</dt>
                <dd className="text-right text-slate-200">
                  매일 07:30
                  <span className="block text-xs text-slate-500">시세 일배치와 함께</span>
                </dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-slate-400">출처</dt>
                <dd className="text-right text-slate-200">
                  Frankfurter
                  <span className="block text-xs text-slate-500">ECB 기준환율</span>
                </dd>
              </div>
            </dl>
            <p className="rounded-lg border border-indigo-500/20 bg-indigo-500/10 px-3.5 py-3 text-xs leading-relaxed text-indigo-200">
              채우기는 통화마다 마지막 고시일 다음날부터 오늘까지만 받는다. 여러 번 눌러도 같은 결과가 된다.
            </p>
          </Panel>

          <Panel className="flex flex-col gap-3 p-5">
            <h2 className="text-sm font-semibold text-slate-100">환산에 쓰이는 곳</h2>
            <ul className="flex flex-col gap-2.5 text-sm leading-relaxed text-slate-300">
              <li className="flex gap-2.5">
                <span aria-hidden="true" className="text-indigo-400">
                  •
                </span>
                <span>
                  XIRR 현금흐름은 <strong className="font-semibold text-slate-100">입금일</strong> 환율
                </span>
              </li>
              <li className="flex gap-2.5">
                <span aria-hidden="true" className="text-indigo-400">
                  •
                </span>
                <span>
                  실현손익은 <strong className="font-semibold text-slate-100">매도일</strong> 환율
                </span>
              </li>
              <li className="flex gap-2.5">
                <span aria-hidden="true" className="text-indigo-400">
                  •
                </span>
                <span>
                  현금잔액과 보유 평가액은 <strong className="font-semibold text-slate-100">최신</strong> 환율
                </span>
              </li>
            </ul>
          </Panel>
        </aside>
      </div>
    </div>
  );
}
