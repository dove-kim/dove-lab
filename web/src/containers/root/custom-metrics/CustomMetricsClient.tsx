"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { cx } from "@/utils/cx";

/** 커스텀 지표 정의·상태·계산 진행 정보. 백엔드 CustomMetricResponse 미러. */
interface CustomMetric {
  id: number;
  name: string;
  description: string | null;
  shape: string;
  spec: string;
  priceType: string;
  active: boolean;
  lastComputedDate: string | null;
  lastError: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

/** 미리보기 시계열 한 점. 백엔드 MetricPoint 미러. */
interface MetricPoint {
  date: string;
  value: number | null;
}

const SHAPES = [
  { value: "SERIES", label: "SERIES" },
  { value: "PANEL", label: "PANEL" },
];
const PRICE_TYPES = [
  { value: "RAW", label: "원주가 (RAW)" },
  { value: "ADJUSTED", label: "수정주가 (ADJUSTED)" },
];

const SPEC_PLACEHOLDER = '{"root":{"op":"agg","agg":"RATIO_POS","colA":"RET_1D","universeFilterId":1}}';

const ERROR_LABEL: Record<string, string> = {
  CUSTOM_METRIC_NOT_FOUND: "지표를 찾을 수 없습니다",
  UNPROCESSABLE_ENTITY: "스펙 검증에 실패했습니다",
};

async function readError(res: Response): Promise<string> {
  try {
    const body = await res.json();
    const code = body?.detail ?? body?.error ?? body?.message;
    if (typeof code === "string") return ERROR_LABEL[code] ?? code;
  } catch {
    /* ignore */
  }
  return `요청 실패 (${res.status})`;
}

function formatDt(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "2-digit", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit",
  });
}

function statusChip(active: boolean) {
  const cfg = active
    ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/30"
    : "bg-slate-500/20 text-slate-400 border-slate-500/30";
  return <span className={`px-2 py-0.5 rounded text-xs border ${cfg}`}>{active ? "활성" : "비활성"}</span>;
}

/** 화면 중앙 고정 크기 모달 셸. */
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={onClose}>
      <div
        className="w-full max-w-lg max-h-[85vh] overflow-y-auto rounded-2xl border border-white/10 bg-slate-900 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-white/10 px-5 py-4">
          <h3 className="text-base font-semibold text-white">{title}</h3>
          <button
            onClick={onClose}
            aria-label="닫기"
            className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-400 hover:bg-white/8 hover:text-white transition"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}

