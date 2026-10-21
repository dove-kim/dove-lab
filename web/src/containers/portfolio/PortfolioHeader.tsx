"use client";

import { ScopeSelector } from "./scopeView";

/**
 * 포트폴리오 화면 공통 헤더 — 제목과 보기 대상 선택을 한 줄에 둔다.
 *
 * @param title    화면 제목
 * @param children 제목 오른쪽에 붙일 화면별 보조 컨트롤
 */
export default function PortfolioHeader({ title, children }: { title: string; children?: React.ReactNode }) {
  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      <h1 className="text-lg font-semibold text-white">{title}</h1>
      <ScopeSelector />
      {children}
    </div>
  );
}
