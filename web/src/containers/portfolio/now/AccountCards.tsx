"use client";

import { useState } from "react";
import { useConfirm } from "@/components/ConfirmProvider";
import { cx } from "@/utils/cx";
import { type PortfolioAccount, natMoney } from "@/types/portfolio";
import { won } from "../usePortfolioData";
import AccountFormModal from "./AccountFormModal";
import AccountShareModal from "./AccountShareModal";

/** 계좌 한 곳의 집계 가치 — 통화별 원통화 잔고와 원화 환산 합계. */
export interface AccountValue {
  nativeByCur: Record<string, number>;
  krw: number;
}

/**
 * 계좌 카드 목록 — 계좌별 평가액·현금과 수정·공유·삭제를 한 자리에 둔다.
 *
 * @param accounts  내 계좌 목록
 * @param values    계좌 ID별 집계 가치
 * @param onChanged 계좌가 바뀐 뒤 재조회 콜백
 */
export default function AccountCards({
  accounts,
  values,
  onChanged,
}: {
  accounts: PortfolioAccount[];
  values: Map<number, AccountValue>;
  onChanged: () => void;
}) {
  const confirm = useConfirm();
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<PortfolioAccount | null>(null);
  const [sharing, setSharing] = useState<PortfolioAccount | null>(null);

  async function remove(account: PortfolioAccount) {
    const ok = await confirm({
      title: "계좌 삭제",
      description: `'${account.name}' 계좌를 삭제할까요?`,
      confirmText: "삭제",
      danger: true,
    });
    if (!ok) return;
    await fetch(`/api/portfolio/accounts/${account.id}`, { method: "DELETE" });
    onChanged();
  }

  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-sm font-medium text-slate-300">계좌</h2>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {accounts.map((a) => {
          const v = values.get(a.id) ?? { nativeByCur: {}, krw: 0 };
          const cash = Object.entries(v.nativeByCur).filter(([, bal]) => Math.round(bal) !== 0);
          return (
            <div key={a.id} className={cx.panel + " flex flex-col gap-2 p-4"}>
              <div className="min-w-0">
                <div className="truncate font-medium text-white">{a.name}</div>
                <div className="truncate text-xs text-slate-500">
                  {a.brokerName ?? "증권사 미지정"}
                  {a.description ? ` · ${a.description}` : ""}
                </div>
              </div>
              <div className={"text-lg tabular-nums " + (v.krw < 0 ? "text-rose-300" : "text-white")}>
                {won(Math.round(v.krw))}원
              </div>
              <div className="text-xs text-slate-500">
                {cash.length === 0 ? "현금 없음" : cash.map(([cur, bal]) => `현금 ${natMoney(bal, cur)}`).join(" · ")}
              </div>
              <div className="mt-1 flex flex-wrap gap-2">
                <button className={cx.btnSm} onClick={() => setEditing(a)}>
                  수정
                </button>
                <button className={cx.btnSm} onClick={() => setSharing(a)}>
                  공유
                </button>
                <button className={cx.btnDangerSm} onClick={() => remove(a)}>
                  삭제
                </button>
              </div>
            </div>
          );
        })}

        <button
          onClick={() => setAdding(true)}
          className="flex min-h-36 items-center justify-center rounded-xl border border-dashed border-border text-sm text-slate-500 transition hover:text-white"
        >
          ＋ 계좌 추가
        </button>
      </div>

      {adding && (
        <AccountFormModal
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            onChanged();
          }}
        />
      )}
      {editing && (
        <AccountFormModal
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            onChanged();
          }}
        />
      )}
      {sharing && <AccountShareModal account={sharing} onClose={() => setSharing(null)} />}
    </section>
  );
}
