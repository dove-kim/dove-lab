import { ComponentProps } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/utils/cn";

/**
 * 테두리 있는 내용 패널 — 표·입력줄·요약 블록을 감싸는 기본 컨테이너.
 */
export default function Panel({ className, ...props }: ComponentProps<typeof Card>) {
  return (
    <Card
      className={cn(
        "block flex-row gap-0 overflow-visible rounded-xl border border-border bg-surface py-0 text-base ring-0",
        className,
      )}
      {...props}
    />
  );
}
