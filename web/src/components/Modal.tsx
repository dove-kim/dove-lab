"use client";

import { ReactNode, useEffect } from "react";

/**
 * 공용 모달 — 화면 전체를 덮는 고정 모달(모바일은 바텀시트). Esc·닫기 버튼으로 닫힘(입력 중 실수 방지 위해 배경 클릭으론 안 닫힘).
 *
 * @param title    상단 제목
 * @param onClose  닫기 콜백
 * @param children 본문
 * @param footer   하단 액션 영역(선택)
 */
export default function Modal({
  title,
  onClose,
  children,
  footer,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-sm sm:p-4">
      <div className="w-full sm:max-w-lg max-h-[92vh] flex flex-col bg-slate-900 border border-white/10 rounded-t-2xl sm:rounded-2xl shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/10 flex-shrink-0">
          <h2 className="text-base font-semibold text-white">{title}</h2>
          <button
            onClick={onClose}
            className="w-9 h-9 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/8 transition cursor-pointer"
            aria-label="닫기"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>
        {footer && (
          <div className="px-5 py-4 border-t border-white/10 flex justify-end gap-2 flex-shrink-0">{footer}</div>
        )}
      </div>
    </div>
  );
}
