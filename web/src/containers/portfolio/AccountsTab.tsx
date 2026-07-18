"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Modal from "@/components/Modal";
import CommaInput from "@/components/CommaInput";
import { cx } from "@/utils/cx";
import {
  type PortfolioAccount,
  type PortfolioTx,
  type PortfolioPosition,
  type PortfolioFxConversion,
  natMoney,
  CURRENCIES,
} from "@/types/portfolio";
import { won, fxRateOf } from "./usePortfolioData";
import AddTransactionModal from "./AddTransactionModal";
import { usePaged, Pagination } from "@/components/Pagination";
import { useDateRange, DateRangeFilter } from "./DateRange";

/** 거래 1건이 자기 통화 현금에 미치는 영향(백엔드 PortfolioCashCalculator와 동일 규칙). */
function cashEffect(t: PortfolioTx): number {
  const amount = t.amount;
  const fee = t.fee ?? 0;
  switch (t.type) {
    case "DEPOSIT":
    case "DIVIDEND":
    case "INTEREST":
      return amount;
    case "WITHDRAW":
      return -amount;
    case "BUY":
      return -(amount + fee);
    case "SELL":
      return amount - fee;
    default:
      return 0;
  }
}

/**
 * 계좌 탭 — 계좌 목록·생성·삭제 + 입출금(거래) 내역. 잔액 집계는 별도 단계(현재 미표시).
 */
