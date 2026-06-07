"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";

interface InvestorFlowBar {
  date: string;
  individualNet: number;
  institutionNet: number;
  foreignNet: number;
}

const SOURCE_OPTIONS = ["KRX", "NXT"] as const;
type Source = (typeof SOURCE_OPTIONS)[number];

const CHART_H = 140;
const LABEL_H = 18;
const BAR_W = 3;
const BAR_GAP = 1;
const GROUP_GAP = 4;
const GROUP_W = 3 * BAR_W + 2 * BAR_GAP + GROUP_GAP;
const AXIS_Y = CHART_H / 2;
const MAX_BAR_H = AXIS_Y - 4;
const INVESTOR_COLORS = ["#60a5fa", "#f59e0b", "#34d399"];

function InvestorFlowChart({ data }: { data: InvestorFlowBar[] }) {
  // chart displays oldest → newest (left → right); API returns newest first
  const sorted = [...data].reverse();
  const maxAbs = Math.max(
    1,
    ...sorted.flatMap((d) => [
      Math.abs(d.individualNet),
      Math.abs(d.institutionNet),
      Math.abs(d.foreignNet),
    ])
  );
  const totalW = Math.max(sorted.length * GROUP_W, 1);

  function bh(value: number) {
    return Math.max(1, (Math.abs(value) / maxAbs) * MAX_BAR_H);
  }

  function by(value: number) {
    return value >= 0 ? AXIS_Y - bh(value) : AXIS_Y;
  }

  return (
    <div className="overflow-x-auto rounded bg-white/[0.02] border border-white/5 p-2">
      <svg width={totalW} height={CHART_H + LABEL_H} style={{ display: "block", minWidth: "100%" }}>
        <line
          x1={0} y1={AXIS_Y} x2={totalW} y2={AXIS_Y}
          stroke="rgba(255,255,255,0.12)" strokeWidth={1}
        />
        {sorted.map((d, i) => {
          const x = i * GROUP_W;
          const nets = [d.individualNet, d.institutionNet, d.foreignNet];
          return (
            <g key={d.date}>
              {nets.map((v, j) => (
                <rect
                  key={j}
                  x={x + j * (BAR_W + BAR_GAP)}
                  y={by(v)}
                  width={BAR_W}
                  height={bh(v)}
                  fill={INVESTOR_COLORS[j]}
                  opacity={0.75}
                />
              ))}
              {i % 10 === 0 && (
                <text
                  x={x + GROUP_W / 2}
                  y={CHART_H + LABEL_H - 2}
                  fontSize={9}
                  textAnchor="middle"
                  fill="#475569"
                >
                  {d.date.slice(5)}
                </text>
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

function fmtNet(v: number): string {
  if (v === 0) return "0";
  const sign = v > 0 ? "+" : "-";
  const abs = Math.abs(v);
  if (abs >= 1_000_000) return sign + (abs / 1_000_000).toFixed(1) + "M";
  if (abs >= 1_000) return sign + (abs / 1_000).toFixed(0) + "K";
  return (v > 0 ? "+" : "") + v.toLocaleString();
}

function netColor(v: number) {
  if (v > 0) return "text-red-400";
  if (v < 0) return "text-blue-400";
  return "text-slate-500";
}

function InvestorFlowTable({ data }: { data: InvestorFlowBar[] }) {
  const thBase = "text-xs text-slate-400 px-4 py-2.5 font-medium";
  const tdBase = "px-4 py-2.5";

  return (
    <div className="overflow-x-auto">
      <table className={cx.table.root + " min-w-[400px]"}>
        <thead className={cx.table.head}>
          <tr>
            <th className={thBase + " text-left"}>날짜</th>
            <th className={thBase + " text-right text-blue-400"}>개인</th>
            <th className={thBase + " text-right text-amber-400"}>기관</th>
            <th className={thBase + " text-right text-emerald-400"}>외국인</th>
          </tr>
        </thead>
        <tbody className={cx.table.body}>
          {data.map((d) => (
            <tr key={d.date} className={cx.table.tr}>
              <td className={tdBase + " font-mono text-xs text-slate-400"}>{d.date}</td>
              <td className={tdBase + " text-right font-mono text-xs " + netColor(d.individualNet)}>
                {fmtNet(d.individualNet)}
              </td>
              <td className={tdBase + " text-right font-mono text-xs " + netColor(d.institutionNet)}>
                {fmtNet(d.institutionNet)}
              </td>
              <td className={tdBase + " text-right font-mono text-xs " + netColor(d.foreignNet)}>
                {fmtNet(d.foreignNet)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * 투자자별 일별 순매수 동향 탭 (개인·기관·외국인).
 */
export default function InvestorFlowTab({ code }: { code: string }) {
  const [data, setData]     = useState<InvestorFlowBar[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [limit, setLimit]   = useState(60);
  const [source, setSource] = useState<Source>("KRX");

  useEffect(() => {
    setData(null);
    setLimit(60);
  }, [code]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetch(`/api/stocks/${code}/investor-flow?source=${source}&limit=${limit}`)
      .then((r) => (r.ok ? r.json() : []))
      .catch(() => [])
      .then((d: InvestorFlowBar[]) => {
        if (!cancelled) {
          setData(d);
          setLoading(false);
        }
      });
    return () => { cancelled = true; };
  }, [code, limit, source]);

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* 툴바 */}
      <div className="flex items-center gap-3 px-4 py-2 border-b border-white/10 flex-shrink-0">
        <div className="flex rounded border border-white/10 overflow-hidden text-xs">
          {SOURCE_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => setSource(s)}
              className={`px-2.5 py-1.5 transition ${
                source === s ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3 ml-auto text-xs text-slate-500">
          {(
            [
              { label: "개인", color: "bg-blue-400" },
              { label: "기관", color: "bg-amber-400" },
              { label: "외국인", color: "bg-emerald-400" },
            ] as const
          ).map(({ label, color }) => (
            <span key={label} className="flex items-center gap-1">
              <span className={`w-2 h-2 rounded-full ${color} inline-block`} />
              {label}
            </span>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {loading && data === null && (
          <p className="text-slate-500 text-sm py-8 text-center">불러오는 중…</p>
        )}
        {!loading && data !== null && data.length === 0 && (
          <p className="text-slate-500 text-sm py-8 text-center">데이터가 없습니다.</p>
        )}
        {data !== null && data.length > 0 && (
          <>
            <InvestorFlowChart data={data} />
            <InvestorFlowTable data={data} />
            <div className="flex justify-center pb-2">
              <button
                onClick={() => setLimit((l) => l + 60)}
                disabled={loading}
                className={cx.btnSecondary}
              >
                {loading ? "불러오는 중…" : "60일 더 보기"}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
