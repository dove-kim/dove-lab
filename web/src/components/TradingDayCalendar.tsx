"use client";

import { useState, useRef, useEffect, useMemo } from "react";

interface Props {
  value: string;         // "YYYY-MM-DD"
  tradingDays: string[]; // 선택 가능한 거래일 목록
  onChange: (date: string) => void;
}

const DOW_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

function pad(n: number) {
  return String(n).padStart(2, "0");
}

function daysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

function firstDowOfMonth(year: number, month: number) {
  return new Date(year, month - 1, 1).getDay(); // 0=일
}

export default function TradingDayCalendar({ value, tradingDays, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear]   = useState(() => parseInt(value.slice(0, 4)));
  const [viewMonth, setViewMonth] = useState(() => parseInt(value.slice(5, 7)));
  const wrapRef = useRef<HTMLDivElement>(null);

  const tradingSet = useMemo(() => new Set(tradingDays), [tradingDays]);

  // 바깥 클릭 / Escape 닫기
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  // 선택값 바뀌면 뷰 동기화
  useEffect(() => {
    setViewYear(parseInt(value.slice(0, 4)));
    setViewMonth(parseInt(value.slice(5, 7)));
  }, [value]);

  function prevMonth() {
    if (viewMonth === 1) { setViewYear(y => y - 1); setViewMonth(12); }
    else setViewMonth(m => m - 1);
  }
  function nextMonth() {
    if (viewMonth === 12) { setViewYear(y => y + 1); setViewMonth(1); }
    else setViewMonth(m => m + 1);
  }

  function handleDay(d: string) {
    if (!tradingSet.has(d)) return;
    onChange(d);
    setOpen(false);
  }

  // 캘린더 셀 구성 (앞 빈칸 + 날짜 + 뒷 빈칸)
  const offset = firstDowOfMonth(viewYear, viewMonth);
  const total  = daysInMonth(viewYear, viewMonth);
  const cells: (string | null)[] = [
    ...Array(offset).fill(null),
    ...Array.from({ length: total }, (_, i) =>
      `${viewYear}-${pad(viewMonth)}-${pad(i + 1)}`
    ),
  ];
  while (cells.length % 7 !== 0) cells.push(null);

  // 트리거 버튼 텍스트
  const [, vm, vd] = value.split("-");
  const triggerDow = DOW_LABELS[new Date(+value.slice(0, 4), +vm - 1, +vd).getDay()];
  const triggerText = `${parseInt(vm)}월 ${parseInt(vd)}일 (${triggerDow})`;

  return (
    <div ref={wrapRef} className="relative">
      {/* 트리거 */}
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="flex items-center gap-2 bg-slate-800 border border-white/15 rounded-lg px-3 py-2 text-white text-sm hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-400/50 transition min-w-[148px]"
      >
        <svg className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" />
          <line x1="16" y1="2" x2="16" y2="6" />
          <line x1="8"  y1="2" x2="8"  y2="6" />
          <line x1="3"  y1="10" x2="21" y2="10" />
        </svg>
        <span>{triggerText}</span>
      </button>

      {/* 달력 패널 */}
      {open && (
        <div className="absolute top-full mt-1.5 left-0 z-50 bg-slate-800 border border-white/15 rounded-xl shadow-2xl p-3 w-64 select-none">
          {/* 월 네비게이션 */}
          <div className="flex items-center justify-between mb-2">
            <button onClick={prevMonth} className="w-7 h-7 flex items-center justify-center rounded text-slate-400 hover:text-white hover:bg-white/10 transition">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <span className="text-sm font-medium text-white">{viewYear}년 {viewMonth}월</span>
            <button onClick={nextMonth} className="w-7 h-7 flex items-center justify-center rounded text-slate-400 hover:text-white hover:bg-white/10 transition">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>

          {/* 요일 헤더 */}
          <div className="grid grid-cols-7 mb-1">
            {DOW_LABELS.map((label, i) => (
              <div
                key={label}
                className={`text-center text-xs py-1 font-medium
                  ${i === 0 ? "text-red-400/70" : i === 6 ? "text-blue-400/70" : "text-slate-500"}`}
              >
                {label}
              </div>
            ))}
          </div>

          {/* 날짜 셀 */}
          <div className="grid grid-cols-7 gap-y-0.5">
            {cells.map((cell, i) => {
              if (!cell) return <div key={`e${i}`} />;
              const isTrading  = tradingSet.has(cell);
              const isSelected = cell === value;
              const dow        = i % 7;
              const dayNum     = parseInt(cell.slice(8));
              const colorClass = dow === 0 ? "text-red-400" : dow === 6 ? "text-blue-400" : "";

              return (
                <button
                  key={cell}
                  type="button"
                  onClick={() => handleDay(cell)}
                  disabled={!isTrading}
                  className={`
                    text-xs rounded py-1.5 transition text-center leading-none
                    ${isSelected
                      ? "bg-indigo-600 text-white font-semibold"
                      : isTrading
                        ? `hover:bg-white/10 ${colorClass || "text-slate-200"}`
                        : `opacity-20 cursor-default ${colorClass || "text-slate-500"}`
                    }
                  `}
                >
                  {dayNum}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

