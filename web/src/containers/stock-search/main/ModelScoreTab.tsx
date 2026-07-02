"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cx } from "@/utils/cx";

// ── 타입 ──────────────────────────────────────────────────────────────────────

/** 활성 모델 요약(점수 선택용). */
interface ModelSummary {
  id: string;
  name: string;
  version: string;
  outputType: string;
  scoreExchanges: string[];
  scorePriceType: string;
  status: string;
  scoreCursor: string | null;
}

/** 한 거래일의 모델 점수. score 없음 = 미채점. */
interface ModelScoreBar {
  ticker: string;
  tradeDate: string;
  score: number | null;
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

const THRESHOLD = 0.5;   // 점수 임계선(보정확률)
const CHART_H   = 220;
const PAD_L     = 36;    // y축 라벨 폭
const PAD_R     = 8;
const PAD_T     = 8;
const PAD_B     = 22;    // x축 라벨 높이
const STEP      = 9;     // 일별 x 간격(px)

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

function scoreColor(v: number): string {
  return v >= THRESHOLD ? "text-red-400" : "text-blue-400";
}

// ── Canvas 점수 라인 차트 ───────────────────────────────────────────────────────

function ScoreCanvas({ data }: { data: ModelScoreBar[] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef    = useRef<HTMLCanvasElement>(null);
  const [hover, setHover] = useState<{ x: number; bar: ModelScoreBar } | null>(null);

  const innerW = Math.max(1, data.length) * STEP;
  const W      = PAD_L + innerW + PAD_R;
  const plotH  = CHART_H - PAD_T - PAD_B;

  const xAt = useCallback((i: number) => PAD_L + i * STEP + STEP / 2, []);
  const yAt = useCallback((v: number) => PAD_T + (1 - v) * plotH, [plotH]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || data.length === 0) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width        = W * dpr;
    canvas.height       = CHART_H * dpr;
    canvas.style.width  = `${W}px`;
    canvas.style.height = `${CHART_H}px`;

    const ctx = canvas.getContext("2d")!;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, W, CHART_H);

    // y축 보조선 + 라벨(0 / 0.5 / 1)
    [0, 0.25, 0.5, 0.75, 1].forEach(v => {
      const y = yAt(v);
      ctx.strokeStyle = v === THRESHOLD ? "rgba(248,113,113,0.35)" : "rgba(255,255,255,0.06)";
      ctx.lineWidth   = 1;
      ctx.setLineDash(v === THRESHOLD ? [4, 3] : []);
      ctx.beginPath();
      ctx.moveTo(PAD_L, y);
      ctx.lineTo(W - PAD_R, y);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle    = "#64748b";
      ctx.font         = "9px monospace";
      ctx.textAlign    = "right";
      ctx.textBaseline = "middle";
      ctx.fillText(v.toFixed(2), PAD_L - 4, y);
    });

    // 점수 라인 — 미채점(null)은 끊어서 그림
    ctx.lineWidth   = 1.5;
    ctx.strokeStyle = "#818cf8";
    let started = false;
    data.forEach((d, i) => {
      if (d.score == null) { started = false; return; }
      const x = xAt(i);
      const y = yAt(d.score);
      if (!started) { ctx.beginPath(); ctx.moveTo(x, y); started = true; }
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // 점
    data.forEach((d, i) => {
      if (d.score == null) return;
      ctx.beginPath();
      ctx.arc(xAt(i), yAt(d.score), 2, 0, Math.PI * 2);
      ctx.fillStyle = d.score >= THRESHOLD ? "#f87171" : "#60a5fa";
      ctx.fill();
    });

    // x축 날짜 라벨 — 대략 6개
    const labelEvery = Math.max(1, Math.ceil(data.length / 6));
    ctx.fillStyle    = "#94a3b8";
    ctx.font         = "9px monospace";
    ctx.textAlign    = "center";
    ctx.textBaseline = "alphabetic";
    data.forEach((d, i) => {
      if (i % labelEvery === 0) ctx.fillText(d.tradeDate.slice(5), xAt(i), CHART_H - 6);
    });
  }, [data, W, plotH, xAt, yAt]);

  const onMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const px   = e.clientX - rect.left + (containerRef.current?.scrollLeft ?? 0);
    const i    = Math.round((px - PAD_L - STEP / 2) / STEP);
    if (i < 0 || i >= data.length) { setHover(null); return; }
    setHover({ x: xAt(i), bar: data[i] });
  };

