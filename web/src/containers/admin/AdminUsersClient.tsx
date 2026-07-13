"use client";

import { useState, useMemo, useEffect } from "react";
import { clientFetch } from "@/services/client";
import { cx } from "@/utils/cx";
import { CAPABILITY_LABELS, ALL_CAPABILITIES } from "@/utils/capability";
import type { UserSummary } from "@/types/user";
import type { CustomMetric } from "@/types/customMetric";
import type { Model } from "@/types/model";

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

  // 커스텀 지표 카탈로그(마운트 시 1회 로드)
  const [metrics, setMetrics] = useState<CustomMetric[]>([]);
  // 선택 사용자의 커스텀 지표 grant — 서버/draft 상태
  const [serverGrantedMetrics, setServerGrantedMetrics] = useState<Set<number>>(new Set());
  const [draftGrantedMetrics, setDraftGrantedMetrics] = useState<Set<number>>(new Set());

  // 모델 카탈로그(마운트 시 1회 로드)
  const [models, setModels] = useState<Model[]>([]);
  // 선택 사용자의 모델 grant — 서버/draft 상태
  const [serverGrantedModels, setServerGrantedModels] = useState<Set<number>>(new Set());
  const [draftGrantedModels, setDraftGrantedModels] = useState<Set<number>>(new Set());

  useEffect(() => {
    (async () => {
      const res = await clientFetch(`/api/admin/custom-metric-grants/metrics`);
      if (res?.ok) {
        setMetrics(await res.json());
      }
    })();
    (async () => {
      const res = await clientFetch(`/api/admin/model-grants/models`);
      if (res?.ok) {
        setModels(await res.json());
      }
    })();
  }, []);

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
    if (serverGrantedMetrics.size !== draftGrantedMetrics.size) return true;
    for (const m of serverGrantedMetrics) {
      if (!draftGrantedMetrics.has(m)) return true;
    }
    if (serverGrantedModels.size !== draftGrantedModels.size) return true;
    for (const m of serverGrantedModels) {
      if (!draftGrantedModels.has(m)) return true;
    }
    return false;
  }, [
    serverGranted,
    draftGranted,
    serverGrantedMetrics,
    draftGrantedMetrics,
    serverGrantedModels,
    draftGrantedModels,
  ]);

  const metricItems = useMemo(
    () => metrics.map((m) => ({ id: m.id, title: m.name, subtitle: m.shape })),
    [metrics],
  );
  const modelItems = useMemo(
    () => models.map((m) => ({ id: m.id, title: `${m.name} v${m.version}`, subtitle: m.outputType })),
    [models],
  );

  const selectedUser = users.find((u) => u.id === selectedUserId);
  const isRootTarget = selectedUser?.role === "ROOT";

  async function selectUser(userId: number) {
    setSelectedUserId(userId);
    setLoading(true);
    setServerGranted(new Set());
    setDraftGranted(new Set());
    setServerGrantedMetrics(new Set());
    setDraftGrantedMetrics(new Set());
    setServerGrantedModels(new Set());
    setDraftGrantedModels(new Set());
    try {
      const [capsRes, metricsRes, modelsRes] = await Promise.all([
        clientFetch(`/api/admin/users/${userId}/capabilities`),
        clientFetch(`/api/admin/custom-metric-grants/users/${userId}`),
        clientFetch(`/api/admin/model-grants/users/${userId}`),
      ]);
      if (capsRes?.ok) {
        const caps: string[] = await capsRes.json();
        const granted = new Set(caps);
        setServerGranted(granted);
        setDraftGranted(new Set(granted));
      }
      if (metricsRes?.ok) {
        const ids: number[] = await metricsRes.json();
        const grantedMetrics = new Set(ids);
        setServerGrantedMetrics(grantedMetrics);
        setDraftGrantedMetrics(new Set(grantedMetrics));
      }
      if (modelsRes?.ok) {
        const ids: number[] = await modelsRes.json();
        const grantedModels = new Set(ids);
        setServerGrantedModels(grantedModels);
        setDraftGrantedModels(new Set(grantedModels));
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

  function toggleMetric(metricId: number) {
    setDraftGrantedMetrics((prev) => {
      const next = new Set(prev);
      if (next.has(metricId)) next.delete(metricId);
      else next.add(metricId);
      return next;
    });
  }

  function toggleModel(modelId: number) {
    setDraftGrantedModels((prev) => {
      const next = new Set(prev);
      if (next.has(modelId)) next.delete(modelId);
      else next.add(modelId);
      return next;
    });
  }

  function discardChanges() {
    setDraftGranted(new Set(serverGranted));
    setDraftGrantedMetrics(new Set(serverGrantedMetrics));
    setDraftGrantedModels(new Set(serverGrantedModels));
  }

  async function saveChanges() {
    if (!selectedUserId || !isDirty) return;
    setSaving(true);
    try {
      const toGrant = [...draftGranted].filter((c) => !serverGranted.has(c));
      const toRevoke = [...serverGranted].filter((c) => !draftGranted.has(c));
      const metricsToGrant = [...draftGrantedMetrics].filter((m) => !serverGrantedMetrics.has(m));
      const metricsToRevoke = [...serverGrantedMetrics].filter((m) => !draftGrantedMetrics.has(m));
      const modelsToGrant = [...draftGrantedModels].filter((m) => !serverGrantedModels.has(m));
      const modelsToRevoke = [...serverGrantedModels].filter((m) => !draftGrantedModels.has(m));

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
        ...metricsToGrant.map((metricId) =>
          clientFetch(`/api/admin/custom-metric-grants/users/${selectedUserId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ metricId, action: "GRANT" }),
          })
        ),
        ...metricsToRevoke.map((metricId) =>
          clientFetch(`/api/admin/custom-metric-grants/users/${selectedUserId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ metricId, action: "REVOKE" }),
          })
        ),
        ...modelsToGrant.map((modelId) =>
          clientFetch(`/api/admin/model-grants/users/${selectedUserId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ modelId, action: "GRANT" }),
          })
        ),
        ...modelsToRevoke.map((modelId) =>
          clientFetch(`/api/admin/model-grants/users/${selectedUserId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ modelId, action: "REVOKE" }),
          })
        ),
      ]);

      setServerGranted(new Set(draftGranted));
      setServerGrantedMetrics(new Set(draftGrantedMetrics));
      setServerGrantedModels(new Set(draftGrantedModels));
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
              <div className="flex-1 overflow-y-auto">
                <p className="px-4 pt-3 pb-1 text-xs font-medium uppercase tracking-wide text-slate-500">기능 권한</p>
                <div className="divide-y divide-white/5">
                {ALL_CAPABILITIES.map((capability) => {
                  const granted = draftGranted.has(capability);
                  const changed = granted !== serverGranted.has(capability);
                  // 개별 항목(지표·모델)을 가진 기능 권한은 부여됐을 때 이 줄 아래에 인라인으로 관리한다.
                  const itemCfg =
                    capability === "CUSTOM_INDICATOR"
                      ? { noun: "지표", items: metricItems, draftItems: draftGrantedMetrics, serverItems: serverGrantedMetrics, toggle: toggleMetric }
                      : capability === "MODEL_SCORE"
                      ? { noun: "모델", items: modelItems, draftItems: draftGrantedModels, serverItems: serverGrantedModels, toggle: toggleModel }
                      : null;

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
                            {itemCfg && granted && (
                              <span className="text-slate-500 text-xs font-normal">
                                {itemCfg.draftItems.size}/{itemCfg.items.length}
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
                      {/* 부모 권한이 부여됐을 때만 그 아래에서 개별 항목 관리 */}
                      {itemCfg && granted && (
                        <div className="px-4 pb-3">
                          <GrantItemsList
                            noun={itemCfg.noun}
                            items={itemCfg.items}
                            draftGranted={itemCfg.draftItems}
                            serverGranted={itemCfg.serverItems}
                            onToggle={itemCfg.toggle}
                            disabled={saving}
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
                </div>
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

interface GrantItem {
  id: number;
  title: string;
  subtitle?: string;
}

/**
 * 기능 권한 아래에 인라인으로 표시되는 개별 항목(지표·모델) 부여 목록 — 검색·일괄·개별 토글. draft Set만 편집(저장은 상위 패널).
 */
function GrantItemsList({ noun, items, draftGranted, serverGranted, onToggle, disabled }: {
  noun: string;
  items: GrantItem[];
  draftGranted: Set<number>;
  serverGranted: Set<number>;
  onToggle: (id: number) => void;
  disabled: boolean;
}) {
  const [q, setQ] = useState("");

  const filtered = useMemo(() => {
    const query = q.trim().toLowerCase();
    if (!query) return items;
    return items.filter((it) =>
      it.title.toLowerCase().includes(query) || (it.subtitle?.toLowerCase().includes(query) ?? false));
  }, [items, q]);

  function grantAllShown() {
    for (const it of filtered) if (!draftGranted.has(it.id)) onToggle(it.id);
  }
  function revokeAllShown() {
    for (const it of filtered) if (draftGranted.has(it.id)) onToggle(it.id);
  }

  if (items.length === 0) {
    return (
      <p className="text-xs text-slate-500 px-3 py-3 rounded-lg bg-black/20 border border-white/10">
        등록된 {noun} 없음
      </p>
    );
  }

  return (
    <div className="rounded-lg bg-black/20 border border-white/10">
      <div className="flex items-center gap-2 p-2 border-b border-white/10">
        <input
          type="text"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={`${noun} 검색`}
          className="flex-1 min-w-0 bg-slate-800 border border-white/10 rounded px-2 py-1 text-xs text-white"
        />
        <button
          disabled={disabled}
          onClick={grantAllShown}
          className="flex-shrink-0 text-xs py-1 px-2 rounded bg-white/5 text-slate-300 border border-white/15 hover:bg-indigo-600/20 hover:text-indigo-300 transition disabled:opacity-50"
        >
          모두 부여
        </button>
        <button
          disabled={disabled}
          onClick={revokeAllShown}
          className="flex-shrink-0 text-xs py-1 px-2 rounded bg-white/5 text-slate-300 border border-white/15 hover:bg-red-900/30 hover:text-red-300 transition disabled:opacity-50"
        >
          모두 해제
        </button>
      </div>
      <div className="max-h-56 overflow-y-auto divide-y divide-white/5">
        {filtered.length === 0 ? (
          <p className="text-center text-xs text-slate-500 py-4">검색 결과 없음</p>
        ) : (
          filtered.map((it) => {
            const granted = draftGranted.has(it.id);
            const changed = granted !== serverGranted.has(it.id);
            return (
              <div key={it.id} className={`flex items-center justify-between px-3 py-2 ${changed ? "bg-amber-500/5" : ""}`}>
                <div className="min-w-0">
                  <p className="text-white text-xs font-medium truncate flex items-center gap-1.5">
                    {it.title}
                    {changed && (
                      <span className="text-amber-400 text-[11px] font-normal flex-shrink-0">
                        {granted ? "+" : "−"}
                      </span>
                    )}
                  </p>
                  {it.subtitle && <p className="text-[11px] text-slate-500 truncate">{it.subtitle}</p>}
                </div>
                <button
                  disabled={disabled}
                  onClick={() => onToggle(it.id)}
                  className={`min-w-[64px] flex-shrink-0 ml-2 text-center text-[11px] py-1 px-2 rounded font-medium transition disabled:opacity-50 ${
                    granted
                      ? "bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 hover:bg-red-900/30 hover:text-red-300 hover:border-red-500/30"
                      : "bg-white/5 text-slate-400 border border-white/15 hover:bg-indigo-600/20 hover:text-indigo-300 hover:border-indigo-500/30"
                  }`}
                >
                  {granted ? "부여됨" : "미부여"}
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
