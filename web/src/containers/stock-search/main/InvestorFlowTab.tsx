"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cx } from "@/utils/cx";

// ── 타입 ──────────────────────────────────────────────────────────────────────

interface InvestorFlowBar {
  date: string;
  individualNet: number;
  institutionNet: number;
  foreignNet: number;
}

type Period = "1M" | "3M" | "6M" | "1Y" | "ALL";

// ── 상수 ──────────────────────────────────────────────────────────────────────

const PERIODS: { label: string; value: Period }[] = [
  { label: "1개월", value: "1M" },
  { label: "3개월", value: "3M" },
  { label: "6개월", value: "6M" },
  { label: "1년",   value: "1Y" },
  { label: "전체",  value: "ALL" },
];

const COLORS = ["#60a5fa", "#f59e0b", "#34d399"]; // 개인·기관·외국인

const BAR_W   = 5;
const BAR_GAP = 1;
const GRP_GAP = 6;
const GRP_W   = 3 * BAR_W + 2 * BAR_GAP + GRP_GAP; // 23px
const CHART_H = 200;

// ── 유틸 ──────────────────────────────────────────────────────────────────────

function toDateStr(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function getFromDate(period: Period): string {
  const d = new Date();
  switch (period) {
    case "1M":  d.setMonth(d.getMonth() - 1);       break;
    case "3M":  d.setMonth(d.getMonth() - 3);       break;
    case "6M":  d.setMonth(d.getMonth() - 6);       break;
    case "1Y":  d.setFullYear(d.getFullYear() - 1); break;
    case "ALL": return "2010-01-01";
  }
  return toDateStr(d);
}

function fmtNet(v: number): string {
  if (v === 0) return "0";
  const sign = v > 0 ? "+" : "-";
  const abs  = Math.abs(v);
  if (abs >= 100_000_000) {
    const n = abs / 100_000_000;
    return sign + (n === Math.floor(n) ? n.toFixed(0) : n.toFixed(1)) + "억";
  }
  if (abs >= 10_000) {
    const n = abs / 10_000;
    return sign + (n === Math.floor(n) ? n.toFixed(0) : n.toFixed(1)) + "만";
  }
  if (abs >= 1_000) {
    const n = abs / 1_000;
    return sign + (n === Math.floor(n) ? n.toFixed(0) : n.toFixed(1)) + "천";
  }
  return (v > 0 ? "+" : "") + v.toLocaleString();
}

function netColor(v: number) {
  if (v > 0) return "text-red-400";
  if (v < 0) return "text-blue-400";
  return "text-slate-500";
}

// ── Canvas 차트 ───────────────────────────────────────────────────────────────

function InvestorCanvas({ data }: { data: InvestorFlowBar[] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef    = useRef<HTMLCanvasElement>(null);
  const dragRef      = useRef<{ startX: number; startScrollLeft: number } | null>(null);

  // 그리기
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || data.length === 0) return;

    const dpr    = window.devicePixelRatio || 1;
    const W      = data.length * GRP_W;
    const H      = CHART_H;
    const labelH = 20;
    const chartH = H - labelH;
    const axisY  = chartH / 2;
    const maxBarH = axisY - 4;

    canvas.width        = W * dpr;
    canvas.height       = H * dpr;
    canvas.style.width  = `${W}px`;
    canvas.style.height = `${H}px`;

    const ctx = canvas.getContext("2d")!;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, W, H);

    const maxAbs = Math.max(1, ...data.flatMap(d => [
      Math.abs(d.individualNet),
      Math.abs(d.institutionNet),
      Math.abs(d.foreignNet),
    ]));

    // ── 배경: 5거래일(주) 단위 교대 음영 ─────────────────────────────────
    data.forEach((_, i) => {
      const week = Math.floor(i / 5);
      if (week % 2 === 0) {
        ctx.fillStyle = "rgba(255,255,255,0.025)";
        ctx.fillRect(i * GRP_W, 0, GRP_W, chartH);
      }
    });

    // ── 수평 보조선 (25% / 50% / 75%) ────────────────────────────────────
    [0.25, 0.5, 0.75].forEach(r => {
      const y = r * chartH;
      ctx.strokeStyle = "rgba(255,255,255,0.05)";
      ctx.lineWidth   = 1;
      ctx.setLineDash([2, 4]);
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(W, y);
      ctx.stroke();
    });
    ctx.setLineDash([]);

    // ── 제로 축 ───────────────────────────────────────────────────────────
    ctx.strokeStyle = "rgba(255,255,255,0.18)";
    ctx.lineWidth   = 1;
    ctx.beginPath();
    ctx.moveTo(0, axisY);
    ctx.lineTo(W, axisY);
    ctx.stroke();

    // ── 바 + 일 구분선 + 날짜 레이블 ─────────────────────────────────────
    data.forEach((d, i) => {
      const x    = i * GRP_W;
      const nets = [d.individualNet, d.institutionNet, d.foreignNet];

      // 일 구분 세로선
      ctx.strokeStyle = "rgba(255,255,255,0.07)";
      ctx.lineWidth   = 1;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, chartH);
      ctx.stroke();

      // 바
      nets.forEach((v, j) => {
        const bh = Math.max(2, (Math.abs(v) / maxAbs) * maxBarH);
        const by = v >= 0 ? axisY - bh : axisY;
        ctx.fillStyle = COLORS[j] + "CC";
        ctx.fillRect(x + j * (BAR_W + BAR_GAP), by, BAR_W, bh);
      });

      // 날짜 레이블 — 5일마다
      if (i % 5 === 0) {
        ctx.fillStyle    = "#94a3b8";
        ctx.font         = "9px monospace";
        ctx.textAlign    = "left";
        ctx.textBaseline = "alphabetic";
        ctx.fillText(d.date.slice(5), x + 2, H - 3);
      }
    });
  }, [data]);

  // 데이터 바뀌면 오른쪽 끝(최신)으로 스크롤
  useEffect(() => {
    if (data.length === 0) return;
    requestAnimationFrame(() => {
      if (containerRef.current)
        containerRef.current.scrollLeft = containerRef.current.scrollWidth;
    });
  }, [data]);

  // 드래그 — window에 달아야 빠른 이동에서 끊기지 않음
  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!dragRef.current || !containerRef.current) return;
      containerRef.current.scrollLeft =
        dragRef.current.startScrollLeft - (e.clientX - dragRef.current.startX);
    };
    const onUp = () => {
      if (!dragRef.current) return;
      dragRef.current = null;
      if (containerRef.current) containerRef.current.style.cursor = "grab";
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup",   onUp);
    return () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup",   onUp);
    };
  }, []);

  const onMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    const container = containerRef.current;
    if (!container) return;
    dragRef.current = { startX: e.clientX, startScrollLeft: container.scrollLeft };
    container.style.cursor = "grabbing";
    e.preventDefault();
  };

  return (
    <div className="rounded bg-white/[0.02] border border-white/5 overflow-hidden">
      <div
        ref={containerRef}
        className="overflow-x-auto overflow-y-hidden select-none"
        style={{ height: CHART_H, cursor: "grab" }}
        onMouseDown={onMouseDown}
      >
        <canvas ref={canvasRef} style={{ display: "block" }} />
      </div>
    </div>
  );
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