  return (
    <div className="rounded bg-white/[0.02] border border-white/5 overflow-hidden relative">
      <div
        ref={containerRef}
        className="overflow-x-auto overflow-y-hidden"
        style={{ height: CHART_H }}
        onMouseMove={onMove}
        onMouseLeave={() => setHover(null)}
      >
        <canvas ref={canvasRef} style={{ display: "block" }} />
        {hover && (
          <div
            className="pointer-events-none absolute top-2 -translate-x-1/2 rounded bg-slate-900/95 border border-white/15 px-2 py-1 text-xs whitespace-nowrap"
            style={{ left: hover.x - (containerRef.current?.scrollLeft ?? 0) }}
          >
            <span className="text-slate-400 font-mono">{hover.bar.tradeDate}</span>{" "}
            {hover.bar.score == null
              ? <span className="text-slate-500">미채점</span>
              : <span className={`font-mono ${scoreColor(hover.bar.score)}`}>{hover.bar.score.toFixed(4)}</span>}
          </div>
        )}
      </div>
    </div>
  );
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

const PAGE_SIZE = 40; // 한 페이지 = 40거래일

export default function ModelScoreTab({ code }: { code: string }) {
  const [models,   setModels]   = useState<ModelSummary[]>([]);
  const [modelId,  setModelId]  = useState("");
  const [data,     setData]     = useState<ModelScoreBar[]>([]);
  const [loading,  setLoading]  = useState(false);
  const [period,   setPeriod]   = useState<Period>("6M");
  const [page,     setPage]     = useState(0);

  // 활성 모델 목록
  useEffect(() => {
    fetch("/api/stocks/models")
      .then(r => (r.ok ? r.json() : []))
      .then((rows: ModelSummary[]) => {
        const active = rows.filter(m => m.status === "ACTIVE");
        const list   = active.length > 0 ? active : rows;
        setModels(list);
        setModelId(prev => prev || (list[0]?.id ?? ""));
      })
      .catch(() => setModels([]));
  }, []);

  const fetchScores = useCallback(async (from: string, to: string) => {
    if (!modelId) return;
    setLoading(true);
    try {
      const res = await fetch(
        `/api/stocks/${code}/scores?modelId=${encodeURIComponent(modelId)}&from=${from}&to=${to}`
      );
      const rows: ModelScoreBar[] = res.ok ? await res.json() : [];
      setData(rows);
      setPage(0);
    } finally {
      setLoading(false);
    }
  }, [code, modelId]);

  // 종목 또는 모델 변경 시 재조회
  useEffect(() => {
    if (!modelId) { setData([]); return; }
    fetchScores(getFromDate(period), toDateStr(new Date()));
  }, [code, modelId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePeriod = (p: Period) => {
    setPeriod(p);
    fetchScores(getFromDate(p), toDateStr(new Date()));
  };

  const selectedModel = models.find(m => m.id === modelId) ?? null;

  // 최신순 페이지 슬라이스 → 차트는 시간순
  const reversed   = [...data].reverse();
  const totalPages = Math.max(1, Math.ceil(reversed.length / PAGE_SIZE));
  const pagedRows  = reversed.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const chartData  = [...pagedRows].reverse();

  const scored = data.filter(d => d.score != null);

  return (
    <div className="flex flex-col h-full overflow-hidden">

      {/* ── 툴바 ─────────────────────────────────────────────────────────── */}
      <div className="flex-shrink-0 border-b border-white/10 px-4 py-2">
        <div className="flex items-center gap-2 flex-wrap">
          {/* 모델 선택 */}
          <select
            value={modelId}
            onChange={e => setModelId(e.target.value)}
            disabled={models.length === 0}
            className="bg-slate-800 border border-white/15 rounded px-2 py-1.5 text-xs text-white max-w-[200px] truncate disabled:opacity-40"
          >
            {models.length === 0 && <option value="">활성 모델 없음</option>}
            {models.map(m => (
              <option key={m.id} value={m.id}>{m.name} v{m.version}</option>
            ))}
          </select>

          {/* 기간 버튼 */}
          <div className="flex rounded border border-white/10 overflow-hidden text-xs">
            {PERIODS.map(({ label, value }) => (
              <button
                key={value}
                onClick={() => handlePeriod(value)}
                disabled={!modelId}
                className={`px-2.5 py-1.5 transition disabled:opacity-30 ${
                  period === value ? "bg-white/10 text-white" : "text-slate-500 hover:text-slate-300"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* 임계선 안내 */}
          <span className="ml-auto text-xs text-slate-500 flex items-center gap-1">
            <span className="w-3 border-t border-dashed border-red-400/60" />
            임계 {THRESHOLD}
          </span>
        </div>
      </div>

      {/* ── 콘텐츠 ────────────────────────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {models.length === 0 && (
          <p className="text-slate-500 text-sm py-8 text-center">활성화된 모델이 없습니다.</p>
        )}
        {models.length > 0 && loading && (
          <p className="text-slate-500 text-sm py-8 text-center">불러오는 중…</p>
        )}
        {models.length > 0 && !loading && data.length === 0 && (
          <p className="text-slate-500 text-sm py-8 text-center">
            해당 기간 채점된 점수가 없습니다.
          </p>
        )}

        {models.length > 0 && !loading && data.length > 0 && (
          <>
            {/* 모델 메타 */}
            {selectedModel && (
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                <span>{selectedModel.outputType}</span>
                <span>거래소 {(selectedModel.scoreExchanges ?? []).join(", ") || "—"}</span>
                <span>주가 {selectedModel.scorePriceType}</span>
                {scored.length > 0 && (
                  <span>
                    최근{" "}
                    <span className={`font-mono ${scoreColor(scored[scored.length - 1].score!)}`}>
                      {scored[scored.length - 1].score!.toFixed(4)}
                    </span>
                  </span>
                )}
              </div>
            )}

            {/* 차트 — 현재 페이지만 */}
            <ScoreCanvas data={chartData} />

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
                  <th className="text-xs text-slate-400 px-2 sm:px-4 py-2 font-medium text-left">날짜</th>
                  <th className="text-xs text-slate-400 px-2 sm:px-4 py-2 font-medium text-right">점수</th>
                </tr>
              </thead>
              <tbody className={cx.table.body}>
                {pagedRows.map(d => (
                  <tr key={d.tradeDate} className={cx.table.tr}>
                    <td className="px-2 sm:px-4 py-2 font-mono text-xs text-slate-400">{d.tradeDate}</td>
                    <td className={`px-2 sm:px-4 py-2 text-right font-mono text-xs ${
                      d.score == null ? "text-slate-600" : scoreColor(d.score)
                    }`}>
                      {d.score == null ? "—" : d.score.toFixed(4)}
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
