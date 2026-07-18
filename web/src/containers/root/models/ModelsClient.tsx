"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { cx } from "@/utils/cx";

/** 모델 메타·상태·채점 진행 정보. 백엔드 MlModelResponse 미러. */
interface MlModel {
  id: number;
  name: string;
  version: string;
  outputType: string;
  scoreExchanges: string[];
  scorePriceType: string;
  status: "ACTIVE" | "INACTIVE";
  scoreCursor: string | null;
  lastScoredAt: string | null;
  lastError: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

/** 스케줄러 작업 진행 상태. 백엔드 JobStatus 미러. */
interface JobStatus {
  name: string;
  state: string;
  total: number;
  processed: number;
  startedAtEpochMs: number;
  updatedAtEpochMs: number;
  message: string | null;
}

const SCORING_JOB_NAME = "MODEL_SCORING";

const DEFAULT_EXCHANGES = ["KOSPI", "KOSDAQ"];
// 채점 시세(venue): 통합·NXT는 단일 거래소값, KRX(정규장)는 아래 시장 선택으로 확장된다.
const VENUES = [
  { value: "INTEGRATED", label: "통합" },
  { value: "KRX", label: "KRX (정규장)" },
  { value: "NXT", label: "NXT" },
];
const KRX_MARKETS = [
  { value: "KOSPI", label: "코스피" },
  { value: "KOSDAQ", label: "코스닥" },
  { value: "KONEX", label: "코넥스" },
];
const KRX_MARKET_VALUES = KRX_MARKETS.map((m) => m.value);
const PRICE_TYPES = [
  { value: "RAW", label: "원주가 (RAW)" },
  { value: "ADJUSTED", label: "수정주가 (ADJUSTED)" },
];

const ERROR_LABEL: Record<string, string> = {
  MISSING_FILE: "파일이 누락되었습니다",
  FILE_READ_FAILED: "파일을 읽지 못했습니다",
  INVALID_EXCHANGE: "잘못된 거래소입니다",
  INVALID_PRICE_TYPE: "잘못된 주가유형입니다",
  MODEL_NOT_FOUND: "모델을 찾을 수 없습니다",
  DELETE_NOT_CONFIRMED: "삭제 확인이 필요합니다",
  UNPROCESSABLE_ENTITY: "메타 검증 또는 활성화에 실패했습니다",
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

function statusChip(status: MlModel["status"]) {
  const cfg = status === "ACTIVE"
    ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/30"
    : "bg-slate-500/20 text-slate-400 border-slate-500/30";
  return <span className={`px-2 py-0.5 rounded text-xs border ${cfg}`}>{status === "ACTIVE" ? "활성" : "비활성"}</span>;
}

/** 화면 중앙 고정 크기 모달 셸. */
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-lg max-h-[85vh] overflow-y-auto rounded-2xl border border-white/10 bg-slate-900 shadow-2xl">
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

export default function ModelsClient() {
  const [models, setModels] = useState<MlModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [scoring, setScoring] = useState<JobStatus | null>(null);

  const [showRegister, setShowRegister] = useState(false);
  const [resetTarget, setResetTarget] = useState<MlModel | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MlModel | null>(null);
  const [scoresTarget, setScoresTarget] = useState<MlModel | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const fetchModels = useCallback(async () => {
    const res = await fetch("/api/admin/ops/models", { cache: "no-store" });
    if (res.ok) setModels((await res.json()) ?? []);
    setLoading(false);
  }, []);

  const fetchScoring = useCallback(async () => {
    const res = await fetch("/api/admin/ops/scheduler-status", { cache: "no-store" });
    if (res.ok) {
      const all: JobStatus[] = (await res.json()) ?? [];
      setScoring(all.find((j) => j.name === SCORING_JOB_NAME) ?? null);
    }
  }, []);

  useEffect(() => { fetchModels(); fetchScoring(); }, [fetchModels, fetchScoring]);

  const scoringRunning = scoring?.state === "RUNNING";
  useEffect(() => {
    if (!scoringRunning) return;
    const id = setInterval(fetchScoring, 3000);
    return () => clearInterval(id);
  }, [scoringRunning, fetchScoring]);

  async function toggleStatus(model: MlModel) {
    setBusyId(model.id);
    try {
      const action = model.status === "ACTIVE" ? "deactivate" : "activate";
      const res = await fetch(`/api/admin/ops/models/${model.id}/${action}`, { method: "POST" });
      if (!res.ok) {
        alert(await readError(res));
        return;
      }
      await fetchModels();
    } finally {
      setBusyId(null);
    }
  }

  const scoringPercent = scoring && scoring.total > 0
    ? Math.min(100, Math.round((scoring.processed / scoring.total) * 100))
    : 0;

  return (
    <div className="flex min-h-full flex-col gap-4 p-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-lg font-semibold text-white">모델 관리</h1>
        <button onClick={() => setShowRegister(true)} className={cx.btnPrimary}>
          모델 등록
        </button>
      </div>

      {/* 채점 진행률 */}
      {scoring && (
        <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-3">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-sm font-medium text-slate-200">
              채점 진행
              <span className="ml-2 text-xs text-slate-400">
                {scoring.state === "RUNNING" ? "실행 중" : scoring.state}
              </span>
            </span>
            <span className="text-xs text-slate-400 tabular-nums">
              {scoring.processed.toLocaleString()} / {scoring.total.toLocaleString()} ({scoringPercent}%)
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
            <div className="h-full bg-indigo-500 transition-all" style={{ width: `${scoringPercent}%` }} />
          </div>
          {scoring.message && <p className="mt-1.5 text-[11px] text-slate-500">{scoring.message}</p>}
        </div>
      )}

      {/* 모델 목록 */}
      <div className="overflow-x-auto rounded-lg border border-white/10">
        <table className={cx.table.root + " min-w-[1000px]"}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>이름</th>
              <th className={cx.table.th}>버전</th>
              <th className={cx.table.th}>출력</th>
              <th className={cx.table.th}>거래소 / 주가</th>
              <th className={cx.table.th}>상태</th>
              <th className={cx.table.th}>채점 커서</th>
              <th className={cx.table.th}>마지막 채점</th>
              <th className={cx.table.th}>등록</th>
              <th className={cx.table.th}></th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {loading && (
              <tr><td colSpan={9} className="py-10 text-center text-slate-400">불러오는 중...</td></tr>
            )}
            {!loading && models.length === 0 && (
              <tr><td colSpan={9} className="py-10 text-center text-slate-400">등록된 모델이 없습니다.</td></tr>
            )}
            {models.map((m) => (
              <tr key={m.id} className={cx.table.tr}>
                <td className={cx.table.td + " font-medium text-white whitespace-nowrap"}>{m.name}</td>
                <td className={cx.table.td + " whitespace-nowrap"}>{m.version}</td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>{m.outputType}</td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>{(m.scoreExchanges ?? []).join(", ") || "—"} / {m.scorePriceType}</td>
                <td className={cx.table.td + " whitespace-nowrap"}>{statusChip(m.status)}</td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>{m.scoreCursor ?? "미시작"}</td>
                <td className={cx.table.td + " text-xs whitespace-nowrap"}>
                  {m.lastError ? (
                    <span
                      title={m.lastError}
                      className="inline-block max-w-[220px] truncate rounded border border-rose-500/30 bg-rose-500/15 px-2 py-0.5 text-rose-300"
                    >
                      {m.lastError}
                    </span>
                  ) : (
                    <span className="text-slate-400">{m.lastScoredAt ? formatDt(m.lastScoredAt) : "—"}</span>
                  )}
                </td>
                <td className={cx.table.td + " text-xs text-slate-400 whitespace-nowrap"}>{formatDt(m.createdAt)}</td>
                <td className={cx.table.td}>
                  <div className="flex flex-wrap items-center gap-1.5">
                    <button
                      onClick={() => toggleStatus(m)}
                      disabled={busyId === m.id}
                      className={(m.status === "ACTIVE" ? cx.btnToggleOff : cx.btnToggleOn) + " !py-1.5 !px-3 text-xs whitespace-nowrap"}
                    >
                      {busyId === m.id ? "..." : m.status === "ACTIVE" ? "비활성화" : "활성화"}
                    </button>
                    <button
                      onClick={() => setResetTarget(m)}
                      className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 transition whitespace-nowrap"
                    >
                      커서 리셋
                    </button>
                    <button
                      onClick={() => setScoresTarget(m)}
                      className="rounded-lg border border-white/15 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/5 transition whitespace-nowrap"
                    >
                      점수 삭제
                    </button>
                    <button
                      onClick={() => setDeleteTarget(m)}
                      className="rounded-lg border border-rose-500/30 px-3 py-1.5 text-xs text-rose-300 hover:bg-rose-500/10 transition whitespace-nowrap"
                    >
                      모델 삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showRegister && (
        <RegisterModal
          onClose={() => setShowRegister(false)}
          onDone={() => { setShowRegister(false); fetchModels(); }}
        />
      )}
      {resetTarget && (
        <ResetCursorModal
          model={resetTarget}
          onClose={() => setResetTarget(null)}
          onDone={() => { setResetTarget(null); fetchModels(); }}
        />
      )}
      {deleteTarget && (
        <DeleteModelModal
          model={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDone={() => { setDeleteTarget(null); fetchModels(); }}
        />
      )}
      {scoresTarget && (
        <DeleteScoresModal
          model={scoresTarget}
          onClose={() => setScoresTarget(null)}
          onDone={() => { setScoresTarget(null); fetchModels(); }}
        />
      )}
    </div>
  );
}

function FileDropzone(
  { label, accept, file, onFile }:
  { label: string; accept: string; file: File | null; onFile: (f: File | null) => void },
) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  const pick = (files: FileList | null) => onFile(files && files.length > 0 ? files[0] : null);

  return (
    <div className="block">
      <span className="mb-1 block text-xs text-slate-400">{label}</span>
      <div
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") { e.preventDefault(); inputRef.current?.click(); }
        }}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => { e.preventDefault(); setDragOver(false); pick(e.dataTransfer.files); }}
        className={`flex min-h-[96px] cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed px-4 py-5 text-center text-sm transition focus:outline-none focus:ring-2 focus:ring-indigo-400 ${
          dragOver
            ? "border-indigo-400 bg-indigo-500/15"
            : "border-white/20 hover:border-white/40 hover:bg-white/5"
        }`}
      >
        {file ? (
          <>
            <span className="break-all font-medium text-white">{file.name}</span>
            <span className="text-xs text-slate-400">{(file.size / 1024).toFixed(1)} KB · 클릭하거나 끌어다 놓아 교체</span>
          </>
        ) : (
          <>
            <span className="text-slate-300">파일을 끌어다 놓거나 <span className="text-indigo-300">클릭해 선택</span></span>
            <span className="text-xs text-slate-500">{accept}</span>
          </>
        )}
      </div>
      <input ref={inputRef} type="file" accept={accept} onChange={(e) => pick(e.target.files)} className="hidden" />
    </div>
  );
}

function RegisterModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [artifact, setArtifact] = useState<File | null>(null);
  const [meta, setMeta] = useState<File | null>(null);
  const [name, setName] = useState("");
  const [version, setVersion] = useState("");
  const [zoneDesc, setZoneDesc] = useState("");
  const [zoneConditions, setZoneConditions] = useState("");
  const [scoreExchanges, setScoreExchanges] = useState<string[]>(DEFAULT_EXCHANGES);
  const [scorePriceType, setScorePriceType] = useState("ADJUSTED");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = !!artifact && !!meta && !!name.trim() && !!version.trim() && scoreExchanges.length > 0 && !submitting;