export default function AccountsTab() {
  const [accounts, setAccounts] = useState<PortfolioAccount[] | null>(null);
  const [txns, setTxns] = useState<PortfolioTx[]>([]);
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [convs, setConvs] = useState<PortfolioFxConversion[]>([]);
  const [err, setErr] = useState(false);
  const [showAddAccount, setShowAddAccount] = useState(false);
  const [editingAccount, setEditingAccount] = useState<PortfolioAccount | null>(null);
  const [showCash, setShowCash] = useState(false);
  const [showConv, setShowConv] = useState(false);
  const [tab, setTab] = useState<"accounts" | "flows" | "conversions">("accounts");
  const [valueMode, setValueMode] = useState<"krw" | "native">("krw");
  const dr = useDateRange();
  const router = useRouter();

  // 계좌·입출금 변경 후: 재조회 + 라우터 캐시 무효화(리포트·대시보드 등 최신화).
  function reload() {
    load();
    router.refresh();
  }

  function load() {
    fetch("/api/portfolio/accounts")
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: PortfolioAccount[]) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => setErr(true));
    fetch("/api/portfolio/transactions")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioTx[]) => setTxns(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/positions")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioPosition[]) => setPositions(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/fx-conversions")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioFxConversion[]) => setConvs(Array.isArray(d) ? d : []))
      .catch(() => {});
  }

  useEffect(() => {
    load();
  }, []);

  async function remove(id: number, name: string) {
    if (!confirm(`'${name}' 계좌를 삭제할까요?`)) return;
    await fetch(`/api/portfolio/accounts/${id}`, { method: "DELETE" });
    reload();
  }

  const flows = txns.filter((t) => (t.type === "DEPOSIT" || t.type === "WITHDRAW") && dr.inRange(t.tradedAt));
  const pagedFlows = usePaged(flows, `flows|${dr.key}`, 10);
  const pagedConvs = usePaged(convs, "convs", 10);

  // 계좌별 통화별 예수금(거래 + 환전을 접어 계산).
  const cashByAccount = useMemo(() => {
    const m = new Map<number, Record<string, number>>();
    const bump = (accId: number, cur: string, delta: number) => {
      const rec = m.get(accId) ?? {};
      rec[cur] = (rec[cur] ?? 0) + delta;
      m.set(accId, rec);
    };
    for (const t of txns) {
      if (t.accountId == null) continue;
      bump(t.accountId, t.currency, cashEffect(t));
    }
    for (const c of convs) {
      if (c.accountId == null) continue;
      bump(c.accountId, c.fromCurrency, -(c.fromAmount + (c.fee ?? 0)));
      bump(c.accountId, c.toCurrency, c.toAmount);
    }
    return m;
  }, [txns, convs]);

  // 통화별 현재 환율(보유 포지션의 평가액/원통화평가에서 파생, KRW=1).
  const fxOf = useMemo(() => {
    const m = new Map<string, number>();
    for (const p of positions) {
      const fx = fxRateOf(p);
      if (fx != null && !m.has(p.currency)) m.set(p.currency, fx);
    }
    return (cur: string) => (cur === "KRW" ? 1 : m.get(cur) ?? 1);
  }, [positions]);

  // 계좌 전체 가치 — 현금 + 보유 평가. 통화별(원통화)과 오늘 환율 원화 환산을 함께.
  const valueByAccount = useMemo(() => {
    const m = new Map<number, { nativeByCur: Record<string, number>; krw: number }>();
    for (const a of accounts ?? []) {
      const nativeByCur: Record<string, number> = { ...(cashByAccount.get(a.id) ?? {}) };
      let krw = 0;
      for (const [cur, bal] of Object.entries(nativeByCur)) krw += bal * fxOf(cur);
      for (const p of positions) {
        if (p.account !== a.name) continue;
        nativeByCur[p.currency] = (nativeByCur[p.currency] ?? 0) + p.curPriceNat * p.quantity;
        krw += p.evalKrw;
      }
      m.set(a.id, { nativeByCur, krw });
    }
    return m;
  }, [accounts, positions, cashByAccount, fxOf]);

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">계좌를 불러오지 못했습니다.</p>;
  if (!accounts) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-1 border-b border-white/10">
        {([["accounts", "계좌"], ["flows", "입출금"], ["conversions", "환전"]] as const).map(([k, label]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={
              "px-4 py-3 text-sm font-medium border-b-2 -mb-px transition cursor-pointer " +
              (tab === k ? "border-indigo-400 text-indigo-300" : "border-transparent text-slate-400 hover:text-white")
            }
          >
            {label}
          </button>
        ))}
      </div>

      {tab === "accounts" && (
      <div className="flex flex-col gap-6">
      <section>
        <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
          <h2 className="text-sm font-medium text-slate-300">계좌 관리</h2>
          <div className="flex items-center gap-2">
            <div className="flex gap-1">
              {([["krw", "원화"], ["native", "원통화"]] as const).map(([k, label]) => (
                <button
                  key={k}
                  onClick={() => setValueMode(k)}
                  className={
                    "px-2.5 py-1 rounded-md text-xs font-medium transition " +
                    (valueMode === k ? "bg-indigo-600/30 text-indigo-200 border border-indigo-500/40" : "text-slate-400 hover:text-white hover:bg-white/5")
                  }
                >
                  {label}
                </button>
              ))}
            </div>
            <button className={cx.btnPrimary} onClick={() => setShowAddAccount(true)}>
              ＋ 계좌 추가
            </button>
          </div>
        </div>
        {accounts.length === 0 ? (
          <p className="text-sm text-slate-500 py-8 text-center rounded-xl border border-white/10 bg-slate-900/40">
            계좌가 없습니다. 먼저 계좌를 추가하세요.
          </p>
        ) : (
          <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
            <table className={cx.table.root}>
              <thead className={cx.table.head}>
                <tr>
                  <th className={cx.table.th}>계좌</th>
                  <th className={cx.table.th}>증권사</th>
                  <th className={cx.table.th + " text-right"}>{valueMode === "krw" ? "평가액(원)" : "평가액(원통화)"}</th>
                  <th className={cx.table.th + " text-right"}></th>
                </tr>
              </thead>
              <tbody className={cx.table.body}>
                {accounts.map((a) => {
                  const v = valueByAccount.get(a.id) ?? { nativeByCur: {}, krw: 0 };
                  const curs = Object.entries(v.nativeByCur).filter(([, b]) => Math.round(b) !== 0);
                  return (
                    <tr key={a.id} className={cx.table.tr}>
                      <td className={cx.table.td + " text-white font-medium whitespace-nowrap"}>{a.name}</td>
                      <td className={cx.table.td + " text-slate-400"}>
                        {a.brokerName ?? "—"}
                        {a.description ? <span className="text-slate-600"> · {a.description}</span> : ""}
                      </td>
                      <td className={cx.table.td + " text-right tabular-nums"}>
                        {valueMode === "krw" ? (
                          <span className={v.krw < 0 ? "text-rose-300" : "text-white"}>{won(Math.round(v.krw))}원</span>
                        ) : curs.length === 0 ? (
                          <span className="text-slate-600">—</span>
                        ) : (
                          curs.map(([cur, bal]) => (
                            <div key={cur} className={bal < 0 ? "text-rose-300" : "text-white"}>
                              {natMoney(bal, cur)}
                            </div>
                          ))
                        )}
                      </td>
                      <td className={cx.table.td + " text-right whitespace-nowrap"}>
                        <button className="text-xs text-slate-400 hover:text-white transition" onClick={() => setEditingAccount(a)}>
                          수정
                        </button>
                        <button className="text-xs text-slate-500 hover:text-rose-300 transition ml-3" onClick={() => remove(a.id, a.name)}>
                          삭제
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
      </div>
      )}

      {tab === "flows" && (
      <section>
        <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
          <h2 className="text-sm font-medium text-slate-300">입출금 내역</h2>
          <div className="flex items-center gap-2 flex-wrap">
            <DateRangeFilter from={dr.from} to={dr.to} setFrom={dr.setFrom} setTo={dr.setTo} />
            <button className={cx.btnSecondary} onClick={() => setShowCash(true)} disabled={accounts.length === 0}>
              ＋ 입출금
            </button>
          </div>
        </div>
        <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
          <table className={cx.table.root}>
            <thead className={cx.table.head}>
              <tr>
                <th className={cx.table.th}>일시</th>
                <th className={cx.table.th}>계좌</th>
                <th className={cx.table.th}>구분</th>
                <th className={cx.table.th + " text-right"}>금액(원)</th>
                <th className={cx.table.th}>메모</th>
              </tr>
            </thead>
            <tbody className={cx.table.body}>
              {pagedFlows.rows.map((f) => (
                <tr key={f.id} className={cx.table.tr}>
                  <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{f.tradedAt.slice(5)}</td>
                  <td className={cx.table.td}>{f.account}</td>
                  <td className={cx.table.td}>
                    <span
                      className={
                        "inline-block rounded-full px-2 py-0.5 text-xs " +
                        (f.type === "DEPOSIT" ? "text-emerald-300 bg-emerald-500/10" : "text-sky-300 bg-sky-500/10")
                      }
                    >
                      {f.type === "DEPOSIT" ? "입금" : "출금"}
                    </span>
                  </td>
                  <td className={cx.table.td + " text-right tabular-nums text-white"}>{won(f.amount)}</td>
                  <td className={cx.table.td + " text-slate-500"}>{f.memo ?? ""}</td>
                </tr>
              ))}
              {flows.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-500">
                    입출금 내역이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="mt-2">
          <Pagination page={pagedFlows.page} pageCount={pagedFlows.pageCount} from={pagedFlows.from} to={pagedFlows.to} total={pagedFlows.total} onPage={pagedFlows.setPage} />
        </div>
      </section>
      )}

      {tab === "conversions" && (
      <section>
        <div className="flex items-center justify-between mb-3 gap-2 flex-wrap">
          <div>
            <h2 className="text-sm font-medium text-slate-300">환전 내역</h2>
            <p className="text-xs text-slate-500 mt-0.5">통화 전환(예: 원화→달러). 순납입엔 잡히지 않고 통화별 현금만 이동합니다.</p>
          </div>
          <button className={cx.btnSecondary} onClick={() => setShowConv(true)} disabled={accounts.length === 0}>
            ＋ 환전
          </button>
        </div>
        <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
          <table className={cx.table.root}>
            <thead className={cx.table.head}>
              <tr>
                <th className={cx.table.th}>일자</th>
                <th className={cx.table.th}>계좌</th>
                <th className={cx.table.th + " text-right"}>보낸 금액</th>
                <th className={cx.table.th + " text-right"}>받은 금액</th>
                <th className={cx.table.th + " text-right"}>환율</th>
                <th className={cx.table.th}></th>
              </tr>
            </thead>
            <tbody className={cx.table.body}>
              {pagedConvs.rows.map((c) => (
                <tr key={c.id} className={cx.table.tr}>
                  <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{c.convDate.slice(2)}</td>
                  <td className={cx.table.td}>{c.account}</td>
                  <td className={cx.table.td + " text-right tabular-nums text-sky-300"}>
                    −{natMoney(c.fromAmount, c.fromCurrency)}
                  </td>
                  <td className={cx.table.td + " text-right tabular-nums text-emerald-300"}>
                    +{natMoney(c.toAmount, c.toCurrency)}
                  </td>
                  <td className={cx.table.td + " text-right tabular-nums text-slate-400"}>
                    {c.toAmount > 0 ? (c.fromAmount / c.toAmount).toFixed(2) : "—"}
                  </td>
                  <td className={cx.table.td + " text-right"}>
                    <button
                      onClick={async () => {
                        if (!confirm("이 환전을 삭제할까요?")) return;
                        await fetch(`/api/portfolio/fx-conversions/${c.id}`, { method: "DELETE" });
                        reload();
                      }}
                      className="text-xs text-slate-400 hover:text-rose-300 transition"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
              {convs.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-500">
                    환전 내역이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="mt-2">
          <Pagination page={pagedConvs.page} pageCount={pagedConvs.pageCount} from={pagedConvs.from} to={pagedConvs.to} total={pagedConvs.total} onPage={pagedConvs.setPage} />
        </div>
      </section>
      )}

      {showAddAccount && (
        <AddAccountModal
          onClose={() => setShowAddAccount(false)}
          onSaved={() => {
            setShowAddAccount(false);
            reload();
          }}
        />
      )}
      {editingAccount && (
        <AddAccountModal
          initial={editingAccount}
          onClose={() => setEditingAccount(null)}
          onSaved={() => {
            setEditingAccount(null);
            reload();
          }}
        />
      )}
      {showCash && (
        <AddTransactionModal
          accounts={accounts}
          defaultType="DEPOSIT"
          onClose={() => setShowCash(false)}
          onSaved={() => {
            setShowCash(false);
            reload();
          }}
        />
      )}
      {showConv && (
        <AddFxConversionModal
          accounts={accounts}
          onClose={() => setShowConv(false)}
          onSaved={() => {
            setShowConv(false);
            reload();
          }}
        />
      )}
    </div>
  );
}

/**
 * 환전 추가 모달.
 *
 * @param accounts 계좌 목록
 * @param onClose  닫기
 * @param onSaved  저장 성공 후 콜백
 */
function AddFxConversionModal({
  accounts,
  onClose,
  onSaved,
}: {
  accounts: PortfolioAccount[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [accountId, setAccountId] = useState<number>(accounts[0]?.id ?? 0);
  const [convDate, setConvDate] = useState(new Date().toISOString().slice(0, 10));
  const [fromCurrency, setFromCurrency] = useState("KRW");
  const [fromAmount, setFromAmount] = useState("");
  const [toCurrency, setToCurrency] = useState("USD");
  const [toAmount, setToAmount] = useState("");
  const [fee, setFee] = useState("");
  const [memo, setMemo] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const rate = Number(fromAmount) > 0 && Number(toAmount) > 0 ? Number(fromAmount) / Number(toAmount) : null;

  async function submit() {
    if (!accountId || !fromAmount || !toAmount) {
      setError("계좌·보낸 금액·받은 금액을 입력하세요.");
      return;
    }
    if (fromCurrency === toCurrency) {
      setError("보낸 통화와 받은 통화가 같습니다.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const res = await fetch("/api/portfolio/fx-conversions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          accountId,
          convDate,
          fromCurrency,
          fromAmount: parseFloat(fromAmount),
          toCurrency,
          toAmount: parseFloat(toAmount),
          fee: fee ? parseInt(fee, 10) : 0,
          memo: memo.trim() || null,
        }),
      });
      if (!res.ok) throw new Error();
      onSaved();
    } catch {
      setError("저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="환전 추가"
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose} className={cx.btnSecondary}>
            취소
          </button>
          <button onClick={submit} disabled={saving} className={cx.btnPrimary}>
            {saving ? "저장 중…" : "저장"}
          </button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">계좌</span>
          <select value={accountId} onChange={(e) => setAccountId(Number(e.target.value))} className={cx.select}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">환전 일자</span>
          <input type="date" value={convDate} onChange={(e) => setConvDate(e.target.value)} className={cx.inputDate} />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">보낸 통화</span>
            <select value={fromCurrency} onChange={(e) => setFromCurrency(e.target.value)} className={cx.select}>
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">보낸 금액</span>
            <CommaInput decimal value={fromAmount} onChange={setFromAmount} className={cx.inputNumber} />
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">받은 통화</span>
            <select value={toCurrency} onChange={(e) => setToCurrency(e.target.value)} className={cx.select}>
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">받은 금액</span>
            <CommaInput decimal value={toAmount} onChange={setToAmount} className={cx.inputNumber} />
          </label>
        </div>
        {rate && <div className="text-xs text-slate-500">적용 환율 ≈ {rate.toFixed(2)} {fromCurrency}/{toCurrency}</div>}
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">수수료 (보낸 통화, 선택)</span>
          <CommaInput value={fee} onChange={setFee} placeholder="0" className={cx.inputNumber} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">메모 (선택)</span>
          <input value={memo} onChange={(e) => setMemo(e.target.value)} className={cx.input} />
        </label>
        {error && <p className="text-xs text-rose-300">{error}</p>}
      </div>
    </Modal>
  );
}

/**
 * 계좌 추가/수정 모달.
 *
 * @param initial 수정 대상 계좌(없으면 신규)
 * @param onClose 닫기
 * @param onSaved 저장 성공 후 콜백(목록 갱신용)
 */
function AddAccountModal({
  initial,
  onClose,
  onSaved,
}: {
  initial?: PortfolioAccount;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!initial;
  const [name, setName] = useState(initial?.name ?? "");
  const [brokerName, setBrokerName] = useState(initial?.brokerName ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!name.trim()) {
      setError("계좌명을 입력하세요.");
      return;
    }
    setSaving(true);
    setError(null);
    const body = JSON.stringify({
      name: name.trim(),
      brokerName: brokerName.trim() || null,
      description: description.trim() || null,
    });
    const res = isEdit
      ? await fetch(`/api/portfolio/accounts/${initial!.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body,
        })
      : await fetch("/api/portfolio/accounts", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body,
        });
    if (res.ok) {
      onSaved();
    } else {
      setError(res.status === 409 ? "이미 같은 이름의 계좌가 있습니다." : "저장에 실패했습니다.");
      setSaving(false);
    }
  }

  return (
    <Modal
      title={isEdit ? "계좌 수정" : "계좌 추가"}
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose} className={cx.btnSecondary} disabled={saving}>
            취소
          </button>
          <button onClick={submit} className={cx.btnPrimary} disabled={saving}>
            {saving ? "저장 중…" : "저장"}
          </button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">계좌명</span>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="예: 장투" className={cx.input} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">증권사</span>
          <input value={brokerName} onChange={(e) => setBrokerName(e.target.value)} placeholder="예: 미래에셋" className={cx.input} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">설명</span>
          <input value={description} onChange={(e) => setDescription(e.target.value)} className={cx.input} />
        </label>
        {error && <p className="text-sm text-rose-300">{error}</p>}
      </div>
    </Modal>
  );
}
