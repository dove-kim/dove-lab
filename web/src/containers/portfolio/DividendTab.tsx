"use client";

import { useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import { cx } from "@/utils/cx";
import { type PortfolioPosition, type PortfolioTx, natMoney } from "@/types/portfolio";
import { usePortfolioData, won, fxRateOf } from "./usePortfolioData";
import { usePaged, Pagination } from "./Pagination";
import { useScope, scopeBase, ScopeSelector } from "./scopeView";

/** 종목별 실수령 배당 집계 — 세후 합계와 개별 배당 거래 목록. */
interface Received {
  net: number;
  txns: PortfolioTx[];
}

/** 통화를 항상 표기하는 금액 문자열(KRW=…원, 외화=심볼+숫자). */
function money(v: number, cur: string): string {
  return cur === "KRW" ? won(Math.round(v)) + "원" : natMoney(v, cur);
}

/** 한 연도의 월별(1~12) 배당 시계열. */
interface YearLine {
  year: string;
  color: string;
  dash: string;
  values: number[];
}

// 겹쳐 그릴 연도 색(최신→과거, 검증된 다크 팔레트 고정 순서).
const LINE_COLORS = ["#3987e5", "#199e70", "#c98500"];
// 값이 겹쳐도 구분되도록 선 스타일을 달리 준다(실선/파선/점선).
const LINE_DASH = ["", "7 4", "2 5"];

/**
 * 거래를 (연도 → 월별 세후 배당 12칸) 맵으로 접는다. rate가 null이면 그 거래는 제외.
 */
function monthlyByYear(txns: PortfolioTx[], rate: (t: PortfolioTx) => number | undefined): Map<string, number[]> {
  const m = new Map<string, number[]>();
  for (const t of txns) {
    const r = rate(t);
    if (r == null) continue;
    const y = t.tradedAt.slice(0, 4);
    if (!m.has(y)) m.set(y, new Array(12).fill(0));
    m.get(y)![Number(t.tradedAt.slice(5, 7)) - 1] += t.amount * r;
  }
  return m;
}

/**
 * 최근 3개년만 골라 겹쳐 그릴 라인 시리즈로 만든다(최신 연도가 진한 색).
 */
function recentYearLines(map: Map<string, number[]>): YearLine[] {
  return Array.from(map.keys())
    .sort()
    .slice(-3)
    .reverse()
    .map((year, i) => ({
      year,
      color: LINE_COLORS[i] ?? "#9085e9",
      dash: LINE_DASH[i] ?? "",
      values: (map.get(year) ?? new Array(12).fill(0)).map((v) => Math.round(v)),
    }));
}

/**
 * 월별 배당 선그래프 — 최근 연도들을 1~12월 위에 겹쳐 그려 같은 달끼리 비교한다.
 */
function MonthLines({ series, cur }: { series: YearLine[]; cur: string }) {
  const max = Math.max(1, ...series.flatMap((s) => s.values));
  const W = 480;
  const H = 184;
  const padL = 12;
  const padR = 12;
  const padT = 28;
  const padB = 22;
  const plotW = W - padL - padR;
  const plotH = H - padT - padB;
  const x = (i: number) => padL + (plotW * i) / 11;
  const y = (v: number) => padT + plotH * (1 - v / max);
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-3">
        {series.map((s) => (
          <span key={s.year} className="flex items-center gap-1.5 text-xs text-slate-300">
            <svg width="20" height="6" aria-hidden>
              <line x1="0" y1="3" x2="20" y2="3" stroke={s.color} strokeWidth="2" strokeLinecap="round" strokeDasharray={s.dash || undefined} />
            </svg>
            {s.year}년
          </span>
        ))}
      </div>
      <div className="overflow-x-auto">
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full min-w-[440px]" style={{ height: H }}>
          <line x1={padL} y1={padT + plotH} x2={W - padR} y2={padT + plotH} stroke="rgba(255,255,255,0.1)" strokeWidth="1" />
          {series.map((s) => (
            <g key={s.year}>
              <polyline
                fill="none"
                stroke={s.color}
                strokeWidth="2"
                strokeLinejoin="round"
                strokeLinecap="round"
                strokeDasharray={s.dash || undefined}
                points={s.values.map((v, i) => `${x(i)},${y(v)}`).join(" ")}
              />
              {s.values.map((v, i) => (
                <circle key={i} cx={x(i)} cy={y(v)} r="2.5" fill={s.color}>
                  <title>{`${s.year}년 ${i + 1}월: ${money(v, cur)}`}</title>
                </circle>
              ))}
              {s.values.map((v, i) =>
                v > 0 ? (
                  <text
                    key={`v${i}`}
                    x={x(i)}
                    y={Math.max(y(v) - 5, 9)}
                    textAnchor={i === 0 ? "start" : i === 11 ? "end" : "middle"}
                    fontSize="9"
                    fill={s.color}
                  >
                    {money(v, cur)}
                  </text>
                ) : null
              )}
            </g>
          ))}
          {Array.from({ length: 12 }, (_, i) => (
            <text key={i} x={x(i)} y={H - 6} textAnchor="middle" fontSize="10" fill="#898781">
              {i + 1}
            </text>
          ))}
        </svg>
      </div>
    </div>
  );
}

