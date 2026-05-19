"use client";

import { useState } from "react";
import { clientFetch } from "@/services/client";
import { cx } from "@/utils/cx";
import type { UserSummary } from "@/types/user";

type Role = "USER" | "ADMIN" | "ROOT";

const ROLE_LABELS: Record<Role, string> = {
  USER: "USER",
  ADMIN: "ADMIN",
  ROOT: "ROOT",
};

const ROLE_BADGE: Record<Role, string> = {
  USER: "bg-slate-600/30 text-slate-300",
  ADMIN: "bg-indigo-600/30 text-indigo-300",
  ROOT: "bg-amber-600/30 text-amber-300",
};

interface Props {
  users: UserSummary[];
}

export default function RootUsersClient({ users: initialUsers }: Props) {
  const [users, setUsers] = useState<UserSummary[]>(initialUsers);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [confirmChange, setConfirmChange] = useState<{ id: number; role: Role } | null>(null);
  const [confirmReset, setConfirmReset] = useState<UserSummary | null>(null);
  const [tempPassword, setTempPassword] = useState<string | null>(null);

  async function applyRoleChange(userId: number, role: Role) {
    setPendingId(userId);
    try {
      const res = await clientFetch(`/api/root/users/${userId}/role`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role }),
      });
      if (res?.ok) {
        setUsers((prev) =>
          prev.map((u) => (u.id === userId ? { ...u, role } : u))
        );
      }
    } finally {
      setPendingId(null);
      setConfirmChange(null);
    }
  }

  async function applyPasswordReset(userId: number) {
    setPendingId(userId);
    try {
      const res = await clientFetch(`/api/root/users/${userId}/reset-password`, {
        method: "POST",
      });
      if (res?.ok) {
        const data = await res.json();
        setTempPassword(data.temporaryPassword);
      }
    } finally {
      setPendingId(null);
      setConfirmReset(null);
    }
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h2 className="text-white text-lg font-semibold mb-6">사용자 관리</h2>

      {/* 역할 변경 확인 모달 */}
      {confirmChange && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-white/15 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-white font-semibold text-base mb-2">역할 변경 확인</h3>
            <p className="text-slate-300 text-sm mb-5">
              이 사용자의 역할을{" "}
              <span className="text-white font-medium">{ROLE_LABELS[confirmChange.role]}</span>
              (으)로 변경하시겠습니까?
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => applyRoleChange(confirmChange.id, confirmChange.role)}
                disabled={pendingId === confirmChange.id}
                className={`flex-1 ${cx.btnPrimary}`}
              >
                {pendingId === confirmChange.id ? "변경 중..." : "확인"}
              </button>
              <button onClick={() => setConfirmChange(null)} className={`flex-1 ${cx.btnSecondary}`}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 비밀번호 초기화 확인 모달 */}
      {confirmReset && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-white/15 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-white font-semibold text-base mb-2">비밀번호 초기화</h3>
            <p className="text-slate-300 text-sm mb-5">
              <span className="text-white font-medium">{confirmReset.name}</span>
              ({confirmReset.username}) 의 비밀번호를 초기화하시겠습니까?
              <br />
              <span className="text-amber-400 text-xs mt-1 block">
                초기화 후 임시 비밀번호가 발급되며, 해당 사용자는 다음 로그인 시 비밀번호를 변경해야 합니다.
              </span>
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => applyPasswordReset(confirmReset.id)}
                disabled={pendingId === confirmReset.id}
                className={`flex-1 ${cx.btnPrimary}`}
              >
                {pendingId === confirmReset.id ? "초기화 중..." : "초기화"}
              </button>
              <button onClick={() => setConfirmReset(null)} className={`flex-1 ${cx.btnSecondary}`}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 임시 비밀번호 표시 모달 */}
      {tempPassword && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-white/15 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-white font-semibold text-base mb-2">임시 비밀번호 발급 완료</h3>
            <p className="text-slate-400 text-sm mb-4">
              아래 임시 비밀번호를 사용자에게 전달하세요.
              <br />사용자는 로그인 후 즉시 비밀번호를 변경해야 합니다.
            </p>
            <div className="bg-slate-900 border border-white/10 rounded-xl px-4 py-3 text-center mb-5">
              <p className="text-white text-xl font-mono tracking-widest">{tempPassword}</p>
            </div>
            <button
              onClick={() => setTempPassword(null)}
              className={`w-full ${cx.btnPrimary}`}
            >
              확인
            </button>
          </div>
        </div>
      )}

      <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>이름</th>
              <th className={cx.table.th}>아이디</th>
              <th className={cx.table.th}>이메일</th>
              <th className={cx.table.th}>현재 역할</th>
              <th className={cx.table.th}>역할 변경</th>
              <th className={cx.table.th}>비밀번호</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {users.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-slate-500 py-8">
                  사용자가 없습니다
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className={cx.table.tr}>
                  <td className={`${cx.table.td} font-medium text-white`}>{user.name}</td>
                  <td className={`${cx.table.td} text-xs font-mono`}>{user.username}</td>
                  <td className={`${cx.table.td} text-xs`}>{user.email}</td>
                  <td className={cx.table.td}>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${ROLE_BADGE[user.role as Role]}`}>
                      {ROLE_LABELS[user.role as Role]}
                    </span>
                  </td>
                  <td className={cx.table.td}>
                    {user.role === "ROOT" ? (
                      <span className="text-xs text-slate-600">변경 불가</span>
                    ) : (
                      <div className="flex gap-1.5 flex-wrap">
                        {(["USER", "ADMIN"] as Role[]).map((r) => (
                          <button
                            key={r}
                            disabled={user.role === r || pendingId === user.id}
                            onClick={() => setConfirmChange({ id: user.id, role: r })}
                            className={
                              user.role === r
                                ? `${cx.btnToggleOn} text-xs py-1 px-2.5 cursor-default`
                                : `${cx.btnToggleOff} text-xs py-1 px-2.5 disabled:opacity-40`
                            }
                          >
                            {ROLE_LABELS[r]}
                          </button>
                        ))}
                      </div>
                    )}
                  </td>
                  <td className={cx.table.td}>
                    <button
                      disabled={pendingId === user.id}
                      onClick={() => setConfirmReset(user)}
                      className="text-xs px-2.5 py-1 rounded-lg border border-amber-500/30 text-amber-400 hover:bg-amber-500/10 transition disabled:opacity-40"
                    >
                      초기화
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
