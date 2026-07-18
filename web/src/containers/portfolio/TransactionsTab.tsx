"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { cx } from "@/utils/cx";
import { type PortfolioTx, type PortfolioAccount, type PortfolioShare, TX_TYPE_LABEL, CUR_SYMBOL, natMoney } from "@/types/portfolio";
import AddTransactionModal from "./AddTransactionModal";
import { useScope, scopeBase, ScopeSelector } from "./scopeView";
import { usePaged, Pagination } from "./Pagination";
import { useDateRange, DateRangeFilter } from "./DateRange";
import { won } from "./usePortfolioData";

function priceText(tx: PortfolioTx): string {
  if (tx.price == null) return "—";
  return tx.currency === "KRW" ? won(tx.price) : (CUR_SYMBOL[tx.currency] ?? "") + won(tx.price);
}

const TYPE_CHIP: Record<string, string> = {
  BUY: "text-rose-300 bg-rose-500/10",
  SELL: "text-sky-300 bg-sky-500/10",
  DIVIDEND: "text-amber-300 bg-amber-500/10",
  INTEREST: "text-amber-300 bg-amber-500/10",
  DEPOSIT: "text-emerald-300 bg-emerald-500/10",
  WITHDRAW: "text-slate-300 bg-white/5",
};

/**
 * 매매내역 탭 — 계좌·종목으로 걸러 거래 이력을 본다.
 */
