"use client";

import { useEffect, useState } from "react";
import Select from "@/components/Select";
import { cx } from "@/utils/cx";
import type { PortfolioAccount, PortfolioShare } from "@/types/portfolio";

const KEY = "portfolio-scope";
const EVENT = "portfolio-scope-change";

/**
 * 보기 대상(scope) 상태 — "own"(내 전체) 또는 계좌 ID 문자열. 로컬 저장 + 같은 탭 내 동기화.
 * 리포트·보유·거래내역·라운드트립이 이 값을 따라 대상만 바꾼다(합산 없음).
 */
export function useScope(): [string, (id: string) => void] {
  const [scope, setScope] = useState<string>("own");
  useEffect(() => {
    const read = () => setScope(localStorage.getItem(KEY) || "own");
    read();
    window.addEventListener(EVENT, read);
    return () => window.removeEventListener(EVENT, read);
  }, []);
  const set = (id: string) => {
    localStorage.setItem(KEY, id);
    window.dispatchEvent(new Event(EVENT));
  };
  return [scope, set];
}

/** scope에 맞는 API 베이스 경로. own=내 계좌 전체, all=내것+공유받은것 합산, 그 외=특정 계좌(/shared/{id}). */
export function scopeBase(scope: string): string {
  if (scope === "own") return "/api/portfolio";
  if (scope === "all") return "/api/portfolio/all";
  return `/api/portfolio/shared/${scope}`;
}

/**
 * 보기 대상 선택자 — 내 전체 / 내 계좌별 / 공유받은 계좌.
 */
export function ScopeSelector() {
  const [scope, setScope] = useScope();
  const [own, setOwn] = useState<PortfolioAccount[]>([]);
  const [shared, setShared] = useState<PortfolioShare[]>([]);

  useEffect(() => {
    fetch("/api/portfolio/accounts")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioAccount[]) => setOwn(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/shares")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioShare[]) => setShared(Array.isArray(d) ? d.filter((s) => s.direction === "IN") : []))
      .catch(() => {});
  }, []);

  return (
    <Select
      value={scope}
      onChange={setScope}
      className="w-auto min-w-36"
      items={[
        { value: "own", label: "내 전체" },
        ...(shared.length > 0 ? [{ value: "all", label: "전체 (공유 포함)" }] : []),
        ...(own.length > 0
          ? [{ group: "내 계좌", options: own.map((a) => ({ value: String(a.id), label: a.name })) }]
          : []),
        ...(shared.length > 0
          ? [{
              group: "공유받은",
              options: shared.map((s) => ({ value: String(s.accountId), label: s.accountName + " · " + s.grantee })),
            }]
          : []),
      ]}
    />
  );
}