export default function CustomMetricsClient() {
  const [metrics, setMetrics] = useState<CustomMetric[]>([]);
  const [loading, setLoading] = useState(true);

  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState<CustomMetric | null>(null);
  const [recomputeTarget, setRecomputeTarget] = useState<CustomMetric | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CustomMetric | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const fetchMetrics = useCallback(async () => {
    const res = await fetch("/api/admin/ops/custom-metrics", { cache: "no-store" });
    if (res.ok) setMetrics((await res.json()) ?? []);
    setLoading(false);
  }, []);

  useEffect(() => { fetchMetrics(); }, [fetchMetrics]);

  async function toggleActive(metric: CustomMetric) {
    setBusyId(metric.id);
    try {
      const action = metric.active ? "deactivate" : "activate";
      const res = await fetch(`/api/admin/ops/custom-metrics/${metric.id}/${action}`, { method: "POST" });
      if (!res.ok) {
        alert(await readError(res));
        return;
      }
      await fetchMetrics();
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="flex min-h-full flex-col gap-4 p-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-lg font-semibold text-white">커스텀 지표 관리</h1>
        <button onClick={() => setShowForm(true)} className={cx.btnPrimary}>
          지표 등록
        </button>
      </div>

      {/* 지표 목록 */}
      <div className="overflow-x-auto rounded-lg border border-white/10">
        <table className={cx.table.root + " min-w-[1000px]"}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>이름</th>
              <th className={cx.table.th}>모양</th>
              <th className={cx.table.th}>주가유형</th>
              <th className={cx.table.th}>상태</th>
              <th className={cx.table.th}>마지막 계산일</th>
              <th className={cx.table.th}>마지막 오류</th>
              <th className={cx.table.th}>생성자 / 생성일</th>
              <th className={cx.table.th}></th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {loading && (
              <tr><td colSpan={8} className="py-10 text-center text-slate-400">불러오는 중...</td></tr>
            )}
            {!loading && metrics.length === 0 && (
              <tr><td colSpan={8} className="py-10 text-center text-slate-400">등록된 지표가 없습니다.</td></tr>
            )}
            {metrics.map((m) => (
              <tr key={m.id} className={cx.table.tr}>
                <td className={cx.table.td + " font-medium text-white whitespace-nowrap"}>
                  {m.name}
                  {m.description && <span className="block text-xs font-normal text-slate-500">{m.description}</span>}
                </td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>{m.shape}</td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>{m.priceType}</td>
                <td className={cx.table.td + " whitespace-nowrap"}>{statusChip(m.active)}</td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>{m.lastComputedDate ?? "미계산"}</td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>
                  {m.lastError ? (
                    <span
                      title={m.lastError}
                      className="inline-block max-w-[220px] truncate rounded border border-rose-500/30 bg-rose-500/15 px-2 py-0.5 text-rose-300"
                    >
                      {m.lastError}
                    </span>
                  ) : (
                    <span className="text-slate-400">—</span>
                  )}
                </td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>
                  {m.createdBy}
                  <span className="block text-slate-500">{formatDt(m.createdAt)}</span>
                </td>
                <td className={cx.table.td}>
                  <div className="flex flex-wrap items-center gap-1.5">
                    <button
                      onClick={() => toggleActive(m)}
                      disabled={busyId === m.id}
                      className={(m.active ? cx.btnToggleOff : cx.btnToggleOn) + " !py-1.5 !px-3 text-xs whitespace-nowrap"}
                    >
                      {busyId === m.id ? "..." : m.active ? "비활성화" : "활성화"}
                    </button>
                    <button
                      onClick={() => setRecomputeTarget(m)}
                      className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 transition whitespace-nowrap"
                    >
                      재계산
                    </button>
                    <button
                      onClick={() => setEditTarget(m)}
                      className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 transition whitespace-nowrap"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => setDeleteTarget(m)}
                      className="rounded-lg border border-rose-500/30 px-3 py-1.5 text-xs text-rose-300 hover:bg-rose-500/10 transition whitespace-nowrap"
                    >
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showForm && (
        <MetricFormModal
          onClose={() => setShowForm(false)}
          onDone={() => { setShowForm(false); fetchMetrics(); }}
        />
      )}
      {editTarget && (
        <MetricFormModal
          metric={editTarget}
          onClose={() => setEditTarget(null)}
          onDone={() => { setEditTarget(null); fetchMetrics(); }}
        />
      )}
      {recomputeTarget && (
        <RecomputeModal
          metric={recomputeTarget}
          onClose={() => setRecomputeTarget(null)}
          onDone={() => { setRecomputeTarget(null); fetchMetrics(); }}
        />
      )}
      {deleteTarget && (
        <DeleteMetricModal
          metric={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDone={() => { setDeleteTarget(null); fetchMetrics(); }}
        />
      )}
    </div>
  );
}

/** 미리보기 시계열의 순수 SVG 꺾은선. value가 null인 점은 건너뛴다. */
function PreviewChart({ points }: { points: MetricPoint[] }) {
  const valued = points.filter((p) => p.value !== null) as { date: string; value: number }[];
  if (valued.length < 2) return null;

  const w = 480;
  const h = 100;
  const pad = 4;
  const values = valued.map((p) => p.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const stepX = (w - pad * 2) / (valued.length - 1);

  const coords = valued.map((p, i) => {
    const x = pad + i * stepX;
    const y = pad + (1 - (p.value - min) / span) * (h - pad * 2);
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="w-full rounded-lg border border-white/10 bg-white/5" preserveAspectRatio="none">
      <polyline points={coords.join(" ")} fill="none" stroke="#818cf8" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}

/* ── 계산식 DSL (백엔드 계약) ─────────────────────────────────────────── */

/** 이항연산 op 키. */
type BinaryOp = "gt" | "lt" | "gte" | "lte" | "add" | "sub" | "mul" | "div" | "and" | "or";

/** 계산식 트리의 한 노드. op 필드로 종류를 구분한다. */
type MetricNode =
  | { op: "const"; value: number }
  | { op: "ref"; name: string }
  | { op: "agg"; agg: "MEAN" | "RATIO_GT" | "RATIO_POS"; colA: string; colB?: string; universeFilterId: number }
  | { op: "roll_mean"; input: MetricNode; window: number; minPeriods: number }
  | { op: "ema"; input: MetricNode; window: number }
  | { op: "cumprod1p"; input: MetricNode }
  | { op: "lag"; input: MetricNode; periods: number }
  | { op: BinaryOp; left: MetricNode; right: MetricNode };

/** 중간값·root 로 이뤄진 계산식 스펙 모델. */
type MetricSpecModel = { lets: Record<string, MetricNode>; root: MetricNode };

/** UI 빌더의 중간값 한 항목(이름·노드). */
type LetEntry = { name: string; node: MetricNode };

/** agg 집계에서 유니버스로 쓰는 시스템 필터. 백엔드 StockFilterResponse 미러. */
interface StockFilter {
  id: number;
  name: string;
}

const METRIC_COLUMNS = [
  "RET_1D", "RET_5D", "RET_10D", "OPEN", "HIGH", "LOW", "CLOSE", "VOLUME", "TURNOVER",
  "SMA_5", "SMA_10", "SMA_20", "SMA_50", "SMA_60", "SMA_120", "SMA_200",
  "EMA_20", "EMA_60", "EMA_120", "EMA_200", "RSI_14",
  "HIGH_52W_RATIO", "HIGH_20D_RATIO", "VOLATILITY_20D", "VOLATILITY_5D",
];

const BINARY_OPS: { value: BinaryOp; label: string }[] = [
  { value: "gt", label: ">" },
  { value: "gte", label: "≥" },
  { value: "lt", label: "<" },
  { value: "lte", label: "≤" },
  { value: "add", label: "+" },
  { value: "sub", label: "−" },
  { value: "mul", label: "×" },
  { value: "div", label: "÷" },
  { value: "and", label: "AND" },
  { value: "or", label: "OR" },
];
const BINARY_OP_VALUES = BINARY_OPS.map((o) => o.value);

const AGG_OPTIONS: { value: "MEAN" | "RATIO_GT" | "RATIO_POS"; label: string }[] = [
  { value: "MEAN", label: "MEAN (colA 평균)" },
  { value: "RATIO_GT", label: "RATIO_GT (colA>colB 비율)" },
  { value: "RATIO_POS", label: "RATIO_POS (colA>0 비율)" },
];

/** op 문자열이 이항연산인지 판별하는 타입 가드. */
function isBinaryNode(node: MetricNode): node is { op: BinaryOp; left: MetricNode; right: MetricNode } {
  return BINARY_OP_VALUES.includes(node.op as BinaryOp);
}

/** 등록 모드 기본 스펙(상승비율 예시). */
function defaultSpecModel(): MetricSpecModel {
  return { lets: {}, root: { op: "agg", agg: "RATIO_POS", colA: "RET_1D", universeFilterId: 0 } };
}

/** op 선택 시 그 op 의 기본 노드를 만든다. */
function defaultNode(op: string, firstUniverseId: number): MetricNode {
  switch (op) {
    case "ref": return { op: "ref", name: "" };
    case "agg": return { op: "agg", agg: "MEAN", colA: "RET_1D", universeFilterId: firstUniverseId || 0 };
    case "roll_mean": return { op: "roll_mean", input: { op: "const", value: 0 }, window: 20, minPeriods: 15 };
    case "ema": return { op: "ema", input: { op: "const", value: 0 }, window: 20 };
    case "cumprod1p": return { op: "cumprod1p", input: { op: "const", value: 0 } };
    case "lag": return { op: "lag", input: { op: "const", value: 0 }, periods: 1 };
    case "const": return { op: "const", value: 0 };
    default: return { op: op as BinaryOp, left: { op: "const", value: 0 }, right: { op: "const", value: 0 } };
  }
}

/** 노드를 백엔드 계약 JSON 으로 직렬화한다(유효 필드만). */
function nodeToJson(node: MetricNode): Record<string, unknown> {
  switch (node.op) {
    case "const": return { op: "const", value: node.value };
    case "ref": return { op: "ref", name: node.name };
    case "agg": {
      const o: Record<string, unknown> = { op: "agg", agg: node.agg, colA: node.colA };
      if (node.agg === "RATIO_GT") o.colB = node.colB ?? "";
      o.universeFilterId = node.universeFilterId;
      return o;
    }
    case "roll_mean": return { op: "roll_mean", input: nodeToJson(node.input), window: node.window, minPeriods: node.minPeriods };
    case "ema": return { op: "ema", input: nodeToJson(node.input), window: node.window };
    case "cumprod1p": return { op: "cumprod1p", input: nodeToJson(node.input) };
    case "lag": return { op: "lag", input: nodeToJson(node.input), periods: node.periods };
    default: return { op: node.op, left: nodeToJson(node.left), right: nodeToJson(node.right) };
  }
}

/** root·중간값 배열을 spec JSON 문자열로 직렬화한다. */
function buildSpecString(rootNode: MetricNode, lets: LetEntry[]): string {
  const letObj: Record<string, unknown> = {};
  for (const l of lets) {
    const key = l.name.trim();
    if (key) letObj[key] = nodeToJson(l.node);
  }
  const obj: Record<string, unknown> = {};
  if (Object.keys(letObj).length > 0) obj.lets = letObj;
  obj.root = nodeToJson(rootNode);
  return JSON.stringify(obj, null, 2);
}

/**
 * raw JSON 노드를 관대하게 파싱한다(op·필수필드만 검증, 모양/타입은 백엔드가 검증).
 * 표현 불가면 null.
 */
function parseNode(raw: unknown): MetricNode | null {
  if (!raw || typeof raw !== "object") return null;
  const r = raw as Record<string, unknown>;
  const op = r.op;
  if (typeof op !== "string") return null;
  switch (op) {
    case "const":
      return typeof r.value === "number" ? { op: "const", value: r.value } : null;
    case "ref":
      return typeof r.name === "string" ? { op: "ref", name: r.name } : null;
    case "agg": {
      if (r.agg !== "MEAN" && r.agg !== "RATIO_GT" && r.agg !== "RATIO_POS") return null;
      if (typeof r.colA !== "string") return null;
      if (typeof r.universeFilterId !== "number") return null;
      const node: MetricNode = { op: "agg", agg: r.agg, colA: r.colA, universeFilterId: r.universeFilterId };
      if (typeof r.colB === "string") node.colB = r.colB;
      return node;
    }
    case "roll_mean": {
      const input = parseNode(r.input);
      if (!input || typeof r.window !== "number" || typeof r.minPeriods !== "number") return null;
      return { op: "roll_mean", input, window: r.window, minPeriods: r.minPeriods };
    }
    case "ema": {
      const input = parseNode(r.input);
      if (!input || typeof r.window !== "number") return null;
      return { op: "ema", input, window: r.window };
    }
    case "cumprod1p": {
      const input = parseNode(r.input);
      return input ? { op: "cumprod1p", input } : null;
    }
    case "lag": {
      const input = parseNode(r.input);
      if (!input || typeof r.periods !== "number") return null;
      return { op: "lag", input, periods: r.periods };
    }
    default: {
      if (!BINARY_OP_VALUES.includes(op as BinaryOp)) return null;
      const left = parseNode(r.left);
      const right = parseNode(r.right);
      if (!left || !right) return null;
      return { op: op as BinaryOp, left, right };
    }
  }
}

/** spec 문자열을 파싱해 모델로 변환한다. 표현 불가면 null. */
function parseSpec(spec: string): MetricSpecModel | null {
  let raw: unknown;
  try {
    raw = JSON.parse(spec);
  } catch {
    return null;
  }
  if (!raw || typeof raw !== "object") return null;
  const r = raw as Record<string, unknown>;
  const root = parseNode(r.root);
  if (!root) return null;
  const lets: Record<string, MetricNode> = {};
  if (r.lets !== undefined) {
    if (typeof r.lets !== "object" || r.lets === null || Array.isArray(r.lets)) return null;
    for (const [key, value] of Object.entries(r.lets as Record<string, unknown>)) {
      const node = parseNode(value);
      if (!node) return null;
      lets[key] = node;
    }
  }
  return { lets, root };
}

/** 숫자 입력을 안전하게 파싱(빈값·NaN→0). */
function toNum(v: string): number {
  if (v.trim() === "") return 0;
  const n = Number(v);
  return Number.isNaN(n) ? 0 : n;
}

/** NodeEditor props. */
type NodeEditorProps = {
  node: MetricNode;
  onChange: (n: MetricNode) => void;
  letNames: string[];
  universes: StockFilter[];
  firstUniverseId: number;
};

/** 컬럼 드롭다운 옵션(현재값이 목록 밖이면 함께 노출). */
function columnOptions(current: string): string[] {
  return METRIC_COLUMNS.includes(current) ? METRIC_COLUMNS : [current, ...METRIC_COLUMNS];
}

/** 계산식 노드를 재귀적으로 편집하는 트리 에디터. */
function NodeEditor({ node, onChange, letNames, universes, firstUniverseId }: NodeEditorProps) {
  return (
    <div className="flex flex-col gap-2">
      <select
        value={node.op}
        onChange={(e) => onChange(defaultNode(e.target.value, firstUniverseId))}
        className={cx.select + " w-full"}
      >
        <optgroup label="값">
          <option value="const">상수 (const)</option>
          <option value="ref">중간값 참조 (ref)</option>
          <option value="agg">유니버스 집계 (agg)</option>
        </optgroup>
        <optgroup label="변환">
          <option value="roll_mean">이동평균 (roll_mean)</option>
          <option value="ema">지수이동평균 (ema)</option>
          <option value="cumprod1p">누적곱 (cumprod1p)</option>
          <option value="lag">지연 (lag)</option>
        </optgroup>
        <optgroup label="이항연산">
          {BINARY_OPS.map((o) => (
            <option key={o.value} value={o.value}>{o.label} ({o.value})</option>
          ))}
        </optgroup>
      </select>

      {node.op === "const" && (
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">값</span>
          <input
            type="number"
            value={node.value}
            onChange={(e) => onChange({ op: "const", value: toNum(e.target.value) })}
            className={cx.inputNumber}
          />
        </label>
      )}

      {node.op === "ref" && (
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">중간값 이름</span>
          {letNames.length > 0 ? (
            <select
              value={node.name}
              onChange={(e) => onChange({ op: "ref", name: e.target.value })}
              className={cx.select + " w-full"}
            >
              <option value="">선택</option>
              {(letNames.includes(node.name) || node.name === "" ? letNames : [node.name, ...letNames]).map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          ) : (
            <input
              value={node.name}
              onChange={(e) => onChange({ op: "ref", name: e.target.value })}
              placeholder="중간값 이름"
              className={cx.input + " w-full"}
            />
          )}
        </label>
      )}

      {node.op === "agg" && (
        <div className="flex flex-col gap-2">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">집계</span>
            <select
              value={node.agg}
              onChange={(e) => {
                const agg = e.target.value as "MEAN" | "RATIO_GT" | "RATIO_POS";
                if (agg === "RATIO_GT") {
                  onChange({ ...node, agg, colB: node.colB ?? "CLOSE" });
                } else {
                  const { colB, ...rest } = node;
                  void colB;
                  onChange({ ...rest, agg });
                }
              }}
              className={cx.select + " w-full"}
            >
              {AGG_OPTIONS.map((a) => <option key={a.value} value={a.value}>{a.label}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">colA</span>
            <select
              value={node.colA}
              onChange={(e) => onChange({ ...node, colA: e.target.value })}
              className={cx.select + " w-full"}
            >
              {columnOptions(node.colA).map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </label>
          {node.agg === "RATIO_GT" && (
            <label className="block">
              <span className="mb-1 block text-xs text-slate-400">colB</span>
              <select
                value={node.colB ?? ""}
                onChange={(e) => onChange({ ...node, colB: e.target.value })}
                className={cx.select + " w-full"}
              >
                {columnOptions(node.colB ?? "").map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </label>
          )}
          <div className="block">
            <span className="mb-1 block text-xs text-slate-400">유니버스 (종목 필터)</span>
            <div className="flex flex-col gap-1.5 sm:flex-row sm:items-center">
              <select
                value={universes.some((u) => u.id === node.universeFilterId) ? String(node.universeFilterId) : ""}
                onChange={(e) => onChange({ ...node, universeFilterId: toNum(e.target.value) })}
                disabled={universes.length === 0}
                className={cx.select + " w-full sm:flex-1 disabled:opacity-50"}
              >
                <option value="">{universes.length === 0 ? "시스템 종목 필터 없음" : "종목 필터 선택"}</option>
                {universes.map((u) => <option key={u.id} value={u.id}>{u.name} (#{u.id})</option>)}
              </select>
              <input
                type="number"
                value={node.universeFilterId}
                onChange={(e) => onChange({ ...node, universeFilterId: toNum(e.target.value) })}
                placeholder="ID 직접"
                title="필터 ID 직접 입력"
                className={cx.inputNumber + " sm:w-28"}
              />
            </div>
            {universes.length === 0 && (
              <p className="mt-1 text-xs text-amber-400">
                시스템 종목 필터가 없습니다 — 백엔드 연결·로그인 상태를 확인하거나, 종목 검색의 필터 관리에서 먼저 만들어 주세요.
              </p>
            )}
          </div>
        </div>
      )}

      {node.op === "roll_mean" && (
        <div className="flex flex-col gap-2">
          <div className="grid grid-cols-2 gap-2">
            <label className="block">
              <span className="mb-1 block text-xs text-slate-400">window</span>
              <input type="number" value={node.window} onChange={(e) => onChange({ ...node, window: toNum(e.target.value) })} className={cx.inputNumber} />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-slate-400">minPeriods</span>
              <input type="number" value={node.minPeriods} onChange={(e) => onChange({ ...node, minPeriods: toNum(e.target.value) })} className={cx.inputNumber} />
            </label>
          </div>
          <NestedNode label="input" node={node.input} onChange={(n) => onChange({ ...node, input: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
        </div>
      )}

      {node.op === "ema" && (
        <div className="flex flex-col gap-2">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">window</span>
            <input type="number" value={node.window} onChange={(e) => onChange({ ...node, window: toNum(e.target.value) })} className={cx.inputNumber} />
          </label>
          <NestedNode label="input" node={node.input} onChange={(n) => onChange({ ...node, input: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
        </div>
      )}

      {node.op === "cumprod1p" && (
        <NestedNode label="input" node={node.input} onChange={(n) => onChange({ ...node, input: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
      )}

      {node.op === "lag" && (
        <div className="flex flex-col gap-2">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">periods</span>
            <input type="number" value={node.periods} onChange={(e) => onChange({ ...node, periods: toNum(e.target.value) })} className={cx.inputNumber} />
          </label>
          <NestedNode label="input" node={node.input} onChange={(n) => onChange({ ...node, input: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
        </div>
      )}

      {isBinaryNode(node) && (
        <div className="flex flex-col gap-2">
          <NestedNode label="left" node={node.left} onChange={(n) => onChange({ ...node, left: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
          <NestedNode label="right" node={node.right} onChange={(n) => onChange({ ...node, right: n })} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
        </div>
      )}
    </div>
  );
}

/** 라벨 붙은 들여쓴 중첩 NodeEditor. */
function NestedNode({ label, node, onChange, letNames, universes, firstUniverseId }: NodeEditorProps & { label: string }) {
  return (
    <div>
      <span className="mb-1 block text-xs text-slate-400">{label}</span>
      <div className="border-l border-white/10 pl-3">
        <NodeEditor node={node} onChange={onChange} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
      </div>
    </div>
  );
}

function MetricFormModal(
  { metric, onClose, onDone }:
  { metric?: CustomMetric; onClose: () => void; onDone: () => void },
) {
  const editing = !!metric;
  const [name, setName] = useState(metric?.name ?? "");
  const [description, setDescription] = useState(metric?.description ?? "");
  const [shape, setShape] = useState(metric?.shape ?? "SERIES");
  const [priceType, setPriceType] = useState(metric?.priceType ?? "RAW");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // spec 문자열이 저장·미리보기의 단일 소스. UI 빌더는 편집 때마다 이 문자열을 갱신한다.
  const initial = useMemo(() => {
    const raw = metric?.spec ?? "";
    const toState = (m: MetricSpecModel) => ({
      rootNode: m.root,
      lets: Object.entries(m.lets).map(([n, node]) => ({ name: n, node })) as LetEntry[],
    });
    if (!raw.trim()) {
      const st = toState(defaultSpecModel());
      return { ...st, spec: buildSpecString(st.rootNode, st.lets), tab: "builder" as const };
    }
    const parsed = parseSpec(raw);
    if (parsed) {
      const st = toState(parsed);
      return { ...st, spec: raw, tab: "builder" as const };
    }
    const st = toState(defaultSpecModel());
    return { ...st, spec: raw, tab: "json" as const };
    // metric 은 모달 수명 동안 고정 — 초기 1회만 계산.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [spec, setSpec] = useState(initial.spec);
  const [rootNode, setRootNode] = useState<MetricNode>(initial.rootNode);
  const [lets, setLets] = useState<LetEntry[]>(initial.lets);
  const [tab, setTab] = useState<"builder" | "json">(initial.tab);
  const [universes, setUniverses] = useState<StockFilter[]>([]);

  useEffect(() => {
    let active = true;
    fetch("/api/stock-filters/system", { cache: "no-store" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => { if (active) setUniverses(Array.isArray(data) ? data : []); })
      .catch(() => { if (active) setUniverses([]); });
    return () => { active = false; };
  }, []);

  const firstUniverseId = universes[0]?.id ?? 0;
  const letNames = lets.map((l) => l.name.trim()).filter(Boolean);
  const canUseBuilder = useMemo(() => spec.trim() === "" || parseSpec(spec) !== null, [spec]);

  function syncRoot(next: MetricNode) {
    setRootNode(next);
    setSpec(buildSpecString(next, lets));
  }
  function syncLets(next: LetEntry[]) {
    setLets(next);
    setSpec(buildSpecString(rootNode, next));
  }
  function addLet() {
    syncLets([...lets, { name: `x${lets.length + 1}`, node: { op: "const", value: 0 } }]);
  }
  function removeLet(i: number) {
    syncLets(lets.filter((_, idx) => idx !== i));
  }
  function renameLet(i: number, newName: string) {
    syncLets(lets.map((l, idx) => (idx === i ? { ...l, name: newName } : l)));
  }
  function setLetNode(i: number, node: MetricNode) {
    syncLets(lets.map((l, idx) => (idx === i ? { ...l, node } : l)));
  }

  function goBuilder() {
    const parsed = spec.trim() === "" ? defaultSpecModel() : parseSpec(spec);
    if (!parsed) return;
    const letsArr: LetEntry[] = Object.entries(parsed.lets).map(([n, node]) => ({ name: n, node }));
    setRootNode(parsed.root);
    setLets(letsArr);
    setSpec(buildSpecString(parsed.root, letsArr));
    setTab("builder");
  }

  const [previewing, setPreviewing] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [points, setPoints] = useState<MetricPoint[] | null>(null);

  const canSubmit = !!name.trim() && !!shape && !!spec.trim() && !submitting;

  async function preview() {
    setPreviewing(true);
    setPreviewError(null);
    setPoints(null);
    try {
      const res = await fetch("/api/admin/ops/custom-metrics/preview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ spec, priceType: priceType || null }),
      });
      if (!res.ok) {
        setPreviewError(await readError(res));
        return;
      }
      setPoints((await res.json()) ?? []);
    } finally {
      setPreviewing(false);
    }
  }

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const body = {
        name: name.trim(),
        description: description.trim() || null,
        shape,
        spec,
        priceType: priceType || null,
      };
      const res = editing
        ? await fetch(`/api/admin/ops/custom-metrics/${metric!.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
          })
        : await fetch("/api/admin/ops/custom-metrics", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
          });
      if (!res.ok) {
        setError(await readError(res));
        return;
      }
      onDone();
    } finally {
      setSubmitting(false);
    }
  }

  // null이 아닌 최근 값 몇 개를 날짜:값 리스트로 보여줄 slice.
  const recent = points ? points.filter((p) => p.value !== null).slice(-6) : [];

  return (
    <Modal title={editing ? "지표 수정" : "지표 등록"} onClose={onClose}>
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">이름</span>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="regime" className={cx.input + " w-full"} />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">설명</span>
            <input value={description} onChange={(e) => setDescription(e.target.value)} className={cx.input + " w-full"} />
          </label>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">모양</span>
            <select value={shape} onChange={(e) => setShape(e.target.value)} className={cx.select + " w-full"}>
              {SHAPES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">주가유형</span>
            <select value={priceType} onChange={(e) => setPriceType(e.target.value)} className={cx.select + " w-full"}>
              {PRICE_TYPES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
            </select>
          </label>
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between gap-2 flex-wrap">
            <span className="text-xs text-slate-400">계산식 spec (DSL)</span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={goBuilder}
                disabled={!canUseBuilder}
                className={(tab === "builder" ? cx.btnToggleOn : cx.btnToggleOff) + " !py-1.5 !px-3 text-xs disabled:opacity-40 disabled:cursor-not-allowed"}
              >
                UI 빌더
              </button>
              <button
                type="button"
                onClick={() => setTab("json")}
                className={(tab === "json" ? cx.btnToggleOn : cx.btnToggleOff) + " !py-1.5 !px-3 text-xs"}
              >
                JSON
              </button>
            </div>
          </div>

          {tab === "json" && (
            <>
              <textarea
                value={spec}
                onChange={(e) => setSpec(e.target.value)}
                rows={8}
                placeholder={SPEC_PLACEHOLDER}
                className={cx.input + " w-full font-mono text-xs"}
              />
              {!canUseBuilder && (
                <p className="text-xs text-amber-400">이 스펙은 UI로 표현할 수 없습니다. JSON 탭에서 편집하세요.</p>
              )}
            </>
          )}

          {tab === "builder" && (
            <div className="flex flex-col gap-3">
              <p className="text-xs text-slate-500">
                예: agg(RATIO_POS, RET_1D, 보통주) = 상승비율. 지수 레짐 = cumprod1p(agg MEAN RET_1D) &gt; roll_mean(...).
              </p>

              <div className="flex flex-col gap-2 rounded-lg border border-white/10 p-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-300">중간값 (lets)</span>
                  <button
                    type="button"
                    onClick={addLet}
                    className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 transition"
                  >
                    중간값 추가
                  </button>
                </div>
                {lets.length === 0 && <p className="text-xs text-slate-500">중간값 없음. 필요하면 추가하세요.</p>}
                {lets.map((entry, i) => (
                  <div key={i} className="flex flex-col gap-2 rounded-lg border border-white/10 bg-white/5 p-2.5">
                    <div className="flex items-center gap-2">
                      <input
                        value={entry.name}
                        onChange={(e) => renameLet(i, e.target.value)}
                        placeholder="이름"
                        className={cx.input + " flex-1"}
                      />
                      <button
                        type="button"
                        onClick={() => removeLet(i)}
                        className="rounded-lg border border-rose-500/30 px-3 py-2 text-xs text-rose-300 hover:bg-rose-500/10 transition whitespace-nowrap"
                      >
                        삭제
                      </button>
                    </div>
                    <div className="border-l border-white/10 pl-3">
                      <NodeEditor node={entry.node} onChange={(n) => setLetNode(i, n)} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex flex-col gap-2 rounded-lg border border-white/10 p-3">
                <span className="text-xs font-medium text-slate-300">최종 결과 (root)</span>
                <NodeEditor node={rootNode} onChange={syncRoot} letNames={letNames} universes={universes} firstUniverseId={firstUniverseId} />
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button onClick={preview} disabled={previewing || !spec.trim()} className={cx.btnSecondary}>
            {previewing ? "미리보기 중..." : "미리보기"}
          </button>
          <span className="text-xs text-slate-500">현재 spec·주가유형으로 시계열을 시험 계산합니다.</span>
        </div>

        {previewError && <p className="text-sm text-rose-400">{previewError}</p>}

        {points && (
          <div className="flex flex-col gap-2 rounded-lg border border-white/10 bg-white/5 p-3">
            {recent.length === 0 ? (
              <p className="text-xs text-slate-400">표시할 값이 없습니다.</p>
            ) : (
              <>
                <PreviewChart points={points} />
                <ul className="flex flex-col gap-0.5 font-mono text-xs text-slate-300">
                  {recent.map((p) => (
                    <li key={p.date} className="flex justify-between">
                      <span className="text-slate-500">{p.date}</span>
                      <span className="tabular-nums">{p.value}</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        )}

        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button onClick={submit} disabled={!canSubmit} className={cx.btnPrimary}>
            {submitting ? "저장 중..." : editing ? "수정" : "등록"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function RecomputeModal({ metric, onClose, onDone }: { metric: CustomMetric; onClose: () => void; onDone: () => void }) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/admin/ops/custom-metrics/${metric.id}/recompute`, { method: "POST" });
      if (!res.ok) {
        setError(await readError(res));
        return;
      }
      onDone();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="지표 재계산" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-300">
          <span className="font-medium text-white">{metric.name}</span> 지표의 저장값을 삭제하고 진행 상태를 리셋합니다.
          다음 배치가 처음부터 다시 계산합니다.
        </p>
        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button onClick={submit} disabled={submitting} className={cx.btnPrimary}>
            {submitting ? "처리 중..." : "재계산"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function DeleteMetricModal({ metric, onClose, onDone }: { metric: CustomMetric; onClose: () => void; onDone: () => void }) {
  const [confirmText, setConfirmText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/admin/ops/custom-metrics/${metric.id}`, { method: "DELETE" });
      if (!res.ok) {
        setError(await readError(res));
        return;
      }
      onDone();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="지표 삭제" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-300">
          <span className="font-medium text-white">{metric.name}</span> 지표 정의와 <span className="text-rose-300">모든 저장값</span>이 함께 삭제됩니다. 되돌릴 수 없습니다.
        </p>
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">확인을 위해 지표 이름(<span className="text-slate-200">{metric.name}</span>)을 입력하세요</span>
          <input value={confirmText} onChange={(e) => setConfirmText(e.target.value)} className={cx.input} placeholder={metric.name} />
        </label>
        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button
            onClick={submit}
            disabled={submitting || confirmText !== metric.name}
            className="rounded-lg bg-rose-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-rose-500 disabled:opacity-50"
          >
            {submitting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </div>
    </Modal>
  );
}
