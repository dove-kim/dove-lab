"use client";

import { useState } from "react";
import { cx } from "@/utils/cx";

const ERROR_MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: "현재 비밀번호가 올바르지 않습니다.",
  DEFAULT: "비밀번호 변경에 실패했습니다. 다시 시도해 주세요.",
};

export default function ForcePasswordChangeModal() {
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (newPassword !== confirm) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    if (newPassword.length < 4) {
      setError("비밀번호는 4자 이상이어야 합니다.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch("/api/account/password", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ newPassword }),
      });

      if (res.ok) {
        window.location.reload();
        return;
      }

      const data = await res.json().catch(() => ({}));
      const code = data?.detail ?? "DEFAULT";
      setError(ERROR_MESSAGES[code] ?? ERROR_MESSAGES.DEFAULT);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-800 border border-white/15 rounded-2xl p-8 w-full max-w-sm shadow-2xl">
        <div className="mb-6 text-center">
          <div className="w-12 h-12 rounded-full bg-amber-500/20 flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-amber-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </div>
          <h2 className="text-white text-lg font-semibold">비밀번호 변경 필요</h2>
          <p className="text-slate-400 text-sm mt-1">
            관리자가 비밀번호를 초기화했습니다.
            <br />새 비밀번호를 설정해야 서비스를 이용할 수 있습니다.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div>
            <label className="block text-xs text-slate-400 mb-1">새 비밀번호</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="새 비밀번호 입력"
              required
              className={cx.input}
            />
          </div>
          <div>
            <label className="block text-xs text-slate-400 mb-1">비밀번호 확인</label>
            <input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              placeholder="비밀번호 다시 입력"
              required
              className={cx.input}
            />
          </div>

          {error && (
            <p className="text-red-400 text-xs">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className={`${cx.btnPrimary} w-full mt-2 disabled:opacity-50`}
          >
            {loading ? "변경 중..." : "비밀번호 변경"}
          </button>
        </form>
      </div>
    </div>
  );
}
