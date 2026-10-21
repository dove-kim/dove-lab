"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Select from "@/components/Select";
import Panel from "@/components/Panel";
import { useConfirm } from "@/components/ConfirmProvider";
import { usePaged, Pagination } from "@/components/Pagination";
import { cx } from "@/utils/cx";
import {
  type PortfolioAccount,
  type PortfolioFxConversion,
  type PortfolioShare,
  type PortfolioTx,
  CUR_SYMBOL,
  natMoney,
} from "@/types/portfolio";
import { useScope, scopeBase } from "../scopeView";
import { useDateRange, DateRangeFilter } from "../DateRange";
import PortfolioHeader from "../PortfolioHeader";
import { won } from "../usePortfolioData";
import QuickEntryModal from "./QuickEntryModal";
import GuidedEntryModal from "./GuidedEntryModal";
import { type LedgerEntry, type LedgerGroup, ledgerDateText, toLedgerEntries } from "./ledgerEntries";

const GROUP_FILTERS: { value: "" | LedgerGroup; label: string }[] = [
  { value: "", label: "전체" },
  { value: "trade", label: "매매" },
  { value: "cash", label: "입출금" },
  { value: "fx", label: "환전" },
  { value: "income", label: "배당·이자" },
];

const LABEL_CHIP: Record<string, string> = {
  매수: "text-rose-300 bg-rose-500/10",
  매도: "text-sky-300 bg-sky-500/10",
  배당: "text-amber-300 bg-amber-500/10",
  이자: "text-amber-300 bg-amber-500/10",
  입금: "text-emerald-300 bg-emerald-500/10",
  출금: "text-slate-300 bg-white/5",
  환전: "text-indigo-300 bg-indigo-500/10",
};

/** 단가 표시(원통화 심볼 포함). */
function priceText(entry: LedgerEntry): string {
  if (entry.price == null) return "—";
  return entry.currency === "KRW" ? won(entry.price) : (CUR_SYMBOL[entry.currency] ?? "") + won(entry.price);
}

/**
 * 기록 화면 — 매매·입출금·환전·배당을 한 타임라인으로 보고 추가·수정·삭제한다.
 */
