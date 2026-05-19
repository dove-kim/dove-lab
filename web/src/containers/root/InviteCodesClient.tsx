"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";
import Select from "@/components/Select";
import { clientFetch } from "@/services/client";

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
  const [error, setError] = useState<string | null>(null);

  const fetchCodes = async () => {
    const res = await clientFetch("/api/root/invite-codes");
    if (res?.ok) {
      setCodes(await res.json());
    } else if (res) {
      setError(`목록 로드 실패 (HTTP ${res.status})`);
    }
  };

  useEffect(() => { fetchCodes(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setNewCode(null);
    setError(null);
    try {
      const res = await clientFetch("/api/root/invite-codes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ role, expireDays }),
      });
      if (res?.ok) {
        const data = await res.json();
        setNewCode(`${window.location.origin}/register?code=${data.code}`);
        await fetchCodes();
      } else if (res) {
        const body = await res.json().catch(() => ({}));
        setError(`생성 실패 (HTTP ${res.status}${body?.error ? ` · ${body.error}` : ""})`);
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

      <div className="bg-white/5 border border-white/10 rounded-xl p-5 mb-6 max-w-md">
        <h3 className="text-slate-300 text-sm font-medium mb-4">새 초대 코드 생성</h3>
        <form onSubmit={handleCreate} className="flex flex-col gap-3">
          <div className="flex gap-3">
            <div className="flex flex-col gap-1 flex-1">
              <label className="text-xs text-slate-400">역할</label>
              <Select
                value={role}
                items={[{ value: "USER", label: "USER" }, { value: "ADMIN", label: "ADMIN" }]}
                onChange={(v) => setRole(v as "USER" | "ADMIN")}
                className="w-full"
              />
            </div>
            <div className="flex flex-col gap-1 flex-1">
              <label className="text-xs text-slate-400">유효 기간 (일)</label>
              <input
                type="number"
                min={1}
                max={365}
                value={expireDays}
                onChange={(e) => setExpireDays(Number(e.target.value))}
                className={cx.inputNumber}
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={loading}
            className={`w-full ${cx.btnPrimary}`}
          >
            {loading ? "생성 중..." : "코드 생성"}
          </button>
        </form>

        {error && (
          <div className="mt-3 p-3 bg-red-900/30 border border-red-500/30 rounded-lg">
            <p className="text-red-400 text-xs">{error}</p>
          </div>
        )}

        {newCode && (
          <div className="mt-4 p-3 bg-slate-700/40 border border-white/25 rounded-lg">
            <p className="text-xs text-slate-400 mb-1">초대 링크 (한 번만 표시됩니다)</p>
            <p className="text-indigo-300 font-mono text-sm break-all">{newCode}</p>
          </div>
        )}
      </div>

      <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead className={cx.table.head}>
            <tr>
              <th className={cx.table.th}>코드</th>
              <th className={cx.table.th}>역할</th>
              <th className={cx.table.th}>만료일</th>
              <th className={cx.table.th}>상태</th>
              <th className={cx.table.th}>생성자</th>
            </tr>
          </thead>
          <tbody className={cx.table.body}>
            {codes.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center text-slate-500 py-8">생성된 코드가 없습니다</td>
              </tr>
            ) : codes.map((c) => (
              <tr key={c.id} className={cx.table.tr}>
                <td className={`${cx.table.td} font-mono text-xs`}>{c.code.slice(0, 12)}...</td>
                <td className={cx.table.td}>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${c.role === "ADMIN" ? "bg-indigo-600/30 text-indigo-300" : "bg-slate-600/30 text-slate-300"}`}>
                    {c.role}
                  </span>
                </td>
                <td className={`${cx.table.td} text-xs`}>{fmt(c.expiresAt)}</td>
                <td className={cx.table.td}>
                  {c.usedAt ? (
                    <span className="text-xs text-slate-500">사용됨</span>
                  ) : new Date(c.expiresAt) < new Date() ? (
                    <span className="text-xs text-red-400">만료</span>
                  ) : (
                    <span className="text-xs text-emerald-400">유효</span>
                  )}
                </td>
                <td className={`${cx.table.td} text-xs`}>{c.createdBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