  // meta.json을 읽어 이름·버전·진입존을 편집 필드에 미리 채운다(이후 사용자가 수정 가능).
  async function handleMeta(file: File | null) {
    setMeta(file);
    if (!file) return;
    try {
      const parsed = JSON.parse(await file.text());
      setName(parsed.name ?? "");
      setVersion(parsed.version ?? "");
      setZoneDesc(parsed.entry_zone?.desc ?? "");
      setZoneConditions((parsed.entry_zone?.conditions ?? []).join("\n"));
    } catch {
      setError("meta.json 파싱 실패 — 형식을 확인하세요");
    }
  }
  const krxOn = scoreExchanges.some((e) => KRX_MARKET_VALUES.includes(e));

  // 통합·NXT는 단일 토글. KRX는 켜면 기본 시장(코스피·코스닥) 추가, 끄면 정규장 시장 전부 제거.
  function toggleVenue(value: string) {
    if (value === "KRX") {
      setScoreExchanges((prev) => {
        const on = prev.some((e) => KRX_MARKET_VALUES.includes(e));
        return on
          ? prev.filter((e) => !KRX_MARKET_VALUES.includes(e))
          : [...prev, "KOSPI", "KOSDAQ"];
      });
      return;
    }
    setScoreExchanges((prev) =>
      prev.includes(value) ? prev.filter((e) => e !== value) : [...prev, value]);
  }