/**
 * 배당 관리 탭 — 배당 추적으로 고른 보유 종목의 예상 배당과 실제 수령 배당을 본다.
 * 추적 대상 선택·배당률 편집은 내 계좌에서만 — 공유받은 계좌는 주인 설정으로 열람만.
 */
export default function DividendTab() {
  const [scope] = useScope();
  const [reloadKey, setReloadKey] = useState(0);
  const [picking, setPicking] = useState(false);
  const [detail, setDetail] = useState<PortfolioPosition | null>(null);
  const [rateEdit, setRateEdit] = useState<PortfolioPosition | null>(null);
  const [dividendCur, setDividendCur] = useState("");
  // 추적 토글 낙관적 갱신(holdingId → tracked). 재조회 없이 즉시 반영해 화면 깜박임 제거.
  const [trackOverride, setTrackOverride] = useState<Record<number, boolean>>({});
  const { data, err } = usePortfolioData<PortfolioPosition[]>(
    `${scopeBase(scope)}/positions${reloadKey ? `?_=${reloadKey}` : ""}`
  );
  const { data: txns } = usePortfolioData<PortfolioTx[]>(`${scopeBase(scope)}/transactions`);
  const editable = scope === "own";
  const reload = () => setReloadKey((k) => k + 1);

  // 보기 대상이 바뀌면 낙관적 오버라이드는 폐기(새 데이터가 진실).
  useEffect(() => setTrackOverride({}), [scope]);

  const isTracked = (p: PortfolioPosition) =>
    p.holdingId != null && Object.prototype.hasOwnProperty.call(trackOverride, p.holdingId)
      ? trackOverride[p.holdingId as number]
      : !!p.dividendTracked;

  // (계좌|종목) → 실수령 배당(세후 합계 + 거래 목록).
  const received = useMemo(() => {
    const m = new Map<string, Received>();
    for (const t of txns ?? []) {
      if (t.type !== "DIVIDEND" || !t.symbol) continue;
      const k = t.account + "|" + t.symbol;
      const r = m.get(k) ?? { net: 0, txns: [] };
      r.net += t.amount;
      r.txns.push(t);
      m.set(k, r);
    }
    return m;
  }, [txns]);
  const recOf = (p: PortfolioPosition): Received | undefined => received.get(p.account + "|" + p.symbol);

  // 종목별 현재 환율(fx=평가액/원통화평가). 과거 환율 이력은 없음.
  const fxByKey = useMemo(() => {
    const fx = new Map<string, number>();
    for (const p of data ?? []) {
      const rate = fxRateOf(p);
      if (rate != null) fx.set(p.account + "|" + p.symbol, rate);
    }
    return fx;
  }, [data]);

  const dividendTxns = useMemo(
    () => (txns ?? []).filter((t) => t.type === "DIVIDEND" && t.symbol),
    [txns]
  );

  // 최근 3개년 월별 배당 — 통화별(원통화 그대로)로 겹쳐 비교. 과거 환율 이력이 없어 환산하지 않는다.
  const dividendGroups = useMemo(() => {
    const byCur = new Map<string, PortfolioTx[]>();
    for (const t of dividendTxns) {
      if (!byCur.has(t.currency)) byCur.set(t.currency, []);
      byCur.get(t.currency)!.push(t);
    }
    return Array.from(byCur.entries())
      .sort((a, b) => (a[0] === "KRW" ? -1 : b[0] === "KRW" ? 1 : a[0].localeCompare(b[0])))
      .map(([cur, txs]) => ({ key: cur, cur, series: recentYearLines(monthlyByYear(txs, () => 1)) }))
      .filter((g) => g.series.length > 0);
  }, [dividendTxns]);

  const currentYear = String(new Date().getFullYear());
  // 연평균 배당 상승률(CAGR) — 진행 중인 올해 제외, 완결 연도 총배당(원 환산) 기준.
  const dividendCagr = useMemo(() => {
    const krwRate = (t: PortfolioTx) => (t.currency === "KRW" ? 1 : fxByKey.get(t.account + "|" + t.symbol));
    const totals = new Map<string, number>();
    for (const t of dividendTxns) {
      const r = krwRate(t);
      if (r == null || t.tradedAt.slice(0, 4) === currentYear) continue;
      const y = t.tradedAt.slice(0, 4);
      totals.set(y, (totals.get(y) ?? 0) + t.amount * r);
    }
    const years = Array.from(totals.keys()).sort();
    if (years.length < 2) return null;
    const first = totals.get(years[0]) ?? 0;
    const last = totals.get(years[years.length - 1]) ?? 0;
    if (first <= 0) return null;
    return (Math.pow(last / first, 1 / (years.length - 1)) - 1) * 100;
  }, [dividendTxns, fxByKey, currentYear]);

  async function saveRate(holdingId: number, value: number | null) {
    await fetch(`/api/portfolio/holdings/${holdingId}/dividend`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ annualDividendPct: value }),
    });
    reload();
  }

  async function setTracked(holdingId: number, tracked: boolean) {
    setTrackOverride((o) => ({ ...o, [holdingId]: tracked }));
    try {
      const res = await fetch(`/api/portfolio/holdings/${holdingId}/tracking`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ tracked }),
      });
      if (!res.ok) throw new Error();
    } catch {
      setTrackOverride((o) => ({ ...o, [holdingId]: !tracked }));
    }
  }

  const linked = (data ?? []).filter((p) => p.holdingId != null);
  const tracked = linked.filter(isTracked);
  const candidates = linked.filter((p) => !isTracked(p));
  const unlinked = (data ?? []).length - linked.length;
  const annual = tracked.reduce((s, p) => s + (p.evalKrw * (p.annualDividendPct ?? 0)) / 100, 0);
  const paged = usePaged(tracked, "", 30);
  const cols = editable ? 8 : 7;

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">배당 정보를 불러오지 못했습니다.</p>;
  if (!data) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2 flex-wrap">
        <span className="text-sm text-slate-400">보기 대상</span>
        <ScopeSelector />
        {editable && (
          <button onClick={() => setPicking(true)} className={cx.btnSecondary + " ml-auto"}>
            ＋ 종목 추가
          </button>
        )}
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400">월 배당 예상</div>
          <div className="text-lg font-semibold tabular-nums text-amber-300">{won(Math.round(annual / 12))}원</div>
        </div>
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400">연 배당 예상</div>
          <div className="text-lg font-semibold tabular-nums text-amber-300">{won(Math.round(annual))}원</div>
        </div>
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400">연평균 배당 상승률</div>
          <div
            className={
              "text-lg font-semibold tabular-nums " +
              (dividendCagr == null ? "text-slate-500" : dividendCagr >= 0 ? "text-rose-300" : "text-sky-300")
            }
          >
            {dividendCagr == null ? "—" : (dividendCagr >= 0 ? "+" : "") + Math.round(dividendCagr * 10) / 10 + "%"}
          </div>
        </div>
      </div>

      {dividendGroups.length > 0 &&
        (() => {
          const sel = dividendGroups.find((g) => g.key === dividendCur) ?? dividendGroups[0];
          return (
            <div className="bg-white/5 rounded-xl p-4">
              <div className="flex items-center justify-between gap-2 mb-3 flex-wrap">
                <div className="text-sm font-medium text-slate-300">월별 배당 — 연도별 비교</div>
                {dividendGroups.length > 1 && (
                  <div className="flex gap-1 flex-wrap">
                    {dividendGroups.map((g) => (
                      <button
                        key={g.key}
                        onClick={() => setDividendCur(g.key)}
                        className={
                          "px-2.5 py-1 rounded-md text-xs font-medium transition " +
                          (g.key === sel.key ? "bg-indigo-600/30 text-indigo-200 border border-indigo-500/40" : "text-slate-400 hover:text-white hover:bg-white/5")
                        }
                      >
                        {g.key}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <MonthLines series={sel.series} cur={sel.cur} />
            </div>
          );
        })()}

      <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>계좌</th>
              <th className={cx.table.th + " text-right"}>평가액(원)</th>
              <th className={cx.table.th + " text-right"}>연배당률(%)</th>
              <th className={cx.table.th + " text-right"}>예상 연배당</th>
              <th className={cx.table.th + " text-right"}>예상 월배당</th>
              <th className={cx.table.th + " text-right"}>최근 배당</th>
              {editable && <th className={cx.table.th + " text-right"}></th>}
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.map((p) => {
              const rate = p.annualDividendPct ?? 0;
              const yrNat = p.curPriceNat * p.quantity * (rate / 100);
              const rec = recOf(p);
              const last = rec ? rec.txns.reduce((a, b) => (a.tradedAt >= b.tradedAt ? a : b)) : null;
              return (
                <tr key={p.symbol + p.account} className={cx.table.tr}>
                  <td className={cx.table.td + " text-white font-medium"}>
                    {p.symbol}
                    {p.currency !== "KRW" && <span className="ml-1 text-xs text-slate-500">{p.currency}</span>}
                  </td>
                  <td className={cx.table.td + " whitespace-nowrap"}>{p.account}</td>
                  <td className={cx.table.td + " text-right tabular-nums text-white"}>{won(p.evalKrw)}</td>
                  <td className={cx.table.td + " text-right tabular-nums"}>
                    {editable ? (
                      <button onClick={() => setRateEdit(p)} className="text-white hover:text-indigo-300 transition">
                        {p.annualDividendPct != null ? (
                          p.annualDividendPct + "%"
                        ) : (
                          <span className="text-slate-400 underline decoration-dotted underline-offset-2">설정</span>
                        )}
                      </button>
                    ) : p.annualDividendPct != null ? (
                      p.annualDividendPct + "%"
                    ) : (
                      "—"
                    )}
                  </td>
                  <td className={cx.table.td + " text-right tabular-nums text-slate-300"}>{rate ? money(yrNat, p.currency) : "—"}</td>
                  <td className={cx.table.td + " text-right tabular-nums text-slate-300"}>{rate ? money(yrNat / 12, p.currency) : "—"}</td>
                  <td className={cx.table.td + " text-right tabular-nums"}>
                    {last ? (
                      <button onClick={() => setDetail(p)} className="text-emerald-300 hover:text-emerald-200">
                        <span className="underline decoration-dotted underline-offset-2">
                          {money(last.amount, p.currency)}
                        </span>
                        <span className="block text-[11px] text-slate-500">{last.tradedAt.slice(2)}</span>
                      </button>
                    ) : (
                      <span className="text-slate-600">—</span>
                    )}
                  </td>
                  {editable && (
                    <td className={cx.table.td + " text-right"}>
                      <button
                        onClick={() => setTracked(p.holdingId as number, false)}
                        className="text-xs text-slate-400 hover:text-rose-300 transition"
                      >
                        제외
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
            {tracked.length === 0 && (
              <tr>
                <td colSpan={cols} className="px-4 py-8 text-center text-sm text-slate-500">
                  {editable ? "‘＋ 종목 추가’로 배당을 추적할 종목을 골라주세요." : "배당 추적 중인 종목이 없습니다."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={paged.page} pageCount={paged.pageCount} from={paged.from} to={paged.to} total={paged.total} onPage={paged.setPage} />

      {unlinked > 0 && (
        <p className="text-xs text-slate-600">종목 연동이 안 된 {unlinked}종목은 배당에 추가할 수 없습니다 (거래 추가에서 시장·티커 연동).</p>
      )}

      {picking && (
        <Modal
          title="배당 추적 종목 추가"
          onClose={() => setPicking(false)}
          footer={
            <button onClick={() => setPicking(false)} className={cx.btnPrimary}>
              완료
            </button>
          }
        >
          {candidates.length === 0 ? (
            <p className="text-sm text-slate-500 py-6 text-center">
              추가할 보유 종목이 없습니다.
              {unlinked > 0 && " 연동 안 된 종목은 거래 추가에서 시장·티커를 먼저 연동하세요."}
            </p>
          ) : (
            <ul className="flex flex-col divide-y divide-white/10">
              {candidates.map((p) => (
                <li key={p.symbol + p.account} className="flex items-center gap-2 py-2.5">
                  <div className="min-w-0">
                    <div className="text-white text-sm font-medium truncate">
                      {p.symbol}
                      {p.currency !== "KRW" && <span className="ml-1 text-xs text-slate-500">{p.currency}</span>}
                    </div>
                    <div className="text-xs text-slate-500">
                      {p.account} · {won(p.evalKrw)}원
                    </div>
                  </div>
                  <button onClick={() => setTracked(p.holdingId as number, true)} className={cx.btnSecondary + " ml-auto"}>
                    추가
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Modal>
      )}

      {detail && (
        <DividendDetailModal position={detail} received={recOf(detail)} onClose={() => setDetail(null)} />
      )}

      {rateEdit && (
        <DividendRateModal
          position={rateEdit}
          received={recOf(rateEdit)}
          onClose={() => setRateEdit(null)}
          onSave={(v) => {
            saveRate(rateEdit.holdingId as number, v);
            setRateEdit(null);
          }}
        />
      )}
    </div>
  );
}

/**
 * 배당률 설정 모달 — 최근 12개월 실적 배당률을 제안하고, 수동 입력·저장한다.
 */
function DividendRateModal({
  position,
  received,
  onClose,
  onSave,
}: {
  position: PortfolioPosition;
  received?: Received;
  onClose: () => void;
  onSave: (value: number | null) => void;
}) {
  const cur = position.currency;
  const nativeEval = position.curPriceNat * position.quantity;
  const { recentNet, suggested } = useMemo(() => {
    const cutoff = new Date();
    cutoff.setFullYear(cutoff.getFullYear() - 1);
    const cutoffStr = cutoff.toISOString().slice(0, 10);
    const net = (received?.txns ?? [])
      .filter((t) => t.tradedAt >= cutoffStr)
      .reduce((s, t) => s + t.amount, 0);
    const y = nativeEval > 0 && net > 0 ? Math.round((net / nativeEval) * 1000) / 10 : null;
    return { recentNet: net, suggested: y };
  }, [received, nativeEval]);
  const [value, setValue] = useState(position.annualDividendPct != null ? String(position.annualDividendPct) : "");

  function save() {
    const v = value.trim() === "" ? null : parseFloat(value);
    if (v !== null && (!isFinite(v) || v < 0)) return;
    onSave(v);
  }

  return (
    <Modal
      title={`배당률 설정 · ${position.symbol}`}
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose} className={cx.btnSecondary}>
            취소
          </button>
          <button onClick={save} className={cx.btnPrimary}>
            저장
          </button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400 mb-1">최근 12개월 실적 배당률</div>
          {suggested != null ? (
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <div className="tabular-nums">
                <span className="text-emerald-300 font-semibold text-lg">{suggested}%</span>
                <span className="text-slate-500 text-xs ml-2">
                  {money(recentNet, cur)} / 평가 {money(nativeEval, cur)}
                </span>
              </div>
              <button onClick={() => setValue(String(suggested))} className={cx.btnSecondary}>
                이 값으로 적용
              </button>
            </div>
          ) : (
            <div className="text-sm text-slate-500">최근 12개월 배당 기록이 없어 계산할 수 없습니다.</div>
          )}
        </div>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-slate-400">연 배당수익률 (%)</span>
          <input
            type="number"
            step="0.01"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="예: 3.5"
            className={cx.input}
          />
        </label>
        <p className="text-xs text-slate-600">
          비우면 배당률 미설정(예상 배당 계산 안 함). 예상 배당은 이 배당률 × 현재 평가액으로 계산됩니다.
        </p>
      </div>
    </Modal>
  );
}

/**
 * 배당 내역 모달 — 한 종목의 배당 수령 내역(날짜·실수령)과 누적 합계를 본다.
 */
function DividendDetailModal({
  position,
  received,
  onClose,
}: {
  position: PortfolioPosition;
  received?: Received;
  onClose: () => void;
}) {
  const cur = position.currency;
  const rows = useMemo(
    () => [...(received?.txns ?? [])].sort((a, b) => b.tradedAt.localeCompare(a.tradedAt)),
    [received]
  );
  const monthLines = useMemo(() => recentYearLines(monthlyByYear(received?.txns ?? [], () => 1)), [received]);
  const last = rows[0];
  const paged = usePaged(rows, "", 10);
  return (
    <Modal
      title={`배당 내역 · ${position.symbol}`}
      onClose={onClose}
      footer={
        <button onClick={onClose} className={cx.btnPrimary}>
          닫기
        </button>
      }
    >
      <div className="text-xs text-slate-500 mb-2">{position.account}</div>
      <div className="grid grid-cols-2 gap-3 mb-3">
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400">최근 수령</div>
          {last ? (
            <div className="tabular-nums">
              <span className="text-emerald-300 font-semibold text-lg">{money(last.amount, cur)}</span>
              <span className="text-slate-500 text-xs ml-1">· {last.tradedAt}</span>
            </div>
          ) : (
            <div className="text-slate-500">—</div>
          )}
        </div>
        <div className="bg-white/5 rounded-lg p-3">
          <div className="text-xs text-slate-400">누적 실수령 ({rows.length}회)</div>
          <div className="text-slate-200 font-semibold tabular-nums text-lg">{money(received?.net ?? 0, cur)}</div>
        </div>
      </div>
      {monthLines.length > 0 && (
        <div className="mb-4">
          <div className="text-xs text-slate-400 mb-2">월별 배당 — 연도별 비교</div>
          <MonthLines series={monthLines} cur={cur} />
        </div>
      )}
      <div className="rounded-lg border border-white/10 overflow-x-auto">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>날짜</th>
              <th className={cx.table.th + " text-right"}>실수령</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.map((t) => (
              <tr key={t.id} className={cx.table.tr}>
                <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{t.tradedAt}</td>
                <td className={cx.table.td + " text-right tabular-nums text-emerald-300"}>{money(t.amount, cur)}</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={2} className="px-4 py-6 text-center text-sm text-slate-500">
                  배당 수령 기록이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="mt-3">
        <Pagination page={paged.page} pageCount={paged.pageCount} from={paged.from} to={paged.to} total={paged.total} onPage={paged.setPage} />
      </div>
    </Modal>
  );
}
