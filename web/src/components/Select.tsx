"use client";

import { useState, useRef, useEffect } from "react";

export type SelectItem =
  | { value: string; label: string; disabled?: boolean }
  | { group: string; options: { value: string; label: string; disabled?: boolean }[] };

interface Props {
  value: string | null | undefined;
  items: SelectItem[];
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string; // 래퍼 너비 (예: "w-full", "w-44")
}

type Entry =
  | { kind: "group"; label: string }
  | { kind: "opt"; value: string; label: string; disabled?: boolean; idx: number; inGroup: boolean };

function toEntries(items: SelectItem[]): Entry[] {
  const result: Entry[] = [];
  let idx = 0;
  for (const item of items) {
    if ("group" in item) {
      result.push({ kind: "group", label: item.group });
      for (const opt of item.options) {
        result.push({ kind: "opt", ...opt, idx: idx++, inGroup: true });
      }
    } else {
      result.push({ kind: "opt", ...item, idx: idx++, inGroup: false });
    }
  }
  return result;
}

export default function Select({ value, items, onChange, placeholder = "선택", disabled, className }: Props) {
  const [open, setOpen]          = useState(false);
  const [focusedIdx, setFocused] = useState(-1);
  const wrapRef  = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const entries  = toEntries(items);
  const flatOpts = entries.filter((e): e is Extract<Entry, { kind: "opt" }> => e.kind === "opt");
  const selected = flatOpts.find(o => o.value === value);

  // 바깥 클릭 닫기
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  // 열릴 때 선택된 항목으로 포커스 + 스크롤
  useEffect(() => {
    if (!open) return;
    const idx = flatOpts.findIndex(o => o.value === value);
    const init = idx >= 0 ? idx : 0;
    setFocused(init);
    requestAnimationFrame(() => {
      panelRef.current?.querySelector<HTMLElement>(`[data-idx="${init}"]`)?.scrollIntoView({ block: "nearest" });
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function moveFocus(dir: 1 | -1) {
    let next = focusedIdx + dir;
    while (next >= 0 && next < flatOpts.length) {
      if (!flatOpts[next].disabled) {
        setFocused(next);
        panelRef.current?.querySelector<HTMLElement>(`[data-idx="${next}"]`)?.scrollIntoView({ block: "nearest" });
        return;
      }
      next += dir;
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (!open) {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); setOpen(true); }
      return;
    }
    if (e.key === "Escape")    { setOpen(false); return; }
    if (e.key === "ArrowDown") { e.preventDefault(); moveFocus(1);  return; }
    if (e.key === "ArrowUp")   { e.preventDefault(); moveFocus(-1); return; }
    if (e.key === "Enter") {
      const opt = flatOpts[focusedIdx];
      if (opt && !opt.disabled) { onChange(opt.value); setOpen(false); }
    }
  }

  function handleSelect(opt: Extract<Entry, { kind: "opt" }>) {
    if (opt.disabled) return;
    onChange(opt.value);
    setOpen(false);
  }

  return (
    <div ref={wrapRef} className={`relative ${className ?? ""}`}>
      {/* 트리거 */}
      <button
        type="button"
        role="combobox"
        aria-expanded={open}
        aria-haspopup="listbox"
        disabled={disabled}
        onClick={() => !disabled && setOpen(o => !o)}
        onKeyDown={handleKeyDown}
        className="w-full flex items-center justify-between gap-2 bg-slate-800 border border-white/15 rounded-lg px-3 py-2 text-sm text-left hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-400/50 transition disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <span className={selected ? "text-white" : "text-slate-500"}>
          {selected?.label ?? placeholder}
        </span>
        <svg
          className={`w-3.5 h-3.5 text-slate-400 flex-shrink-0 transition-transform duration-150 ${open ? "rotate-180" : ""}`}
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
          strokeLinecap="round" strokeLinejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {/* 드롭다운 패널 */}
      {open && (
        <div
          role="listbox"
          ref={panelRef}
          className="absolute left-0 top-full mt-1.5 z-50 min-w-full bg-slate-800 border border-white/15 rounded-xl shadow-2xl overflow-y-auto py-1"
          style={{ maxHeight: 280 }}
        >
          {entries.map((entry, i) =>
            entry.kind === "group" ? (
              <div key={`g${i}`} className="px-3 pt-2.5 pb-1 text-xs text-slate-500 font-medium tracking-wide select-none">
                {entry.label}
              </div>
            ) : (
              <div
                key={entry.value}
                data-idx={entry.idx}
                role="option"
                aria-selected={entry.value === value}
                onMouseDown={() => handleSelect(entry)}
                onMouseEnter={() => !entry.disabled && setFocused(entry.idx)}
                className={[
                  "py-2 text-sm cursor-pointer transition-colors select-none",
                  entry.inGroup ? "px-5" : "px-3",
                  entry.disabled     ? "opacity-30 cursor-default"                  : "",
                  entry.value === value ? "bg-indigo-600/20 text-indigo-300 font-medium" : "",
                  entry.idx === focusedIdx && entry.value !== value && !entry.disabled
                    ? "bg-white/8 text-white"
                    : "",
                  entry.value !== value && entry.idx !== focusedIdx ? "text-slate-300" : "",
                ].filter(Boolean).join(" ")}
              >
                {entry.label}
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}