  function toggleMarket(value: string) {
    setScoreExchanges((prev) =>
      prev.includes(value) ? prev.filter((e) => e !== value) : [...prev, value]);
  }

  const chip = (checked: boolean) =>
    `flex min-h-[44px] cursor-pointer items-center gap-2 rounded-lg border px-3 py-2 text-sm transition ${
      checked ? "border-indigo-500/50 bg-indigo-500/15 text-white" : "border-white/15 text-slate-300 hover:bg-white/5"
    }`;

  async function submit() {
    if (!artifact || !meta) return;
    setSubmitting(true);
    setError(null);
    try {
      const form = new FormData();
      form.append("artifact", artifact);
      form.append("meta", meta);
      form.append("name", name.trim());
      form.append("version", version.trim());
      form.append("zoneDesc", zoneDesc);
      form.append("zoneConditions", zoneConditions);
      form.append("scoreExchanges", scoreExchanges.join(","));
      form.append("scorePriceType", scorePriceType);
      const res = await fetch("/api/admin/ops/models", { method: "POST", body: form });
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
    <Modal title="모델 등록" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <FileDropzone label="모델 아티팩트 (.pkl)" accept=".pkl" file={artifact} onFile={setArtifact} />
        <FileDropzone label="메타 (meta.json)" accept=".json,application/json" file={meta} onFile={handleMeta} />
        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">이름</span>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="swing_entry" className={cx.input + " w-full"} />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">버전</span>
            <input value={version} onChange={(e) => setVersion(e.target.value)} placeholder="1.0.0" className={cx.input + " w-full"} />
          </label>
        </div>
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">진입존 설명 (내용)</span>
          <input value={zoneDesc} onChange={(e) => setZoneDesc(e.target.value)} className={cx.input + " w-full"} />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">진입존 조건 (zone, 한 줄에 하나)</span>
          <textarea value={zoneConditions} onChange={(e) => setZoneConditions(e.target.value)} rows={4}
            placeholder={"rsi_14>=50\nmacd_histogram>0"} className={cx.input + " w-full font-mono text-xs"} />
        </label>
        <div className="block">
          <span className="mb-1 block text-xs text-slate-400">채점 시세</span>
          <div className="grid grid-cols-3 gap-2">
            {VENUES.map((v) => {
              const checked = v.value === "KRX" ? krxOn : scoreExchanges.includes(v.value);
              return (
                <label key={v.value} className={chip(checked)}>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggleVenue(v.value)}
                    className="h-5 w-5"
                  />
                  {v.label}
                </label>
              );
            })}
          </div>
        </div>
        {krxOn && (
          <div className="block">
            <span className="mb-1 block text-xs text-slate-400">KRX 시장 선택</span>
            <div className="grid grid-cols-3 gap-2">
              {KRX_MARKETS.map((m) => {
                const checked = scoreExchanges.includes(m.value);
                return (
                  <label key={m.value} className={chip(checked)}>
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleMarket(m.value)}
                      className="h-5 w-5"
                    />
                    {m.label}
                  </label>
                );
              })}
            </div>
          </div>
        )}
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">채점 주가유형</span>
          <select value={scorePriceType} onChange={(e) => setScorePriceType(e.target.value)} className={cx.select + " w-full"}>
            {PRICE_TYPES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
          </select>
        </label>
        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button onClick={submit} disabled={!canSubmit} className={cx.btnPrimary}>
            {submitting ? "등록 중..." : "등록"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function ResetCursorModal({ model, onClose, onDone }: { model: MlModel; onClose: () => void; onDone: () => void }) {
  const [toDate, setToDate] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/admin/ops/models/${model.id}/reset-cursor`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ toDate: toDate || null }),
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

  return (
    <Modal title="채점 커서 리셋" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-300">
          <span className="font-medium text-white">{model.name}</span> 모델의 채점 커서를 되돌립니다.
          지정 거래일 이후 점수가 삭제되고 다음 배치가 그 지점부터 재채점합니다.
        </p>
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">되돌릴 거래일 (비우면 미시작으로 리셋 — 전 점수 삭제)</span>
          <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} className={cx.inputDate + " w-full"} />
        </label>
        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button onClick={submit} disabled={submitting} className={cx.btnPrimary}>
            {submitting ? "처리 중..." : "리셋"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function DeleteModelModal({ model, onClose, onDone }: { model: MlModel; onClose: () => void; onDone: () => void }) {
  const [confirmText, setConfirmText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/admin/ops/models/${model.id}`, { method: "DELETE" });
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
    <Modal title="모델 삭제" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-300">
          <span className="font-medium text-white">{model.name}</span> 모델과 그 모델의 <span className="text-rose-300">모든 점수</span>가 함께 삭제됩니다. 되돌릴 수 없습니다.
        </p>
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">확인을 위해 모델 이름(<span className="text-slate-200">{model.name}</span>)을 입력하세요</span>
          <input value={confirmText} onChange={(e) => setConfirmText(e.target.value)} className={cx.input} placeholder={model.name} />
        </label>
        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button
            onClick={submit}
            disabled={submitting || confirmText !== model.name}
            className="rounded-lg bg-rose-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-rose-500 disabled:opacity-50"
          >
            {submitting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

type ScoreScope = "ALL" | "RANGE" | "TICKER";

function DeleteScoresModal({ model, onClose, onDone }: { model: MlModel; onClose: () => void; onDone: () => void }) {
  const [scope, setScope] = useState<ScoreScope>("RANGE");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [ticker, setTicker] = useState("");
  const [confirmChecked, setConfirmChecked] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<number | null>(null);

  const scopeInvalid =
    (scope === "TICKER" && !ticker.trim()) ||
    (scope === "RANGE" && !from && !to);

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const body =
        scope === "ALL"
          ? { confirm: true }
          : scope === "TICKER"
            ? { ticker: ticker.trim(), confirm: true }
            : { from: from || null, to: to || null, confirm: true };
      const res = await fetch(`/api/admin/ops/models/${model.id}/scores/delete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        setError(await readError(res));
        return;
      }
      const data = await res.json();
      setResult(data?.deleted ?? 0);
    } finally {
      setSubmitting(false);
    }
  }

  if (result !== null) {
    return (
      <Modal title="점수 삭제 완료" onClose={onDone}>
        <div className="flex flex-col gap-4">
          <p className="text-sm text-slate-300">{result.toLocaleString()}건의 점수가 삭제되었습니다.</p>
          <div className="flex justify-end">
            <button onClick={onDone} className={cx.btnPrimary}>확인</button>
          </div>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="점수 데이터 삭제" onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-slate-300">
          <span className="font-medium text-white">{model.name}</span> 모델의 점수를 삭제합니다.
        </p>
        <div className="flex gap-2">
          {([
            ["ALL", "전체"],
            ["RANGE", "기간"],
            ["TICKER", "종목"],
          ] as const).map(([v, label]) => (
            <button
              key={v}
              type="button"
              onClick={() => setScope(v)}
              className={scope === v ? cx.btnToggleOn : cx.btnToggleOff}
            >
              {label}
            </button>
          ))}
        </div>

        {scope === "RANGE" && (
          <div className="grid grid-cols-2 gap-3">
            <label className="block">
              <span className="mb-1 block text-xs text-slate-400">시작일 (포함)</span>
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={cx.inputDate + " w-full"} />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-slate-400">종료일 (포함)</span>
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={cx.inputDate + " w-full"} />
            </label>
          </div>
        )}
        {scope === "TICKER" && (
          <label className="block">
            <span className="mb-1 block text-xs text-slate-400">종목 티커</span>
            <input value={ticker} onChange={(e) => setTicker(e.target.value)} className={cx.input} placeholder="예: 005930" />
          </label>
        )}
        {scope === "ALL" && (
          <p className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">
            이 모델의 모든 점수가 삭제됩니다. 되돌릴 수 없습니다.
          </p>
        )}

        <label className="flex items-center gap-2 text-sm text-slate-300">
          <input type="checkbox" checked={confirmChecked} onChange={(e) => setConfirmChecked(e.target.checked)} className="h-4 w-4" />
          삭제를 확인합니다
        </label>

        {error && <p className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className={cx.btnSecondary}>취소</button>
          <button
            onClick={submit}
            disabled={submitting || !confirmChecked || scopeInvalid}
            className="rounded-lg bg-rose-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-rose-500 disabled:opacity-50"
          >
            {submitting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </div>
    </Modal>
  );
}
