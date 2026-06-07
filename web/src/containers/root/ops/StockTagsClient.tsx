"use client";

import { useMemo, useState } from "react";
import { cx } from "@/utils/cx";
import type {
  StockTagFieldGroup,
  StockTagsResponse,
} from "@/types/stock-tag";

interface Props {
  initial: StockTagsResponse | null;
}

const SOURCE_LABEL: Record<string, string> = { KRX: "KRX", KIS: "KIS" };

export default function StockTagsClient({ initial }: Props) {
  const [tags] = useState<StockTagsResponse | null>(initial);
  // value id → 현재 라벨 입력값
  const [drafts, setDrafts] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  // 활성 탭 (field 이름)
  const [activeField, setActiveField] = useState<string>("");

  const categoryFields = useMemo(
    () => (tags?.tagFields ?? []).filter((f) => f.type === "CATEGORY"),
    [tags]
  );

  const activeGroup = useMemo(
    () =>
      categoryFields.find((f) => f.field === activeField) ??
      categoryFields[0] ??
      null,
    [categoryFields, activeField]
  );

  function draftOf(id: number, current: string): string {
    return drafts[id] ?? current;
  }

  async function handleSave(id: number, value: string) {
    const label = (drafts[id] ?? value).trim();
    if (!label) return;
    setSavingId(id);
    setMessage(null);
    try {
      const res = await fetch(`/api/admin/stock-tags/${id}/label`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ label }),
      });
      if (!res.ok && res.status !== 204) {
        setMessage(`저장에 실패했습니다. (${res.status})`);
        return;
      }
      setMessage("표시명을 저장했습니다.");
    } catch {
      setMessage("요청 중 오류가 발생했습니다.");
    } finally {
      setSavingId(null);
    }
  }

  if (!tags) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-white/10 py-12 text-center text-slate-400">
          분류 정보를 불러오지 못했습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-full p-6 gap-4">
      <div>
        <h1 className="text-xl font-bold text-white">분류 표시명</h1>
        <p className="text-sm text-slate-400 mt-1">
          수집된 분류 값(KRX·KIS 원문)의 표시명을 편집합니다. 원문값은 그대로 두고 화면에 보이는
          이름만 바꿉니다. 필터·검색 매칭은 항상 원문값 기준입니다.
        </p>
      </div>

      {message && (
        <p className="rounded-lg bg-emerald-900/40 border border-emerald-400/30 px-4 py-3 text-sm text-emerald-200">
          {message}
        </p>
      )}

      {/* 차원(field) 탭 */}
      <div className="flex flex-wrap gap-2 border-b border-white/10 pb-2">
        {categoryFields.map((group: StockTagFieldGroup) => {
          const active = activeGroup?.field === group.field;
          return (
            <button
              key={group.field}
              type="button"
              onClick={() => setActiveField(group.field)}
              className={`px-3 py-1.5 rounded-lg text-sm flex items-center gap-1.5 transition ${
                active
                  ? "bg-indigo-600 text-white"
                  : "bg-white/5 text-slate-400 border border-white/10 hover:text-white"
              }`}
            >
              {group.label}
              <span
                className={`text-[10px] px-1.5 py-0.5 rounded ${
                  active ? "bg-indigo-500/40 text-indigo-100" : "bg-slate-700 text-slate-300"
                }`}
              >
                {SOURCE_LABEL[group.source] ?? group.source}
              </span>
              <span className={active ? "text-indigo-200 text-xs" : "text-slate-500 text-xs"}>
                {group.values.length}
              </span>
            </button>
          );
        })}
      </div>

      {/* 활성 탭의 값 목록 */}
      <div className="pr-1">
        {!activeGroup ? (
          <div className="text-sm text-slate-500">분류 차원이 없습니다.</div>
        ) : activeGroup.values.length === 0 ? (
          <div className="text-sm text-slate-500">아직 수집된 값이 없습니다.</div>
        ) : (
          <div className="overflow-auto rounded-lg border border-white/10">
            <table className={cx.table.root}>
              <thead className={cx.table.head}>
                <tr>
                  <th className={cx.table.th + " w-1/3"}>원문값</th>
                  <th className={cx.table.th}>표시명</th>
                  <th className={cx.table.th + " w-28"}></th>
                </tr>
              </thead>
              <tbody className={cx.table.body}>
                {activeGroup.values.map((v) => (
                  <tr key={v.id} className={cx.table.tr}>
                    <td className={cx.table.td}>
                      <code className="rounded bg-slate-700 px-1.5 py-0.5 text-sm text-slate-100">
                        {v.value}
                      </code>
                    </td>
                    <td className={cx.table.td}>
                      <input
                        className={cx.input}
                        value={draftOf(v.id, v.label)}
                        onChange={(e) =>
                          setDrafts({ ...drafts, [v.id]: e.target.value })
                        }
                      />
                    </td>
                    <td className={cx.table.td}>
                      <button
                        onClick={() => handleSave(v.id, v.label)}
                        disabled={savingId === v.id}
                        className={cx.btnPrimary + " whitespace-nowrap"}
                      >
                        {savingId === v.id ? "저장 중…" : "저장"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
