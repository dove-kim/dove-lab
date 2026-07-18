"use client";

import { useMemo, useState } from "react";
import { clientFetch } from "@/services/client";
import { cx } from "@/utils/cx";
import { usePaged, Pagination } from "@/components/Pagination";
import type { UserSummary } from "@/types/user";

type Role = "USER" | "ADMIN" | "ROOT";
type StatusFilter = "ACTIVE" | "DELETED" | "ALL";

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

const fmtDate = (iso: string) => iso.slice(0, 10);
const todayStr = () => new Date().toISOString().slice(0, 10);
/** 오늘로부터 1년 전(yyyy-MM-dd). 가입일 시작 기본값. */
const oneYearAgoStr = () => {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 1);
  return d.toISOString().slice(0, 10);
};
/** 목록에서 가장 이른 가입일(yyyy-MM-dd). 비어 있으면 "". */
const earliestDate = (list: UserSummary[]) =>
  list.map((u) => fmtDate(u.createdAt)).filter(Boolean).sort()[0] ?? "";

interface Props {
  users: UserSummary[];
}

export default function RootUsersClient({ users: initialUsers }: Props) {
  const [users, setUsers] = useState<UserSummary[]>(initialUsers);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [confirmChange, setConfirmChange] = useState<{ id: number; role: Role } | null>(null);
  const [confirmReset, setConfirmReset] = useState<UserSummary | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<UserSummary | null>(null);
  const [tempPassword, setTempPassword] = useState<string | null>(null);

  // 초안(입력 중) / 적용(검색 클릭 시 반영) 필터 분리. 가입일 시작 기본값 = 최근 1년.
  const [draftStatus, setDraftStatus] = useState<StatusFilter>("ACTIVE");
  const [draftFrom, setDraftFrom] = useState(oneYearAgoStr);
  const [draftTo, setDraftTo] = useState(todayStr);
  const [status, setStatus] = useState<StatusFilter>("ACTIVE");
  const [from, setFrom] = useState(oneYearAgoStr);
  const [to, setTo] = useState(todayStr);

  function search() {
    setStatus(draftStatus);
    setFrom(draftFrom);
    setTo(draftTo);
  }

  function resetFilters() {
    const t = todayStr();
    setDraftStatus("ACTIVE");
    setDraftFrom(oneYearAgoStr());
    setDraftTo(t);
    setStatus("ACTIVE");
    setFrom(oneYearAgoStr());
    setTo(t);
  }

  /** 전체 기간 — 가입일 하한 해제(가장 이른 가입일)로 넓혀 즉시 적용. */
  function allPeriod() {
    const f = earliestDate(users);
    const t = todayStr();
    setDraftFrom(f);
    setDraftTo(t);
    setFrom(f);
    setTo(t);
    setStatus(draftStatus);
  }

  const filtered = useMemo(
    () =>
      users.filter((u) => {
        const deleted = !!u.deletedAt;
        if (status === "ACTIVE" && deleted) return false;
        if (status === "DELETED" && !deleted) return false;
        const d = fmtDate(u.createdAt);
        if (from && d < from) return false;
        if (to && d > to) return false;
        return true;
      }),
    [users, status, from, to]
  );

  const paged = usePaged(filtered, `${status}|${from}|${to}`, 10);

  async function applyRoleChange(userId: number, role: Role) {
    setPendingId(userId);
    try {
      const res = await clientFetch(`/api/admin/users/${userId}/role`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role }),
      });
      if (res?.ok) {
        setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role } : u)));
      }
    } finally {
      setPendingId(null);
      setConfirmChange(null);
    }
  }

  async function applyPasswordReset(userId: number) {
    setPendingId(userId);
    try {
      const res = await clientFetch(`/api/admin/users/${userId}/reset-password`, {
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

  async function applyDelete(userId: number) {
    setPendingId(userId);
    try {
      const res = await clientFetch(`/api/admin/users/${userId}`, { method: "DELETE" });
      if (res?.ok) {
        const now = new Date().toISOString();
        setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, deletedAt: now } : u)));
      }
    } finally {
      setPendingId(null);
      setConfirmDelete(null);
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

      {/* 회원 삭제 확인 모달 */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-white/15 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h3 className="text-white font-semibold text-base mb-2">회원 삭제</h3>
            <p className="text-slate-300 text-sm mb-5">
              <span className="text-white font-medium">{confirmDelete.name}</span>
              ({confirmDelete.username}) 을(를) 삭제하시겠습니까?
              <br />
              <span className="text-rose-400 text-xs mt-1 block">
                즉시 로그아웃되고 로그인이 차단됩니다. 거래·권한 등 기존 데이터는 보존됩니다(soft delete).
              </span>
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => applyDelete(confirmDelete.id)}
                disabled={pendingId === confirmDelete.id}
                className={`flex-1 ${cx.btnDanger}`}
              >
                {pendingId === confirmDelete.id ? "삭제 중..." : "삭제"}
              </button>
              <button onClick={() => setConfirmDelete(null)} className={`flex-1 ${cx.btnSecondary}`}>
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
            <button onClick={() => setTempPassword(null)} className={`w-full ${cx.btnPrimary}`}>
              확인
            </button>
          </div>
        </div>
      )}

      {/* 필터 */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          search();
        }}
        className="flex flex-wrap items-end gap-4 mb-4"
      >
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">삭제 여부</span>
          <select value={draftStatus} onChange={(e) => setDraftStatus(e.target.value as StatusFilter)} className={cx.select}>
            <option value="ACTIVE">활성</option>
            <option value="DELETED">삭제됨</option>
            <option value="ALL">전체</option>
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">가입일 시작</span>
          <input type="date" value={draftFrom} onChange={(e) => setDraftFrom(e.target.value)} className={cx.inputDate} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-slate-400">가입일 종료</span>
          <input type="date" value={draftTo} onChange={(e) => setDraftTo(e.target.value)} className={cx.inputDate} />
        </label>
        <button type="submit" className={cx.btnPrimary}>
          검색
        </button>
        <button type="button" onClick={allPeriod} className={cx.btnSecondary}>
          전체 기간
        </button>
        <button type="button" onClick={resetFilters} className={cx.btnSecondary}>
          필터 초기화
        </button>
      </form>

      <div className="bg-white/5 border border-white/10 rounded-xl overflow-x-auto">
        <table className="w-full text-sm">
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>이름</th>
              <th className={cx.table.th}>아이디</th>
              <th className={cx.table.th}>이메일</th>
              <th className={cx.table.th}>가입일</th>
              <th className={cx.table.th}>현재 역할</th>
              <th className={cx.table.th}>상태</th>
              <th className={cx.table.th}>역할 변경</th>
              <th className={cx.table.th}>비밀번호</th>
              <th className={cx.table.th}>삭제</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {paged.rows.length === 0 ? (
              <tr>
                <td colSpan={9} className="text-center text-slate-500 py-8">
                  조건에 맞는 사용자가 없습니다
                </td>
              </tr>
            ) : (
              paged.rows.map((user) => {
                const deleted = !!user.deletedAt;
                return (
                  <tr key={user.id} className={cx.table.tr + (deleted ? " opacity-60" : "")}>
                    <td className={`${cx.table.td} font-medium text-white`}>{user.name}</td>
                    <td className={`${cx.table.td} text-xs font-mono`}>{user.username}</td>
                    <td className={`${cx.table.td} text-xs`}>{user.email}</td>
                    <td className={`${cx.table.td} text-xs tabular-nums whitespace-nowrap`}>{fmtDate(user.createdAt)}</td>
                    <td className={cx.table.td}>
                      <span className={`text-xs px-2 py-0.5 rounded-full ${ROLE_BADGE[user.role as Role]}`}>
                        {ROLE_LABELS[user.role as Role]}
                      </span>
                    </td>
                    <td className={cx.table.td}>
                      {deleted ? (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-rose-600/25 text-rose-300 whitespace-nowrap">
                          삭제됨 · {fmtDate(user.deletedAt!)}
                        </span>
                      ) : (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-600/25 text-emerald-300">활성</span>
                      )}
                    </td>
                    {deleted ? (
                      <td className={`${cx.table.td} text-xs text-slate-600`} colSpan={3}>
                        —
                      </td>
                    ) : (
                      <>
                        <td className={cx.table.td}>
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
                        <td className={cx.table.td}>
                          <button
                            disabled={pendingId === user.id}
                            onClick={() => setConfirmDelete(user)}
                            className="text-xs px-2.5 py-1 rounded-lg border border-rose-500/30 text-rose-400 hover:bg-rose-500/10 transition disabled:opacity-40"
                          >
                            삭제
                          </button>
                        </td>
                      </>
                    )}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3">
        <Pagination
          page={paged.page}
          pageCount={paged.pageCount}
          from={paged.from}
          to={paged.to}
          total={paged.total}
          onPage={paged.setPage}
        />
      </div>
    </div>
  );
}
