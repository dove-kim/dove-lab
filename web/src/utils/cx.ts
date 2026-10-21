import { buttonVariants } from "@/components/ui/button";
import { inputClassName } from "@/components/ui/input";
import { cn } from "@/utils/cn";

export const cx = {
  input: inputClassName,
  inputNumber: cn(
    inputClassName,
    "[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none",
  ),
  inputDate: cn(
    inputClassName,
    "w-auto [color-scheme:dark] [&::-webkit-calendar-picker-indicator]:opacity-40 [&::-webkit-calendar-picker-indicator]:invert",
  ),
  inputSm: cn(inputClassName, "px-2 py-1 text-xs sm:text-xs"),
  inputNumberSm: cn(
    inputClassName,
    "px-2 py-1 text-xs sm:text-xs [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none",
  ),
  select: cn(
    inputClassName,
    "w-auto bg-popover dark:bg-popover [&_optgroup]:bg-popover [&_option]:bg-popover",
  ),
  btnPrimary: buttonVariants(),
  btnSecondary: cn(buttonVariants({ variant: "outline" }), "px-4"),
  btnDanger: buttonVariants({ variant: "destructive" }),
  btnToggleOn: cn(buttonVariants(), "px-3"),
  btnToggleOff: cn(buttonVariants({ variant: "secondary" }), "px-3"),
  // Dialog 본문 셸 — 모바일 바텀시트, sm 이상 가운데 모달. 폭·높이는 호출부에서 덧붙인다.
  // div 가 아닌 태그(section 등)에서 쓰는 패널 모양 — div 라면 <Panel> 컴포넌트를 쓴다
  panel: "rounded-xl border border-border bg-surface",
  dialogSheet:
    "top-auto bottom-0 flex w-full max-w-full translate-y-0 flex-col gap-0 rounded-t-2xl rounded-b-none border border-border bg-card p-0 text-base shadow-2xl ring-0 sm:top-1/2 sm:bottom-auto sm:-translate-y-1/2 sm:rounded-2xl",
  tab: {
    list: "h-auto w-full justify-start gap-1 rounded-none border-b border-white/10 p-0",
    trigger:
      "-mb-px flex-none rounded-none px-4 py-3 text-sm font-medium after:-bottom-px after:bg-ring focus-visible:ring-0 focus-visible:outline-none data-active:text-indigo-300",
    // shadcn TabsContent 기본 text-sm 가 본문 글자 크기를 줄이지 않게 되돌린다.
    content: "text-base",
  },
  btnSm: cn(buttonVariants({ variant: "outline" }), "px-3 py-1.5 text-xs whitespace-nowrap"),
  btnDangerSm: cn(
    buttonVariants({ variant: "outline" }),
    "border-destructive/30 px-3 py-1.5 text-xs text-rose-300 whitespace-nowrap hover:bg-destructive/10 hover:text-rose-200",
  ),
  btnIcon: cn(buttonVariants({ variant: "ghost", size: "icon-sm" }), "text-slate-400 hover:text-white"),
  btnIconDanger: cn(buttonVariants({ variant: "ghost", size: "icon-sm" }), "text-slate-400 hover:text-rose-300"),
  // 세그먼트 컨트롤(보기 전환) — 테두리 하나에 버튼을 붙여 쓰는 묶음
  segment: {
    root: "flex h-9 overflow-hidden rounded-lg border border-border text-sm",
    on: "bg-primary px-3 text-primary-foreground",
    off: "px-3 text-slate-400 transition hover:text-white",
    disabled: "px-3 text-slate-600 cursor-not-allowed",
  },
  // 내용이 없을 때 자리를 지키는 안내 문구
  emptyState: "rounded-xl border border-border bg-slate-900/40 py-8 text-center text-sm text-slate-500",
  table: {
    root: "w-full caption-bottom text-sm",
    head: "border-b bg-card/95 backdrop-blur-sm",
    th: "px-4 py-2.5 text-left align-middle text-xs font-medium text-muted-foreground",
    body: "divide-y divide-border/30",
    tr: "transition-colors hover:bg-muted/50",
    td: "px-4 py-2.5 align-middle text-slate-300",
  },
};

