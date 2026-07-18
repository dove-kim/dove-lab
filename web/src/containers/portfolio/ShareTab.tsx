"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Modal from "@/components/Modal";
import { cx } from "@/utils/cx";
import { type PortfolioShare, type PortfolioAccount, SHARE_PERM_LABEL } from "@/types/portfolio";
import { setScopeExternally } from "./scopeView";

/**
 * 공유 탭 — 계좌 단위 공유(내보낸/받은)를 관리한다. 받은 공유는 열람 페이지로 진입한다.
 */
export default function ShareTab() {
  const [shares, setShares] = useState<PortfolioShare[] | null>(null);
  const [accounts, setAccounts] = useState<PortfolioAccount[]>([]);
  const [err, setErr] = useState(false);
  const [showGrant, setShowGrant] = useState(false);
  const router = useRouter();

  function load() {
    fetch("/api/portfolio/shares")
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: PortfolioShare[]) => setShares(Array.isArray(d) ? d : []))
      .catch(() => setErr(true));
    fetch("/api/portfolio/accounts")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioAccount[]) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {});
  }

  useEffect(() => {
    load();
  }, []);

  async function revoke(id: number, name: string) {
    if (!confirm(`'${name}' 공유를 철회할까요?`)) return;
    await fetch(`/api/portfolio/shares/${id}`, { method: "DELETE" });
    load();
  }

  if (err) return <p className="text-sm text-rose-300 py-8 text-center">공유 정보를 불러오지 못했습니다.</p>;
  if (!shares) return <p className="text-sm text-slate-500 py-8 text-center">불러오는 중…</p>;

  const out = shares.filter((s) => s.direction === "OUT");
  const inc = shares.filter((s) => s.direction === "IN");

  return (
    <div className="flex flex-col gap-6">
      <p className="text-xs text-slate-500">공유는 <span className="text-slate-300">계좌 단위</span>입니다. 상대의 아이디로 초대합니다.</p>

      <section className="bg-white/5 border border-white/10 rounded-xl p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-medium text-slate-300">내가 공유한 계좌</h2>
          <button className={cx.btnPrimary} onClick={() => setShowGrant(true)} disabled={accounts.length === 0}>
            ＋ 공유 초대
          </button>
        </div>
        {out.length === 0 && <p className="text-xs text-slate-500">공유한 계좌가 없습니다.</p>}
        <div className="divide-y divide-white/5">
          {out.map((s) => (
            <div key={s.id} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0">
                <div className="text-white font-medium">{s.accountName}</div>
                <div className="text-xs text-slate-500">{s.grantee}</div>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-xs text-slate-400">{SHARE_PERM_LABEL[s.permission]}</span>
                <button className="text-xs text-slate-500 hover:text-rose-300 transition" onClick={() => revoke(s.id, s.accountName)}>
                  철회
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-white/5 border border-white/10 rounded-xl p-5">
        <h2 className="text-sm font-medium text-slate-300 mb-3">공유받은 계좌</h2>
        {inc.length === 0 && <p className="text-xs text-slate-500">공유받은 계좌가 없습니다.</p>}
        <div className="divide-y divide-white/5">
          {inc.map((s) => (
            <div key={s.id} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0">
                <div className="text-white font-medium">{s.accountName}</div>
                <div className="text-xs text-slate-500">{s.grantee} · {SHARE_PERM_LABEL[s.permission]}</div>
              </div>
              <button
                className={cx.btnSecondary}
                onClick={() => {
                  setScopeExternally(String(s.accountId));
                  router.push("/portfolio/analysis");
                }}
              >
                열람
              </button>
            </div>
          ))}
        </div>
      </section>

      {showGrant && (
        <GrantModal
          accounts={accounts}
          onClose={() => setShowGrant(false)}
          onSaved={() => {
            setShowGrant(false);
            load();
          }}
        />
      )}
    </div>
  );
}

/**
 * 공유 초대 모달 — 계좌·상대 아이디·권한을 지정해 공유한다.
 */
function GrantModal({
  accounts,
  onClose,
  onSaved,
}: {
  accounts: PortfolioAccount[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [accountId, setAccountId] = useState<number | "">(accounts[0]?.id ?? "");
  const [granteeUsername, setGranteeUsername] = useState("");
  const [permission, setPermission] = useState<PortfolioShare["permission"]>("READ");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (accountId === "") {
      setError("계좌를 선택하세요.");
      return;
    }
    if (!granteeUsername.trim()) {
      setError("상대 아이디를 입력하세요.");
      return;
    }
    setSaving(true);
    setError(null);
    const res = await fetch("/api/portfolio/shares", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ accountId, granteeUsername: granteeUsername.trim(), permission }),
    });
    if (res.ok) {
      onSaved();
    } else {
      const code = await res.json().then((d) => d?.detail).catch(() => null);
      setError(
        code === "PORTFOLIO_GRANTEE_NOT_FOUND" ? "해당 아이디의 사용자가 없습니다."
          : code === "CANNOT_SHARE_TO_SELF" ? "자기 자신에게는 공유할 수 없습니다."
          : "공유에 실패했습니다."
      );
      setSaving(false);
    }
  }

  return (
    <Modal
      title="공유 초대"
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose} className={cx.btnSecondary} disabled={saving}>취소</button>
          <button onClick={submit} className={cx.btnPrimary} disabled={saving}>{saving ? "공유 중…" : "공유"}</button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">계좌</span>
          <select value={accountId} onChange={(e) => setAccountId(Number(e.target.value))} className={cx.select + " w-full"}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">상대 아이디</span>
          <input value={granteeUsername} onChange={(e) => setGranteeUsername(e.target.value)} placeholder="공유받을 사용자 아이디" className={cx.input} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">권한</span>
          <select value={permission} onChange={(e) => setPermission(e.target.value as PortfolioShare["permission"])} className={cx.select + " w-full"}>
            <option value="READ">읽기 (금액 포함)</option>
            <option value="READ_RELATIVE">읽기(상대값) — 금액 숨김, 비중·수익률만</option>
            <option value="WRITE">읽기·쓰기</option>
          </select>
        </label>
        {error && <p className="text-sm text-rose-300">{error}</p>}
      </div>
    </Modal>
  );
}
