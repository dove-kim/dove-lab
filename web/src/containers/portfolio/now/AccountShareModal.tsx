"use client";

import { useEffect, useState } from "react";
import Modal from "@/components/Modal";
import Select from "@/components/Select";
import { useConfirm } from "@/components/ConfirmProvider";
import { cx } from "@/utils/cx";
import { type PortfolioAccount, type PortfolioShare, SHARE_PERM_LABEL } from "@/types/portfolio";

/**
 * 계좌 공유 모달 — 한 계좌의 공유 대상을 보고, 초대하거나 철회한다.
 *
 * @param account 공유 대상 계좌
 * @param onClose 닫기
 */
export default function AccountShareModal({
  account,
  onClose,
}: {
  account: PortfolioAccount;
  onClose: () => void;
}) {
  const confirm = useConfirm();
  const [shares, setShares] = useState<PortfolioShare[]>([]);
  const [granteeUsername, setGranteeUsername] = useState("");
  const [permission, setPermission] = useState<PortfolioShare["permission"]>("READ");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function load() {
    fetch("/api/portfolio/shares", { cache: "no-store" })
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioShare[]) =>
        setShares(Array.isArray(d) ? d.filter((s) => s.direction === "OUT" && s.accountId === account.id) : [])
      )
      .catch(() => {});
  }

  useEffect(() => {
    load();
  }, [account.id]); // eslint-disable-line react-hooks/exhaustive-deps

  async function revoke(share: PortfolioShare) {
    const ok = await confirm({
      title: "공유 철회",
      description: `'${share.grantee}' 에게 준 공유를 철회할까요?`,
      confirmText: "철회",
      danger: true,
    });
    if (!ok) return;
    await fetch(`/api/portfolio/shares/${share.id}`, { method: "DELETE" });
    load();
  }

  async function grant() {
    if (!granteeUsername.trim()) {
      setError("상대 아이디를 입력하세요.");
      return;
    }
    setSaving(true);
    setError(null);
    const res = await fetch("/api/portfolio/shares", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ accountId: account.id, granteeUsername: granteeUsername.trim(), permission }),
    });
    setSaving(false);
    if (res.ok) {
      setGranteeUsername("");
      load();
      return;
    }
    const code = await res.json().then((d) => d?.detail).catch(() => null);
    setError(
      code === "PORTFOLIO_GRANTEE_NOT_FOUND" ? "해당 아이디의 사용자가 없습니다."
        : code === "CANNOT_SHARE_TO_SELF" ? "자기 자신에게는 공유할 수 없습니다."
        : "공유에 실패했습니다."
    );
  }

  return (
    <Modal
      title={`공유 · ${account.name}`}
      onClose={onClose}
      footer={
        <button onClick={onClose} className={cx.btnSecondary}>
          닫기
        </button>
      }
    >
      <div className="flex flex-col gap-4">
        {shares.length === 0 ? (
          <p className="text-xs text-slate-500">이 계좌를 공유한 상대가 없습니다.</p>
        ) : (
          <div className="divide-y divide-white/5">
            {shares.map((s) => (
              <div key={s.id} className="flex items-center justify-between gap-3 py-2.5">
                <div className="min-w-0">
                  <div className="text-white text-sm">{s.grantee}</div>
                  <div className="text-xs text-slate-500">{SHARE_PERM_LABEL[s.permission]}</div>
                </div>
                <button className={cx.btnDangerSm} onClick={() => revoke(s)}>
                  철회
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="flex flex-col gap-3 rounded-xl bg-white/3 p-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">상대 아이디</span>
            <input
              value={granteeUsername}
              onChange={(e) => setGranteeUsername(e.target.value)}
              placeholder="공유받을 사용자 아이디"
              className={cx.input}
            />
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-slate-400">권한</span>
            <Select
              value={permission}
              onChange={(v) => setPermission(v as PortfolioShare["permission"])}
              items={[
                { value: "READ", label: "읽기 (금액 포함)" },
                { value: "READ_RELATIVE", label: "읽기(상대값) — 금액 숨김, 비중·수익률만" },
                { value: "WRITE", label: "읽기·쓰기" },
              ]}
            />
          </label>
          <button onClick={grant} className={cx.btnPrimary} disabled={saving}>
            {saving ? "공유 중…" : "공유 초대"}
          </button>
          {error && <p className="text-sm text-rose-300">{error}</p>}
        </div>
      </div>
    </Modal>
  );
}
