"use client";

import { useEffect, useState } from "react";
import { type PortfolioPosition } from "@/types/portfolio";

/** 포트폴리오 목/백엔드 엔드포인트를 읽어오는 공통 훅. */
export function usePortfolioData<T>(url: string): { data: T | null; err: boolean } {
  const [data, setData] = useState<T | null>(null);
  const [err, setErr] = useState(false);
  useEffect(() => {
    let live = true;
    // 이전 데이터는 유지한 채 재검증(stale-while-revalidate) — URL 변경/재조회 시 화면 깜박임 방지.
    setErr(false);
    fetch(url, { cache: "no-store" })
      .then((r) => {
        if (!r.ok) throw new Error();
        return r.json();
      })
      .then((d: T) => live && setData(d))
      .catch(() => live && setErr(true));
    return () => {
      live = false;
    };
  }, [url]);
  return { data, err };
}

export const won = (n: number) => n.toLocaleString("ko-KR");
export const signed = (n: number) => (n >= 0 ? "+" : "") + won(n);
/** 손익 부호에 따른 색(한국 관례: 이익=빨강, 손실=파랑). */
export const pnlColor = (n: number) => (n > 0 ? "text-rose-300" : n < 0 ? "text-sky-300" : "text-slate-300");

/**
 * 보유 포지션에서 역산한 원통화→원화 환율(원통화 평가액이 0이면 null).
 */
export function fxRateOf(p: PortfolioPosition): number | null {
  const nat = p.curPriceNat * p.quantity;
  return nat > 0 ? p.evalKrw / nat : null;
}
