"use client";

import { useEffect, useState } from "react";

interface InviteCode {
  id: number;
  code: string;
  role: string;
  expiresAt: string;
  usedAt: string | null;
  createdBy: string;
  createdAt: string;
}

export default function InviteCodesClient() {
  const [codes, setCodes] = useState<InviteCode[]>([]);
  const [role, setRole] = useState<"USER" | "ADMIN">("USER");
  const [expireDays, setExpireDays] = useState(7);
  const [newCode, setNewCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchCodes = async () => {
    const res = await fetch("/api/admin/invite-codes");
    if (res.status === 401) { window.location.href = "/login"; return; }
    if (res.ok) setCodes(await res.json());
  };

  useEffect(() => { fetchCodes(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setNewCode(null);
    try {
      const res = await fetch("/api/admin/invite-codes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role, expireDays }),
      });
      if (res.status === 401) { window.location.href = "/login"; return; }
      if (res.ok) {
        const data = await res.json();
        setNewCode(`${window.location.origin}/register?code=${data.code}`);
        await fetchCodes();
      }
    } finally {
      setLoading(false);
    }
  };

  const fmt = (dt: string) =>
    new Date(dt).toLocaleDateString("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit",
    });

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h2 className="text-white text-lg font-semibold mb-6">초대 코드 관리</h2>

      {/* 발급 폼 */}
      <div className="bg-white/5 border border-white/10 rounded-xl p-5 mb-6 max-w-md">
        <h3 className="text-slate-300 text-sm font-medium mb-4">새 초대 코드 발급</h3>
        <form onSubmit={handleCreate} className="flex flex-col gap-3">
          <div className="flex gap-3">
            <div className="flex flex-col gap-1 flex-1">
              <label className="text-xs text-slate-400">역할</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as "USER" | "ADMIN")}
                className="px-3 py-2 rounded-lg bg-white/8 border border-white/15 text-white outline-none focus:ring-2 focus:ring-indigo-400/50 text-sm cursor-pointer"
              >
                <option value="USER" className="bg-slate-800">USER</option>
                <option value="ADMIN" className="bg-slate-800">ADMIN</option>
              </select>
            </div>
            <div className="flex flex-col gap-1 flex-1">
              <label className="text-xs text-slate-400">유효 기간 (일)</label>
              <input
                type="number"
                min={1}
                max={365}
                value={expireDays}
                onChange={(e) => setExpireDays(Number(e.target.value))}
                className="px-3 py-2 rounded-lg bg-white/8 border border-white/15 text-white outline-none focus:ring-2 focus:ring-indigo-400/50 text-sm [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium transition disabled:opacity-50 cursor-pointer"
          >
            {loading ? "발급 중..." : "코드 발급"}
          </button>
        </form>

        {newCode && (
          <div className="mt-4 p-3 bg-indigo-900/40 border border-indigo-400/30 rounded-lg">
            <p className="text-xs text-slate-400 mb-1">초대 링크 (한 번만 표시됩니다)</p>
            <p className="text-indigo-300 font-mono text-sm break-all">{newCode}</p>
          </div>
        )}
      </div>

      {/* 코드 목록 */}
      <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/10 text-slate-400 text-xs">
              <th className="text-left px-4 py-3">코드</th>
              <th className="text-left px-4 py-3">역할</th>
              <th className="text-left px-4 py-3">만료일</th>
              <th className="text-left px-4 py-3">상태</th>
              <th className="text-left px-4 py-3">발급자</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {codes.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center text-slate-500 py-8">발급된 코드가 없습니다</td>
              </tr>
            ) : codes.map((c) => (
              <tr key={c.id} className="hover:bg-white/3">
                <td className="px-4 py-3 font-mono text-slate-300 text-xs">{c.code.slice(0, 12)}...</td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${c.role === "ADMIN" ? "bg-indigo-600/30 text-indigo-300" : "bg-slate-600/30 text-slate-300"}`}>
                    {c.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-slate-400 text-xs">{fmt(c.expiresAt)}</td>
                <td className="px-4 py-3">
                  {c.usedAt ? (
                    <span className="text-xs text-slate-500">사용됨</span>
                  ) : new Date(c.expiresAt) < new Date() ? (
                    <span className="text-xs text-red-400">만료</span>
                  ) : (
                    <span className="text-xs text-emerald-400">유효</span>
                  )}
                </td>
                <td className="px-4 py-3 text-slate-400 text-xs">{c.createdBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
