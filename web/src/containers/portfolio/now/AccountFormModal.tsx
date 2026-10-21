"use client";

import { useState } from "react";
import Modal from "@/components/Modal";
import { cx } from "@/utils/cx";
import type { PortfolioAccount } from "@/types/portfolio";

/**
 * 계좌 추가/수정 모달.
 *
 * @param initial 수정 대상 계좌(없으면 신규)
 * @param onClose 닫기
 * @param onSaved 저장 성공 후 콜백
 */
export default function AccountFormModal({
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