export default function LedgerView() {
  const confirm = useConfirm();
  const router = useRouter();
  const [scope] = useScope();
  const [txns, setTxns] = useState<PortfolioTx[] | null>(null);
  const [convs, setConvs] = useState<PortfolioFxConversion[]>([]);
  const [accounts, setAccounts] = useState<PortfolioAccount[]>([]);
  const [shares, setShares] = useState<PortfolioShare[]>([]);
  const [err, setErr] = useState(false);
  const [group, setGroup] = useState<"" | LedgerGroup>("");
  const [sym, setSym] = useState("");
  const [acct, setAcct] = useState("");
  const dr = useDateRange();
  const [adding, setAdding] = useState(false);
  const [guiding, setGuiding] = useState(false);
  const [editingTx, setEditingTx] = useState<PortfolioTx | null>(null);
  const [editingFx, setEditingFx] = useState<PortfolioFxConversion | null>(null);

  const scopeId = scope === "own" ? null : Number(scope);
  const isOwnScope = scope === "own" || (scopeId != null && accounts.some((a) => a.id === scopeId));
  const sharedPerm = scopeId != null ? shares.find((s) => s.accountId === scopeId)?.permission : undefined;
  const writable = isOwnScope || sharedPerm === "WRITE";
  const scopeAccountName =
    scopeId == null
      ? null
      : accounts.find((a) => a.id === scopeId)?.name ?? shares.find((s) => s.accountId === scopeId)?.accountName ?? "계좌";
  const modalAccounts = scopeId == null ? accounts : [{ id: scopeId, name: scopeAccountName as string }];
  const createUrl = isOwnScope ? "/api/portfolio/transactions" : `/api/portfolio/shared/${scope}/transactions`;

  const loadTx = useCallback(() => {
    fetch(`${scopeBase(scope)}/transactions`, { cache: "no-store" })
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: PortfolioTx[]) => {
        setTxns(d);
        setErr(false);
      })
      .catch(() => setErr(true));
  }, [scope]);

  const loadFx = useCallback(() => {
    fetch("/api/portfolio/fx-conversions", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioFxConversion[]) => setConvs(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  // 기록 변경은 현황·성과에도 반영되므로 라우터 캐시까지 무효화한다.
  function reload() {
    loadTx();
    loadFx();
    router.refresh();
  }

  useEffect(() => {
    loadTx();
  }, [loadTx]);

  useEffect(() => {
    loadFx();
    fetch("/api/portfolio/accounts")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioAccount[]) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/shares")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioShare[]) => setShares(Array.isArray(d) ? d.filter((s) => s.direction === "IN") : []))
      .catch(() => {});
  }, [loadFx]);

  // 환전은 내 계좌 것만 존재한다. 특정 계좌를 보고 있으면 그 계좌 것만 남긴다.
  const scopedConvs = useMemo(() => {
    if (scopeId == null) return convs;
    if (!isOwnScope) return [];
    return convs.filter((c) => c.accountId === scopeId);
  }, [convs, scopeId, isOwnScope]);

  const entries = useMemo(() => toLedgerEntries(txns ?? [], scopedConvs), [txns, scopedConvs]);
  const accountNames = useMemo(() => Array.from(new Set(entries.map((e) => e.account))), [entries]);
  // 최근 거래 순 종목명 — 빠른 입력의 종목 칩 정렬에 쓴다(entries는 일자 내림차순).
  const symbols = useMemo(
    () => Array.from(new Set(entries.filter((e) => e.group === "trade").map((e) => e.title))),
    [entries]
  );

  const q = sym.trim().toLowerCase();
  const filtered = useMemo(
    () =>
      entries.filter((e) => {
        if (group && e.group !== group) return false;
        if (q && !e.title.toLowerCase().includes(q)) return false;
        if (acct && e.account !== acct) return false;
        return dr.inRange(e.date);
      }),
    [entries, group, q, acct, dr]
  );

  const paged = usePaged(filtered, `${group}|${q}|${acct}|${scope}|${dr.key}`, 15);
  const thisYear = useMemo(() => new Date().getFullYear(), []);

  async function delTx(id: number) {
    const ok = await confirm({
      title: "거래 삭제",
      description: "이 거래를 삭제할까요? 보유수량과 평단이 다시 계산됩니다.",
      confirmText: "삭제",
      danger: true,
    });
    if (!ok) return;
    await fetch(`/api/portfolio/transactions/${id}`, { method: "DELETE" });
    reload();
  }

  async function delFx(id: number) {
    const ok = await confirm({
      title: "환전 삭제",
      description: "이 환전 기록을 삭제할까요?",
      confirmText: "삭제",
      danger: true,
    });
    if (!ok) return;
    await fetch(`/api/portfolio/fx-conversions/${id}`, { method: "DELETE" });
    reload();
  }

  if (err) return <p className="py-8 text-center text-sm text-rose-300">기록을 불러오지 못했습니다.</p>;
  if (!txns) return <p className="py-8 text-center text-sm text-slate-500">불러오는 중…</p>;

  return (
    <>
      <PortfolioHeader title="기록">
        {writable && (
          <div className="ml-auto flex flex-wrap gap-2">
            <button onClick={() => setGuiding(true)} className={cx.btnPrimary}>
              차근차근 기록하기
            </button>
            <button onClick={() => setAdding(true)} className={cx.btnSecondary}>
              ＋ 기록 추가
            </button>
          </div>
        )}
      </PortfolioHeader>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2">
          {GROUP_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setGroup(f.value)}
              className={group === f.value ? cx.btnToggleOn : cx.btnToggleOff}
            >
              {f.label}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <input
            list="portfolio-ledger-symbols"
            value={sym}
            onChange={(e) => setSym(e.target.value)}
            placeholder="종목 검색 (비우면 전체)"
            className={cx.input + " sm:w-56"}
          />
          <datalist id="portfolio-ledger-symbols">
            {symbols.map((s) => (
              <option key={s} value={s} />
            ))}
          </datalist>
          <Select
            value={acct}
            onChange={setAcct}
            className="w-auto min-w-36"
            items={[{ value: "", label: "전체 계좌" }, ...accountNames.map((a) => ({ value: a, label: a }))]}
          />
          <DateRangeFilter from={dr.from} to={dr.to} setFrom={dr.setFrom} setTo={dr.setTo} />
        </div>

        <Panel className="overflow-x-auto rounded-xl bg-slate-900/40">
          <table className={cx.table.root}>
            <thead className={cx.table.head}>
              <tr>
                <th className={cx.table.th}>일자</th>
                <th className={cx.table.th}>유형</th>
                <th className={cx.table.th}>내용</th>
                <th className={cx.table.th}>계좌</th>
                <th className={cx.table.th + " text-right"}>수량</th>
                <th className={cx.table.th + " text-right"}>단가</th>
                <th className={cx.table.th + " text-right"}>금액</th>
                <th className={cx.table.th}>메모</th>
                <th className={cx.table.th + " text-right"}>관리</th>
              </tr>
            </thead>
            <tbody className={cx.table.body}>
              {paged.rows.map((e) => (
                <tr key={e.key} className={cx.table.tr}>
                  <td className={cx.table.td + " whitespace-nowrap tabular-nums"}>
                    {ledgerDateText(e.date, thisYear)}
                  </td>
                  <td className={cx.table.td}>
                    <span className={"inline-block rounded-full px-2 py-0.5 text-xs " + (LABEL_CHIP[e.label] ?? "")}>
                      {e.label}
                    </span>
                  </td>
                  <td className={cx.table.td + " font-medium text-white"}>
                    {e.title}
                    {e.group === "trade" && e.currency !== "KRW" && (
                      <span className="ml-1 text-xs text-slate-500">{e.currency}</span>
                    )}
                  </td>
                  <td className={cx.table.td}>{e.account}</td>
                  <td className={cx.table.td + " text-right tabular-nums"}>{e.quantity ?? "—"}</td>
                  <td className={cx.table.td + " text-right tabular-nums"}>{priceText(e)}</td>
                  <td className={cx.table.td + " text-right tabular-nums text-white"}>
                    {natMoney(e.amount, e.currency)}
                  </td>
                  <td className={cx.table.td + " text-slate-500"}>{e.tx?.memo ?? e.fx?.memo ?? ""}</td>
                  <td className={cx.table.td + " whitespace-nowrap text-right"}>
                    {!isOwnScope ? (
                      <span className="text-xs text-slate-600">—</span>
                    ) : (
                      <>
                        <button
                          onClick={() => (e.tx ? setEditingTx(e.tx) : setEditingFx(e.fx))}
                          className="mr-2 text-xs text-slate-400 transition hover:text-white"
                        >
                          수정
                        </button>
                        <button
                          onClick={() => (e.tx ? delTx(e.tx.id) : delFx(e.fx!.id))}
                          className="text-xs text-slate-500 transition hover:text-rose-300"
                        >
                          삭제
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-4 py-8 text-center text-sm text-slate-500">
                    조건에 맞는 기록이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </Panel>

        <Pagination
          page={paged.page}
          pageCount={paged.pageCount}
          from={paged.from}
          to={paged.to}
          total={paged.total}
          onPage={paged.setPage}
        />

        <p className="text-xs text-slate-600">환전은 통화 간 현금 이동이라 순납입에 잡히지 않는다.</p>
      </div>

      {guiding && (
        <GuidedEntryModal
          accounts={modalAccounts}
          recentSymbols={symbols}
          createUrl={createUrl}
          holdingLink={isOwnScope}
          onClose={() => setGuiding(false)}
          onSaved={() => {
            setGuiding(false);
            reload();
          }}
        />
      )}
      {adding && (
        <QuickEntryModal
          accounts={modalAccounts}
          recentSymbols={symbols}
          createUrl={createUrl}
          holdingLink={isOwnScope}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            reload();
          }}
        />
      )}
      {editingTx && (
        <QuickEntryModal
          accounts={accounts}
          recentSymbols={symbols}
          initialTx={editingTx}
          onClose={() => setEditingTx(null)}
          onSaved={() => {
            setEditingTx(null);
            reload();
          }}
        />
      )}
      {editingFx && (
        <QuickEntryModal
          accounts={accounts}
          initialFx={editingFx}
          onClose={() => setEditingFx(null)}
          onSaved={() => {
            setEditingFx(null);
            reload();
          }}
        />
      )}
    </>
  );
}
