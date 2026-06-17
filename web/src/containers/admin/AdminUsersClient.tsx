"use client";

import { useState, useMemo } from "react";
import { clientFetch } from "@/services/client";
import { cx } from "@/utils/cx";
import { CAPABILITY_LABELS, ALL_CAPABILITIES } from "@/utils/capability";
import type { UserSummary } from "@/types/user";

const ROLE_BADGE: Record<string, string> = {
  USER: "bg-slate-600/30 text-slate-300",
  ADMIN: "bg-indigo-600/30 text-indigo-300",
  ROOT: "bg-amber-600/30 text-amber-300",
};

interface Props {
  users: UserSummary[];
}

export default function AdminUsersClient({ users }: Props) {
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");

  // 서버에서 확인된 상태
  const [serverGranted, setServerGranted] = useState<Set<string>>(new Set());
  // 로컬 편집 중인 draft 상태
  const [draftGranted, setDraftGranted] = useState<Set<string>>(new Set());

  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) =>
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.username.toLowerCase().includes(q)
    );
  }, [users, search]);

  const isDirty = useMemo(() => {
    if (serverGranted.size !== draftGranted.size) return true;
    for (const c of serverGranted) {
      if (!draftGranted.has(c)) return true;
    }
    return false;
  }, [serverGranted, draftGranted]);

  const selectedUser = users.find((u) => u.id === selectedUserId);
  const isRootTarget = selectedUser?.role === "ROOT";

  async function selectUser(userId: number) {
    setSelectedUserId(userId);
    setLoading(true);
    setServerGranted(new Set());
    setDraftGranted(new Set());
    try {
      const res = await clientFetch(`/api/admin/users/${userId}/capabilities`);
      if (res?.ok) {
        const caps: string[] = await res.json();
        const granted = new Set(caps);
        setServerGranted(granted);
        setDraftGranted(new Set(granted));
      }
    } finally {
      setLoading(false);
    }
  }

  function toggleCapability(capability: string) {
    setDraftGranted((prev) => {
      const next = new Set(prev);
      if (next.has(capability)) next.delete(capability);
      else next.add(capability);
      return next;
    });
  }

  function discardChanges() {
    setDraftGranted(new Set(serverGranted));
  }

  async function saveChanges() {
    if (!selectedUserId || !isDirty) return;
    setSaving(true);
    try {
      const toGrant = [...draftGranted].filter((c) => !serverGranted.has(c));
      const toRevoke = [...serverGranted].filter((c) => !draftGranted.has(c));

      await Promise.all([
        ...toGrant.map((capability) =>
          clientFetch(`/api/admin/users/${selectedUserId}/capabilities`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ capability, action: "GRANT" }),
          })
        ),
        ...toRevoke.map((capability) =>
          clientFetch(`/api/admin/users/${selectedUserId}/capabilities`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ capability, action: "REVOKE" }),
          })
        ),
      ]);

      setServerGranted(new Set(draftGranted));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h2 className="text-white text-lg font-semibold mb-6">권한 관리</h2>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 max-w-5xl">
        {/* 사용자 목록 */}
        <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
          <div className="px-4 py-3 border-b border-white/10 flex flex-col gap-2">
            <h3 className="text-slate-300 text-sm font-medium">사용자 목록</h3>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="이름 · 아이디 · 이메일 검색"
              className={cx.input}
            />
          </div>
          {users.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">사용자가 없습니다</p>
          ) : filteredUsers.length === 0 ? (
            <p className="text-slate-500 text-sm text-center py-8">검색 결과가 없습니다</p>
          ) : (
            <div className="divide-y divide-white/5">
              {filteredUsers.map((user) => (
                <button
                  key={user.id}
                  onClick={() => selectUser(user.id)}
                  className={`w-full flex items-center gap-3 px-4 py-3 text-left transition ${
                    selectedUserId === user.id
                      ? "bg-indigo-600/15 border-l-2 border-indigo-400"
                      : "hover:bg-white/5"
                  }`}
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-white text-sm font-medium truncate">{user.name}</p>
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full flex-shrink-0 ${ROLE_BADGE[user.role] ?? ROLE_BADGE.USER}`}
                      >
                        {user.role}
                      </span>
                    </div>
                    <p className="text-slate-300 text-sm font-mono truncate mt-0.5">{user.username}</p>
                    <p className="text-slate-400 text-xs truncate">{user.email}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 권한 패널 */}
        <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-white/10">
            <h3 className="text-slate-300 text-sm font-medium">
              {selectedUser ? `${selectedUser.name} — 권한` : "사용자를 선택하세요"}
            </h3>
          </div>

          {!selectedUserId ? (
            <div className="flex items-center justify-center py-16">
              <p className="text-slate-600 text-sm">좌측에서 사용자를 선택하세요</p>
            </div>
          ) : loading ? (
            <div className="flex items-center justify-center py-16">
              <p className="text-slate-500 text-sm">불러오는 중...</p>
            </div>
          ) : (
            <>
              {isRootTarget && (
                <p className="px-4 py-2 text-xs text-amber-300/80 bg-amber-500/5 border-b border-white/10">
                  ROOT는 모든 권한을 자동 보유합니다. 부여 설정은 적용되지 않습니다.
                </p>
              )}
              <div className="divide-y divide-white/5 flex-1">
                {ALL_CAPABILITIES.map((capability) => {
                  const granted = draftGranted.has(capability);
                  const changed = granted !== serverGranted.has(capability);

                  return (
                    <div key={capability} className={changed ? "bg-amber-500/5" : ""}>
                      <div className="flex items-center justify-between px-4 py-3">
                        <div>
                          <p className="text-white text-sm font-medium flex items-center gap-2">
                            {CAPABILITY_LABELS[capability] ?? capability}
                            {changed && (
                              <span className="text-amber-400 text-xs font-normal">
                                {granted ? "(+추가)" : "(−제거)"}
                              </span>
                            )}
                          </p>
                          <p className="text-xs text-slate-500 mt-0.5">{capability}</p>
                        </div>
                        <button
                          disabled={saving}
                          onClick={() => toggleCapability(capability)}
                          className={`min-w-[72px] text-center text-xs py-1.5 px-3 rounded-lg font-medium transition disabled:opacity-50 ${
                            granted
                              ? "bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 hover:bg-red-900/30 hover:text-red-300 hover:border-red-500/30"
                              : "bg-white/5 text-slate-400 border border-white/15 hover:bg-indigo-600/20 hover:text-indigo-300 hover:border-indigo-500/30"
                          }`}
                        >
                          {granted ? "부여됨" : "미부여"}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* 저장 / 되돌리기 */}
              <div className="px-4 py-3 border-t border-white/10 flex items-center justify-end gap-2">
                {isDirty && (
                  <button
                    onClick={discardChanges}
                    disabled={saving}
                    className={cx.btnSecondary}
                  >
                    되돌리기
                  </button>
                )}
                <button
                  onClick={saveChanges}
                  disabled={!isDirty || saving}
                  className={`${cx.btnPrimary} disabled:opacity-30 disabled:cursor-not-allowed`}
                >
                  {saving ? "저장 중..." : "저장"}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