export default function TransactionsTab() {
  const [scope] = useScope();
  const [rows, setRows] = useState<PortfolioTx[] | null>(null);
  const [accounts, setAccounts] = useState<PortfolioAccount[]>([]);
  const [shares, setShares] = useState<PortfolioShare[]>([]);
  const [err, setErr] = useState(false);
  const [sym, setSym] = useState("");
  const [acct, setAcct] = useState("");
  const dr = useDateRange();
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState<PortfolioTx | null>(null);
  const router = useRouter();

  const scopeId = scope === "own" ? null : Number(scope);
  const isOwnScope = scope === "own" || (scopeId != null && accounts.some((a) => a.id === scopeId));
  const sharedPerm = scopeId != null ? shares.find((s) => s.accountId === scopeId)?.permission : undefined;
  const writable = isOwnScope || sharedPerm === "WRITE";
  const scopeAccountName =
    scopeId == null ? null : accounts.find((a) => a.id === scopeId)?.name
      ?? shares.find((s) => s.accountId === scopeId)?.accountName ?? "계좌";
  // 특정 계좌 대상이면 그 계좌로 고정, 내 전체면 내 계좌 목록에서 선택.
  const modalAccounts = scopeId == null ? accounts : [{ id: scopeId, name: scopeAccountName as string }];
  const createUrl = isOwnScope ? "/api/portfolio/transactions" : `/api/portfolio/shared/${scope}/transactions`;

  // 거래 변경 후: 이 탭 재조회 + 라우터 캐시 무효화(리포트·대시보드 등 다른 화면 최신화).
  function reload() {
    loadTx();
    router.refresh();
  }

  async function del(id: number) {
    if (!confirm("이 거래를 삭제할까요?")) return;
    await fetch(`/api/portfolio/transactions/${id}`, { method: "DELETE" });
    reload();
  }

  function loadTx() {
    fetch(`${scopeBase(scope)}/transactions`, { cache: "no-store" })
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: PortfolioTx[]) => {
        setRows(d);
        setErr(false);
      })
      .catch(() => setErr(true));
  }

  useEffect(() => {
    loadTx();
  }, [scope]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    fetch("/api/portfolio/accounts")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioAccount[]) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/shares")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioShare[]) => setShares(Array.isArray(d) ? d.filter((s) => s.direction === "IN") : []))
      .catch(() => {});
  }, []);

  const accountNames = useMemo(() => (rows ? Array.from(new Set(rows.map((r) => r.account))) : []), [rows]);
  const symbols = useMemo(
    () => (rows ? Array.from(new Set(rows.filter((r) => r.symbol).map((r) => r.symbol as string))) : []),
    [rows]
  );

  const q = sym.trim().toLowerCase();
  const filtered = useMemo(
    () =>
      (rows ?? []).filter((r) => {
        const okS = !q || (!!r.symbol && r.symbol.toLowerCase().includes(q));
        const okA = !acct || r.account === acct;
        return okS && okA && dr.inRange(r.tradedAt);
      }),
    [rows, q, acct, dr]
  );

  const summary = useMemo(() => {
    if (!q && !acct) return null;
    const buy = filtered.filter((r) => r.type === "BUY").reduce((s, r) => s + r.amount, 0);
    const sell = filtered.filter((r) => r.type === "SELL").reduce((s, r) => s + r.amount, 0);
    const seenSym = Array.from(new Set(filtered.filter((r) => r.symbol).map((r) => r.symbol)));
    const label = seenSym.length === 1 ? seenSym[0] : q ? `"${sym}"` : "전체 종목";
    return { label, count: filtered.length, buy, sell, acct };
  }, [filtered, q, acct, sym]);

  const paged = usePaged(filtered, `${q}|${acct}|${scope}|${dr.key}`, 10);

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">거래 내역을 불러오지 못했습니다.</p>;
  if (!rows) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <ScopeSelector />
        <input
          list="portfolio-tx-symbols"
          value={sym}
          onChange={(e) => setSym(e.target.value)}
          placeholder="종목 검색 (비우면 전체)"
          className={cx.input + " sm:w-56"}
        />
        <datalist id="portfolio-tx-symbols">
          {symbols.map((s) => (
            <option key={s} value={s} />
          ))}
        </datalist>
        <select value={acct} onChange={(e) => setAcct(e.target.value)} className={cx.select}>
          <option value="">전체 계좌</option>
          {accountNames.map((a) => (
            <option key={a} value={a}>
              {a}
            </option>
          ))}
        </select>
        <DateRangeFilter from={dr.from} to={dr.to} setFrom={dr.setFrom} setTo={dr.setTo} />
        {writable && (
          <button onClick={() => setShowAdd(true)} className={cx.btnPrimary + " ml-auto"}>
            ＋ 거래 추가
          </button>
        )}
      </div>

      {summary && (
        <div className="rounded-lg bg-indigo-600/15 border border-indigo-500/25 px-4 py-2.5 text-sm text-slate-200">
          <span className="font-medium text-white">{summary.label}</span>
          {summary.acct && <span className="text-indigo-300"> · {summary.acct} 계좌</span>}
          <span className="text-slate-400"> · {summary.count}건</span>
          {summary.buy > 0 && <span className="text-rose-300"> · 매수 {won(summary.buy)}</span>}
          {summary.sell > 0 && <span className="text-sky-300"> · 매도 {won(summary.sell)}</span>}
        </div>
      )}

      <div className="rounded-xl border border-white/10 bg-slate-900/40 overflow-x-auto">
        <table className={cx.table.root}>
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>일시</th>
              <th className={cx.table.th}>유형</th>
              <th className={cx.table.th}>종목</th>
              <th className={cx.table.th}>계좌</th>
              <th className={cx.table.th}>태그</th>
              <th className={cx.table.th + " text-right"}>수량</th>
              <th className={cx.table.th + " text-right"}>단가</th>
              <th className={cx.table.th + " text-right"}>금액</th>
              <th className={cx.table.th}>메모</th>
              <th className={cx.table.th + " text-right"}>관리</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.map((r) => (
              <tr key={r.id} className={cx.table.tr}>
                <td className={cx.table.td + " tabular-nums whitespace-nowrap"}>{r.tradedAt.slice(5)}</td>
                <td className={cx.table.td}>
                  <span className={"inline-block rounded-full px-2 py-0.5 text-xs " + (TYPE_CHIP[r.type] ?? "")}>
                    {TX_TYPE_LABEL[r.type]}
                  </span>
                </td>
                <td className={cx.table.td + " text-white font-medium"}>
                  {r.symbol ?? "—"}
                  {r.symbol && r.currency !== "KRW" && (
                    <span className="ml-1 text-xs text-slate-500">{r.currency}</span>
                  )}
                </td>
                <td className={cx.table.td}>{r.account}</td>
                <td className={cx.table.td}>
                  {r.tag ? (
                    <span className="inline-block rounded-full bg-white/5 px-2 py-0.5 text-xs text-slate-300">{r.tag}</span>
                  ) : (
                    "—"
                  )}
                </td>
                <td className={cx.table.td + " text-right tabular-nums"}>{r.quantity ?? "—"}</td>
                <td className={cx.table.td + " text-right tabular-nums"}>{priceText(r)}</td>
                <td className={cx.table.td + " text-right tabular-nums text-white"}>{natMoney(r.amount, r.currency)}</td>
                <td className={cx.table.td + " text-slate-500"}>{r.memo ?? ""}</td>
                <td className={cx.table.td + " text-right whitespace-nowrap"}>
                  {isOwnScope ? (
                    <>
                      <button onClick={() => setEditing(r)} className="text-xs text-slate-400 hover:text-white transition mr-2">
                        수정
                      </button>
                      <button onClick={() => del(r.id)} className="text-xs text-slate-500 hover:text-rose-300 transition">
                        삭제
                      </button>
                    </>
                  ) : (
                    <span className="text-xs text-slate-600">—</span>
                  )}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-sm text-slate-500">
                  조건에 맞는 거래가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={paged.page} pageCount={paged.pageCount} from={paged.from} to={paged.to} total={paged.total} onPage={paged.setPage} />

      {showAdd && (
        <AddTransactionModal
          accounts={modalAccounts}
          createUrl={createUrl}
          holdingLink={isOwnScope}
          onClose={() => setShowAdd(false)}
          onSaved={() => {
            setShowAdd(false);
            reload();
          }}
        />
      )}
      {editing && (
        <AddTransactionModal
          accounts={accounts}
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            reload();
          }}
        />
      )}
    </div>
  );
}
