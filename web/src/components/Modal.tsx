"use client";

import { ReactNode } from "react";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/utils/cn";
import { cx } from "@/utils/cx";

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
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        showCloseButton={false}
        aria-describedby={undefined}
        onInteractOutside={(e) => e.preventDefault()}
        className={cn(cx.dialogSheet, "max-h-[92vh] sm:max-w-lg")}
      >
        <div className="flex flex-shrink-0 items-center justify-between border-b border-white/10 px-5 py-4">
          <DialogTitle className="text-base font-semibold text-white">{title}</DialogTitle>
          <DialogClose
            aria-label="닫기"
            className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg text-slate-400 transition hover:bg-white/8 hover:text-white"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </DialogClose>
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>
        {footer && (
          <div className="flex flex-shrink-0 justify-end gap-2 border-t border-white/10 px-5 py-4">{footer}</div>
        )}
      </DialogContent>
    </Dialog>
  );
}
