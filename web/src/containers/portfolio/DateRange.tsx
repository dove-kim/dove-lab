"use client";

import { useState } from "react";
import { cx } from "@/utils/cx";

/**
 * 날짜 범위 필터 상태. inRange(date)로 행을 거른다(날짜 없는 행은 통과).
 */
export function useDateRange() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const key = `${from}|${to}`;
  const inRange = (dateStr?: string | null) => {
    if (!dateStr) return true;
    const d = dateStr.slice(0, 10);
    if (from && d < from) return false;
    if (to && d > to) return false;
    return true;
  };
  return { from, to, setFrom, setTo, key, inRange };
}

/**
 * 날짜 범위 입력(시작~종료 + 초기화).
 */
export function DateRangeFilter({
  from,
  to,
  setFrom,
  setTo,
}: {
  from: string;
  to: string;
  setFrom: (v: string) => void;
  setTo: (v: string) => void;
}) {
  return (
    <div className="flex items-center gap-1">
      <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={cx.inputDate} aria-label="시작일" />
      <span className="text-slate-500 text-sm">~</span>
      <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={cx.inputDate} aria-label="종료일" />
      {(from || to) && (
        <button
          onClick={() => {
            setFrom("");
            setTo("");
          }}
          className="text-xs text-slate-400 hover:text-white transition px-1"
        >
          초기화
        </button>
      )}
    </div>
  );
}
