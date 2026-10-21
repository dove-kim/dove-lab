"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Pagination as UiPagination,
  PaginationContent,
  PaginationItem,
} from "@/components/ui/pagination";

/**
 * 클라이언트 페이징 훅 — 전체 목록을 페이지 단위로 잘라 반환한다.
 * resetKey(필터·대상 등)가 바뀌면 1페이지로 되돌린다.
 *
 * @param items    전체 목록(필터 적용 후)
 * @param resetKey 바뀌면 1페이지로 리셋할 키
 * @param pageSize 페이지당 행 수
 */
export function usePaged<T>(items: T[], resetKey: string, pageSize = 50) {
  const [page, setPage] = useState(1);
  useEffect(() => {
    setPage(1);
  }, [resetKey]);
  const pageCount = Math.max(1, Math.ceil(items.length / pageSize));
  const cur = Math.min(page, pageCount);
  return {
    rows: items.slice((cur - 1) * pageSize, cur * pageSize),
    page: cur,
    pageCount,
    setPage,
    total: items.length,
    from: items.length ? (cur - 1) * pageSize + 1 : 0,
    to: Math.min(cur * pageSize, items.length),
  };
}

/**
 * 페이지 이동 컨트롤(이전/다음 + 범위 표시).
 */
export function Pagination({
  page,
  pageCount,
  from,
  to,
  total,
  onPage,
}: {
  page: number;
  pageCount: number;
  from: number;
  to: number;
  total: number;
  onPage: (p: number) => void;
}) {
  if (total === 0) return null;
  return (
    <div className="flex items-center justify-between gap-2 text-sm text-muted-foreground">
      <span className="tabular-nums">
        {from}–{to} <span className="text-slate-600">/ {total}</span>
      </span>
      <UiPagination className="mx-0 w-auto justify-end">
        <PaginationContent className="gap-2">
          <PaginationItem>
            <Button
              variant="outline"
              className="h-auto px-4 py-2 disabled:opacity-40"
              disabled={page <= 1}
              onClick={() => onPage(page - 1)}
            >
              이전
            </Button>
          </PaginationItem>
          <PaginationItem className="min-w-16 text-center tabular-nums text-slate-300">
            {page} / {pageCount}
          </PaginationItem>
          <PaginationItem>
            <Button
              variant="outline"
              className="h-auto px-4 py-2 disabled:opacity-40"
              disabled={page >= pageCount}
              onClick={() => onPage(page + 1)}
            >
              다음
            </Button>
          </PaginationItem>
        </PaginationContent>
      </UiPagination>
    </div>
  );
}
