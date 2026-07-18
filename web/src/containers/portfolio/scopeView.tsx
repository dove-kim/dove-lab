"use client";

import { useEffect, useState } from "react";
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

/** scope 설정용 훅에서 외부(예: 공유 탭)가 대상을 바꾸도록 노출. */
export function setScopeExternally(id: string) {
  localStorage.setItem(KEY, id);
  window.dispatchEvent(new Event(EVENT));
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
    <select value={scope} onChange={(e) => setScope(e.target.value)} className={cx.select} title="보기 대상">
      <option value="own">내 전체</option>
      {shared.length > 0 && <option value="all">전체 (공유 포함)</option>}
      {own.length > 0 && (
        <optgroup label="내 계좌">
          {own.map((a) => (
            <option key={a.id} value={String(a.id)}>
              {a.name}
            </option>
          ))}
        </optgroup>
      )}
      {shared.length > 0 && (
        <optgroup label="공유받은">
          {shared.map((s) => (
            <option key={s.id} value={String(s.accountId)}>
              {s.accountName} · {s.grantee}
            </option>
          ))}
        </optgroup>
      )}
    </select>
  );
}
