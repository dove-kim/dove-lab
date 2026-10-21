"use client";

import { useEffect, useState } from "react";
import Select from "@/components/Select";
import { cx } from "@/utils/cx";
import { MARKETS, natMoney } from "@/types/portfolio";
import type { SymbolChip } from "./symbolChips";

/** 고른 종목과 그에 딸린 시장·티커·통화. */
export interface SymbolSelection {
  symbol: string;
  market: string;
  ticker: string;
  currency: string;
  accountId?: number | null;
}

/** 시장 코드에 대응하는 통화(모르면 KRW). */
export function currencyOfMarket(market: string): string {
  return MARKETS.find((m) => m.value === market)?.currency ?? "KRW";
}

/**
 * 종목 고르기 — 최근·보유 종목은 칩 한 번으로, 그 밖의 종목은 거래소와 이름으로 찾는다.
 *
 * @param chips    고를 수 있는 종목 칩
 * @param value    현재 선택
 * @param onChange 선택이 바뀔 때
 */
export default function SymbolPicker({
  chips,
  value,
  onChange,
}: {
  chips: SymbolChip[];
  value: SymbolSelection;
  onChange: (next: SymbolSelection) => void;
}) {
  const [mode, setMode] = useState<"chips" | "search" | null>(null);
  const [hits, setHits] = useState<{ ticker: string; name: string; market: string }[]>([]);
  const [verify, setVerify] = useState<{ loading: boolean; valid: boolean; price: number | null } | null>(null);

  const derived = chips.length === 0 || (value.symbol && !chips.some((c) => c.symbol === value.symbol))
    ? "search"
    : "chips";
  const effective = mode ?? derived;
  const overseas = value.market !== "" && currencyOfMarket(value.market) !== "KRW";

  useEffect(() => setVerify(null), [value.market, value.ticker, value.symbol]);

  // 종목명 입력 시 국내 종목 검색(디바운스). 해외 거래소를 고른 상태면 티커=심볼이라 검색하지 않는다.
  useEffect(() => {
    if (effective !== "search" || overseas || !value.symbol.trim()) {
      setHits([]);
      return;
    }
    const t = setTimeout(() => {
      fetch(`/api/portfolio/stock-search?q=${encodeURIComponent(value.symbol.trim())}`)
        .then((r) => (r.ok ? r.json() : []))
        .then((d: { ticker: string; name: string; market: string }[]) => setHits(Array.isArray(d) ? d : []))
        .catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [value.symbol, effective, overseas]);

  function changeMarket(market: string) {
    const currency = currencyOfMarket(market);
    // 해외로 바꾸면 티커=심볼(대문자)로 확정해 국내 매칭 잔값이 섞이지 않게 한다.
    if (currency !== "KRW") {
      const up = value.symbol.trim().toUpperCase();
      onChange({ symbol: up, market, ticker: up, currency });
      return;
    }
    onChange({ ...value, market, currency });
  }

  function changeText(text: string) {
    if (overseas) {
      const up = text.toUpperCase();
      onChange({ ...value, symbol: up, ticker: up.trim() });
      return;
    }
    onChange({ ...value, symbol: text });
  }

  async function verifyOverseas() {
    if (!value.market || !value.ticker.trim()) return;
    setVerify({ loading: true, valid: false, price: null });
    try {
      const r = await fetch(
        `/api/portfolio/stock-search/overseas?market=${encodeURIComponent(value.market)}&ticker=${encodeURIComponent(value.ticker.trim())}`
      );
      const d = await r.json();
      setVerify({ loading: false, valid: !!d.valid, price: d.price ?? null });
    } catch {
      setVerify({ loading: false, valid: false, price: null });
    }
  }

  if (effective === "chips") {
    return (
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap gap-2">
          {chips.map((c) => (
            <button
              key={c.symbol}
              type="button"
              onClick={() => onChange({ ...c })}
              className={value.symbol === c.symbol ? cx.btnToggleOn : cx.btnToggleOff}
            >
              {c.symbol}
            </button>
          ))}
          <button type="button" onClick={() => setMode("search")} className={cx.btnSecondary}>
            목록에 없어요
          </button>
        </div>
        {value.market && value.ticker && (
          <p className="text-xs text-slate-500">
            {MARKETS.find((m) => m.value === value.market)?.label ?? value.market} · {value.ticker} · {value.currency}
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <Select
        value={value.market}
        onChange={changeMarket}
        placeholder="거래소 선택"
        items={MARKETS.map((m) => ({ value: m.value, label: m.label }))}
      />
      <input
        value={value.symbol}
        onChange={(e) => changeText(e.target.value)}
        placeholder="예: 삼성전자 / TSLA"
        className={cx.input}
      />
      {hits.length > 0 && value.symbol.trim() && (
        <ul className="max-h-56 divide-y divide-white/5 overflow-y-auto rounded-lg border border-white/10 bg-slate-900">
          {hits.slice(0, 5).map((h) => (
            <li key={h.ticker}>
              <button
                type="button"
                onClick={() => {
                  onChange({ symbol: h.name, market: h.market, ticker: h.ticker, currency: "KRW" });
                  setHits([]);
                }}
                className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left transition hover:bg-white/5"
              >
                <span className="text-sm text-white">{h.name}</span>
                <span className="text-xs text-slate-500">
                  {MARKETS.find((m) => m.value === h.market)?.label ?? h.market} · {h.ticker}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {overseas && value.ticker.trim() && (
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" onClick={verifyOverseas} className={cx.btnSecondary} disabled={verify?.loading}>
            {verify?.loading ? "확인 중…" : "시세 확인"}
          </button>
          {verify && !verify.loading && (
            verify.valid ? (
              <span className="text-xs text-emerald-300">✓ 현재가 {natMoney(verify.price ?? 0, value.currency)}</span>
            ) : (
              <span className="text-xs text-rose-300">✗ 조회 실패 — 티커·거래소를 확인하세요</span>
            )
          )}
        </div>
      )}
      {chips.length > 0 && (
        <button
          type="button"
          onClick={() => setMode("chips")}
          className="self-start text-xs text-slate-400 transition hover:text-white"
        >
          ▴ 최근 종목에서 고르기
        </button>
      )}
    </div>
  );
}