const PAGE_SIZE = 30; // 한 페이지 = 30거래일

export default function InvestorFlowTab({ code }: { code: string }) {
  const [data,       setData]       = useState<InvestorFlowBar[]>([]);
  const [loading,    setLoading]    = useState(false);
  const [period,     setPeriod]     = useState<Period>("6M");
  const [useCustom,  setUseCustom]  = useState(false);
  const [customFrom, setCustomFrom] = useState("");
  const [customTo,   setCustomTo]   = useState(toDateStr(new Date()));
  const [page,       setPage]       = useState(0);

  const fetchData = useCallback(async (from: string, to: string) => {
    setLoading(true);
    try {
      const res  = await fetch(`/api/stocks/${code}/investor-flow?from=${from}&to=${to}`);
      const rows: InvestorFlowBar[] = res.ok ? await res.json() : [];
      setData(rows);
      setPage(0);
    } finally {
      setLoading(false);
    }
  }, [code]);

  // 종목 변경 시 초기화
  useEffect(() => {
    setPeriod("6M");
    setUseCustom(false);
    setData([]);
    setPage(0);
    fetchData(getFromDate("6M"), toDateStr(new Date()));
  }, [code]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePeriod = (p: Period) => {
    setPeriod(p);
    setUseCustom(false);
    fetchData(getFromDate(p), toDateStr(new Date()));
  };

  const handleCustomApply = () => {
    if (!customFrom || !customTo) return;
    setUseCustom(true);
    fetchData(customFrom, customTo);
  };

  // 최신순 정렬 후 페이지 슬라이스
  const reversedData = [...data].reverse();
  const totalPages   = Math.max(1, Math.ceil(reversedData.length / PAGE_SIZE));
  const pagedRows    = reversedData.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  // 차트는 시간순(오래된→최신)으로 표시
  const chartData    = [...pagedRows].reverse();

  return (
    <div className="flex flex-col h-full overflow-hidden">

      {/* ── 툴바 ─────────────────────────────────────────────────────────── */}
      <div className="flex-shrink-0 border-b border-white/10 px-4 py-2">
        <div className="flex items-center gap-2 flex-wrap">
          {/* 기간 버튼 */}
          <div className="flex rounded border border-white/10 overflow-hidden text-xs">
            {PERIODS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => handlePeriod(value)}
                className={`px-2.5 py-1.5 transition ${
                  !useCustom && period === value
                    ? "bg-white/10 text-white"
                    : "text-slate-500 hover:text-slate-300"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* 직접 날짜 입력 */}
          <div className="flex items-center gap-1 text-xs">
            <input
              type="date"
              value={customFrom}
              onChange={e => setCustomFrom(e.target.value)}
              className="bg-white/5 border border-white/10 rounded px-2 py-1 text-xs text-white w-32 focus:outline-none focus:ring-1 focus:ring-indigo-400/50"
            />
            <span className="text-slate-500">~</span>
            <input
              type="date"
              value={customTo}
              onChange={e => setCustomTo(e.target.value)}
              className="bg-white/5 border border-white/10 rounded px-2 py-1 text-xs text-white w-32 focus:outline-none focus:ring-1 focus:ring-indigo-400/50"
            />
            <button onClick={handleCustomApply} className={cx.btnSecondary}>
              조회
            </button>
          </div>

          {/* 범례 */}
          <div className="flex items-center gap-3 ml-auto text-xs text-slate-500">
            {[
              { label: "개인",   color: "bg-blue-400"    },
              { label: "기관",   color: "bg-amber-400"   },
              { label: "외국인", color: "bg-emerald-400" },
            ].map(({ label, color }) => (
              <span key={label} className="flex items-center gap-1">
                <span className={`w-2 h-2 rounded-full ${color}`} />
                {label}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* ── 콘텐츠 ────────────────────────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {loading && (
          <p className="text-slate-500 text-sm py-8 text-center">불러오는 중…</p>
        )}
        {!loading && data.length === 0 && (
          <p className="text-slate-500 text-sm py-8 text-center">데이터가 없습니다.</p>
        )}

        {!loading && data.length > 0 && (
          <>
            {/* 차트 — 현재 페이지 30일만 표시 */}
            <InvestorCanvas data={chartData} />

            {/* 페이지네이션 */}
            <div className="flex items-center justify-between">
              <span className="text-xs text-slate-500">
                총 {data.length}일 · 페이지 {page + 1} / {totalPages}
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={cx.btnSecondary + " disabled:opacity-30"}
                >
                  이전
                </button>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className={cx.btnSecondary + " disabled:opacity-30"}
                >
                  다음
                </button>
              </div>
            </div>

            {/* 테이블 */}
            <table className={cx.table.root + " w-full"}>
              <thead className={cx.table.head}>
                <tr>
                  <th className="text-xs text-slate-400   px-2 sm:px-4 py-2 font-medium text-left">날짜</th>
                  <th className="text-xs text-blue-400    px-2 sm:px-4 py-2 font-medium text-right">개인</th>
                  <th className="text-xs text-amber-400   px-2 sm:px-4 py-2 font-medium text-right">기관</th>
                  <th className="text-xs text-emerald-400 px-2 sm:px-4 py-2 font-medium text-right">외국인</th>
                </tr>
              </thead>
              <tbody className={cx.table.body}>
                {pagedRows.map(d => (
                  <tr key={d.date} className={cx.table.tr}>
                    <td className="px-2 sm:px-4 py-2 font-mono text-xs text-slate-400">{d.date}</td>
                    <td className={`px-2 sm:px-4 py-2 text-right font-mono text-xs ${netColor(d.individualNet)}`}>
                      {fmtNet(d.individualNet)}
                    </td>
                    <td className={`px-2 sm:px-4 py-2 text-right font-mono text-xs ${netColor(d.institutionNet)}`}>
                      {fmtNet(d.institutionNet)}
                    </td>
                    <td className={`px-2 sm:px-4 py-2 text-right font-mono text-xs ${netColor(d.foreignNet)}`}>
                      {fmtNet(d.foreignNet)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </div>
    </div>
  );
}
