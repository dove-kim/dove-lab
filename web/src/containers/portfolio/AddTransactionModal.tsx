"use client";

import { useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import CommaInput from "@/components/CommaInput";
import { cx } from "@/utils/cx";
import { CURRENCIES, MARKETS, TX_TYPE_LABEL, natMoney, type PortfolioTx, type TxType } from "@/types/portfolio";

interface AccountOption {
  id: number;
  name: string;
}

const TRADE_TYPES: TxType[] = ["BUY", "SELL", "DEPOSIT", "WITHDRAW", "DIVIDEND", "INTEREST"];
const STEP_LABEL: Record<string, string> = {
  basic: "유형·계좌",
  stock: "거래소·종목",
  trade: "매매정보",
  extra: "추가사항",
};

const numText = (v?: number) => (v == null ? "" : String(v));

/**
 * 거래 추가/수정 모달 — 추가는 단계별 위저드(유형·계좌 → 거래소·종목 → 매매정보 → 추가사항), 수정은 단일 폼.
 * 거래소·종목을 단계로 받아 항상 종목 매핑(현재가·환율 자동조회)이 붙는다.
 *
 * @param accounts    선택 가능한 계좌 목록
 * @param initial     수정 대상 거래(없으면 신규)
 * @param defaultType 신규 시 기본 유형
 * @param onClose     닫기
 * @param onSaved     저장 성공 후 콜백
 */
export default function AddTransactionModal({
  accounts,
  initial,
  defaultType,
  createUrl = "/api/portfolio/transactions",
  holdingLink = true,
  onClose,
  onSaved,
}: {
  accounts: AccountOption[];
  initial?: PortfolioTx;
  defaultType?: TxType;
  createUrl?: string;
  holdingLink?: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!initial;
  const [accountId, setAccountId] = useState<number | "">(initial?.accountId ?? accounts[0]?.id ?? "");
  const [type, setType] = useState<TxType>(initial?.type ?? defaultType ?? "BUY");
  const [tradedAt, setTradedAt] = useState(initial?.tradedAt ?? new Date().toISOString().slice(0, 10));
  const [symbol, setSymbol] = useState(initial?.symbol ?? "");
  const [currency, setCurrency] = useState(initial?.currency ?? "KRW");
  const [quantity, setQuantity] = useState(numText(initial?.quantity));
  const [price, setPrice] = useState(numText(initial?.price));
  const [amount, setAmount] = useState(numText(initial?.amount));
  const [fee, setFee] = useState(numText(initial?.fee));
  const [tag, setTag] = useState(initial?.tag ?? "");
  const [memo, setMemo] = useState(initial?.memo ?? "");
  const [market, setMarket] = useState("");
  const [ticker, setTicker] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showAdvanced, setShowAdvanced] = useState(
    !!initial && ((initial.currency ?? "KRW") !== "KRW" || !!initial.fee || !!initial.tag || !!initial.memo)
  );
  const [known, setKnown] = useState<{ symbol: string; market: string; ticker: string; currency: string }[]>([]);
  const [searchHits, setSearchHits] = useState<{ ticker: string; name: string; market: string }[]>([]);
  const [step, setStep] = useState(0);
  const [verify, setVerify] = useState<{ loading: boolean; valid: boolean; price: number | null } | null>(null);
  const [pickedName, setPickedName] = useState<string | null>(null);

  useEffect(() => setVerify(null), [market, ticker, symbol]);

  async function verifyOverseas() {
    if (!market || !ticker.trim()) return;
    setVerify({ loading: true, valid: false, price: null });
    try {
      const r = await fetch(
        `/api/portfolio/stock-search/overseas?market=${encodeURIComponent(market)}&ticker=${encodeURIComponent(ticker.trim())}`
      );
      const d = await r.json();
      setVerify({ loading: false, valid: !!d.valid, price: d.price ?? null });
    } catch {
      setVerify({ loading: false, valid: false, price: null });
    }
  }

  useEffect(() => {
    fetch("/api/portfolio/holdings")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: { symbol: string; market: string; ticker: string; currency: string }[]) => setKnown(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  const isTrade = type === "BUY" || type === "SELL";
  const hasSymbol = isTrade || type === "DIVIDEND";
  const stepKeys = hasSymbol ? ["basic", "stock", "trade", "extra"] : ["basic", "trade", "extra"];
  const curKey = stepKeys[Math.min(step, stepKeys.length - 1)];
  const isLast = step >= stepKeys.length - 1;
  const marketCurrencyOf = (mv: string) => MARKETS.find((m) => m.value === mv)?.currency ?? "KRW";
  const overseasSelected = market !== "" && marketCurrencyOf(market) !== "KRW";

  // 종목명 입력 시 국내 종목 검색(디바운스). 해외 거래소를 고른 상태면 국내 검색 안 함.
  useEffect(() => {
    if (!hasSymbol || overseasSelected) {
      setSearchHits([]);
      return;
    }
    const q = symbol.trim();
    if (!q) {
      setSearchHits([]);
      return;
    }
    const t = setTimeout(() => {
      fetch(`/api/portfolio/stock-search?q=${encodeURIComponent(q)}`)
        .then((r) => (r.ok ? r.json() : []))
        .then((d: { ticker: string; name: string; market: string }[]) => setSearchHits(Array.isArray(d) ? d : []))
        .catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [symbol, hasSymbol, overseasSelected]);

  // 입력어에 맞는 후보(내 보유 + 국내 검색), 이름 기준 dedup, 최대 5개.
  type Candidate = { name: string; ticker: string; market: string; currency: string; own: boolean };
  const candidates = useMemo<Candidate[]>(() => {
    if (overseasSelected) return []; // 해외는 티커=심볼, 후보 리스트 없음
    const q = symbol.trim().toLowerCase();
    if (!q) return [];
    const seen = new Set<string>();
    const out: Candidate[] = [];
    for (const k of known) {
      if (k.symbol.toLowerCase().includes(q) && !seen.has(k.symbol)) {
        seen.add(k.symbol);
        out.push({ name: k.symbol, ticker: k.ticker, market: k.market, currency: k.currency, own: true });
      }
    }
    for (const h of searchHits) {
      if (!seen.has(h.name)) {
        seen.add(h.name);
        out.push({ name: h.name, ticker: h.ticker, market: h.market, currency: "KRW", own: false });
      }
    }
    return out.slice(0, 5);
  }, [known, searchHits, symbol, overseasSelected]);

  function selectSuggestion(c: Candidate) {
    setSymbol(c.name);
    setCurrency(c.currency);
    setMarket(c.market);
    setTicker(c.ticker);
    setPickedName(c.name);
  }

  // 후보가 하나뿐이면(2글자 이상) 자동 매핑.
  useEffect(() => {
    if (symbol.trim().length >= 2 && candidates.length === 1 && candidates[0].name !== symbol) {
      selectSuggestion(candidates[0]);
    }
  }, [candidates]); // eslint-disable-line react-hooks/exhaustive-deps

  function onSymbolChange(v: string) {
    setPickedName(null);
    // 해외 거래소를 고른 상태면 티커=심볼(대문자)로 확정(국내 매칭이 티커를 오염시키지 않게).
    if (overseasSelected) {
      const up = v.toUpperCase();
      setSymbol(up);
      setTicker(up.trim());
      setCurrency(marketCurrencyOf(market));
      return;
    }
    setSymbol(v);
    const h = known.find((k) => k.symbol === v);
    if (h) {
      setCurrency(h.currency);
      setMarket(h.market);
      setTicker(h.ticker);
      setPickedName(v);
      return;
    }
    const s = searchHits.find((x) => x.name === v);
    if (s) {
      setMarket(s.market);
      setTicker(s.ticker);
      setCurrency("KRW");
      setPickedName(v);
    }
  }

  // 매매 금액은 거래 통화 기준 수량×단가 자동 계산(직접 수정 가능).
  const computedAmount = useMemo(() => {
    if (!isTrade) return null;
    const q = parseFloat(quantity);
    const p = parseFloat(price);
    if (!isFinite(q) || !isFinite(p)) return null;
    return Number((q * p).toFixed(8)).toString();
  }, [isTrade, quantity, price]);

  useEffect(() => {
    if (computedAmount != null) setAmount(computedAmount);
  }, [computedAmount]);

  function onMarketChange(v: string) {
    setMarket(v);
    const m = MARKETS.find((x) => x.value === v);
    if (m) {
      setCurrency(m.currency);
      // 해외로 바꾸면 티커=심볼(대문자)로 확정(잔값 오염 방지).
      if (m.currency !== "KRW") {
        const up = symbol.trim().toUpperCase();
        setSymbol(up);
        setTicker(up);
      }
    }
  }

  function next() {
    if (curKey === "basic" && accountId === "") {
      setError("계좌를 선택하세요.");
      return;
    }
    if (curKey === "stock") {
      if (!symbol.trim()) {
        setError("종목을 입력하세요.");
        return;
      }
      if (isTrade && !market) {
        setError("거래소를 선택하세요.");
        return;
      }
    }
    setError(null);
    setStep((s) => s + 1);
  }

  function back() {
    setError(null);
    setStep((s) => Math.max(0, s - 1));
  }

  async function submit() {
    setError(null);
    if (!isEdit && accountId === "") {
      setError("계좌를 선택하세요.");
      return;
    }
    if (!amount || parseFloat(amount) < 0) {
      setError("금액을 입력하세요.");
      return;
    }
    setSaving(true);
    try {
      const common = {
        type,
        tradedAt,
        symbol: hasSymbol && symbol.trim() ? symbol.trim() : null,
        currency,
        quantity: isTrade && quantity ? parseFloat(quantity) : null,
        price: isTrade && price ? parseFloat(price) : null,
        amount: parseFloat(amount),
        fee: fee ? parseInt(fee, 10) : 0,
        tag: tag.trim() || null,
        memo: memo.trim() || null,
      };
      const res = isEdit
        ? await fetch(`/api/portfolio/transactions/${initial!.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(common),
          })
        : await fetch(createUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ accountId, ...common }),
          });
      if (!res.ok) throw new Error("TX");

      if (holdingLink && !isEdit && common.symbol && market && ticker.trim()) {
        await fetch("/api/portfolio/holdings", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ accountId, symbol: common.symbol, market, ticker: ticker.trim() }),
        }).catch(() => {});
      }
      onSaved();
    } catch {
      setError("저장에 실패했습니다. 입력값을 확인하세요.");
      setSaving(false);
    }
  }

  const suggestionList =
    symbol.trim() && symbol !== pickedName && candidates.length > 0 ? (
      <ul className="mt-1 rounded-lg border border-white/10 bg-slate-900 divide-y divide-white/5 max-h-56 overflow-y-auto">
        {candidates.map((c) => (
          <li key={c.name}>
            <button
              type="button"
              onClick={() => selectSuggestion(c)}
              className="w-full text-left px-3 py-2 hover:bg-white/5 transition flex items-center justify-between gap-2"
            >
              <span className="text-white text-sm">{c.name}</span>
              <span className="text-xs text-slate-500">
                {c.own ? "내 종목" : MARKETS.find((m) => m.value === c.market)?.label ?? c.market} · {c.ticker} · {c.currency}
              </span>
            </button>
          </li>
        ))}
      </ul>
    ) : null;

  return (
    <Modal
      title={isEdit ? "거래 수정" : "거래 추가"}
      onClose={onClose}
      footer={
        isEdit ? (
          <>
            <button onClick={onClose} className={cx.btnSecondary} disabled={saving}>
              취소
            </button>
            <button onClick={submit} className={cx.btnPrimary} disabled={saving}>
              {saving ? "저장 중…" : "저장"}
            </button>
          </>
        ) : (
          <>
            <button onClick={step === 0 ? onClose : back} className={cx.btnSecondary} disabled={saving}>
              {step === 0 ? "취소" : "이전"}
            </button>
            <button onClick={isLast ? submit : next} className={cx.btnPrimary} disabled={saving}>
              {isLast ? (saving ? "저장 중…" : "저장") : "다음"}
            </button>
          </>
        )
      }
    >
      {isEdit ? (
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3">
            <Field label="계좌">
              <select value={accountId} disabled className={cx.select + " w-full opacity-50"}>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="유형">
              <select value={type} onChange={(e) => setType(e.target.value as TxType)} className={cx.select + " w-full"}>
                {TRADE_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {TX_TYPE_LABEL[t]}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <Field label="일자">
            <input type="date" value={tradedAt} onChange={(e) => setTradedAt(e.target.value)} className={cx.inputDate + " w-full"} />
          </Field>
          {hasSymbol && (
            <Field label="종목">
              <input value={symbol} onChange={(e) => onSymbolChange(e.target.value)} className={cx.input} />
              {suggestionList}
            </Field>
          )}
          {isTrade && (
            <div className="grid grid-cols-2 gap-3">
              <Field label="수량">
                <CommaInput decimal value={quantity} onChange={setQuantity} className={cx.inputNumber} />
              </Field>
              <Field label={`단가 (${currency})`}>
                <CommaInput decimal value={price} onChange={setPrice} className={cx.inputNumber} />
              </Field>
            </div>
          )}
          <Field label={`금액 (${currency})`}>
            <CommaInput decimal value={amount} onChange={setAmount} className={cx.inputNumber} />
          </Field>
          <button type="button" onClick={() => setShowAdvanced((v) => !v)} className="self-start text-xs text-slate-400 hover:text-white transition">
            {showAdvanced ? "▴ 상세 접기" : "▾ 상세 (통화 · 수수료 · 태그 · 메모)"}
          </button>
          {showAdvanced && (
            <div className="flex flex-col gap-3 rounded-xl border border-white/10 bg-white/3 p-3">
              <div className="grid grid-cols-2 gap-3">
                <Field label="통화">
                  <select value={currency} onChange={(e) => setCurrency(e.target.value)} className={cx.select + " w-full"}>
                    {CURRENCIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="수수료 (원)">
                  <CommaInput value={fee} onChange={setFee} placeholder="0" className={cx.inputNumber} />
                </Field>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Field label="태그">
                  <input value={tag} onChange={(e) => setTag(e.target.value)} className={cx.input} />
                </Field>
                <Field label="메모">
                  <input value={memo} onChange={(e) => setMemo(e.target.value)} className={cx.input} />
                </Field>
              </div>
            </div>
          )}
          {error && <p className="text-sm text-rose-300">{error}</p>}
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {/* 단계 표시 */}
          <div className="flex items-center gap-1.5">
            {stepKeys.map((k, i) => (
              <div key={k} className="flex items-center gap-1.5">
                <span
                  className={
                    "text-xs px-2 py-0.5 rounded-full " +
                    (i === step ? "bg-indigo-600/25 text-indigo-200" : i < step ? "text-emerald-300" : "text-slate-500")
                  }
                >
                  {STEP_LABEL[k]}
                </span>
                {i < stepKeys.length - 1 && <span className="text-slate-600 text-xs">›</span>}
              </div>
            ))}
          </div>

          {curKey === "basic" && (
            <div className="flex flex-col gap-4">
              <Field label="유형">
                <div className="flex flex-wrap gap-2">
                  {TRADE_TYPES.map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setType(t)}
                      className={
                        "px-3.5 py-2 rounded-lg text-sm border transition " +
                        (type === t ? "bg-indigo-600/25 text-indigo-200 border-indigo-500/40" : "bg-white/5 text-slate-400 border-white/10 hover:text-white")
                      }
                    >
                      {TX_TYPE_LABEL[t]}
                    </button>
                  ))}
                </div>
              </Field>
              <Field label="계좌">
                <select value={accountId} onChange={(e) => setAccountId(Number(e.target.value))} className={cx.select + " w-full"}>
                  {accounts.length === 0 && <option value="">계좌 없음</option>}
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
          )}

          {curKey === "stock" && (
            <div className="flex flex-col gap-4">
              <Field label="거래소" hint="국내는 이름 검색 · 해외는 티커=심볼">
                <select value={market} onChange={(e) => onMarketChange(e.target.value)} className={cx.select + " w-full"}>
                  <option value="">선택</option>
                  {MARKETS.map((m) => (
                    <option key={m.value} value={m.value}>
                      {m.label}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="종목" hint={currency !== "KRW" ? `${currency} · 티커=심볼` : "이름/티커로 검색"}>
                <input value={symbol} onChange={(e) => onSymbolChange(e.target.value)} placeholder="예: 삼성전자 / TSLA" className={cx.input} />
                {suggestionList}
              </Field>
              {market && ticker && (
                <p className="text-xs text-slate-500">
                  연동: {MARKETS.find((m) => m.value === market)?.label ?? market} · {ticker} · {currency} → 현재가·환율 자동
                </p>
              )}
              {currency !== "KRW" && ticker.trim() && (
                <div className="flex items-center gap-2 flex-wrap">
                  <button type="button" onClick={verifyOverseas} className={cx.btnSecondary} disabled={verify?.loading}>
                    {verify?.loading ? "확인 중…" : "시세 확인"}
                  </button>
                  {verify && !verify.loading && (
                    verify.valid ? (
                      <span className="text-xs text-emerald-300">✓ 현재가 {natMoney(verify.price ?? 0, currency)} — 유효한 종목</span>
                    ) : (
                      <span className="text-xs text-rose-300">✗ 조회 실패 — 티커·거래소를 확인하세요</span>
                    )
                  )}
                </div>
              )}
            </div>
          )}

          {curKey === "trade" && (
            <div className="flex flex-col gap-4">
              <Field label="일자">
                <input type="date" value={tradedAt} onChange={(e) => setTradedAt(e.target.value)} className={cx.inputDate + " w-full"} />
              </Field>
              {isTrade ? (
                <div className="grid grid-cols-2 gap-3">
                  <Field label="수량">
                    <CommaInput decimal value={quantity} onChange={setQuantity} className={cx.inputNumber} />
                  </Field>
                  <Field label={`단가 (${currency})`}>
                    <CommaInput decimal value={price} onChange={setPrice} className={cx.inputNumber} />
                  </Field>
                </div>
              ) : (
                !hasSymbol && (
                  <Field label="통화">
                    <select value={currency} onChange={(e) => setCurrency(e.target.value)} className={cx.select + " w-full"}>
                      {CURRENCIES.map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </Field>
                )
              )}
              <Field label={`금액 (${currency})`} hint={isTrade ? "수량·단가로 자동 계산 · 직접 수정 가능" : "거래 통화 기준"}>
                <CommaInput decimal value={amount} onChange={setAmount} className={cx.inputNumber} />
              </Field>
            </div>
          )}

          {curKey === "extra" && (
            <div className="flex flex-col gap-4">
              <Field label="수수료 (원)">
                <CommaInput value={fee} onChange={setFee} placeholder="0" className={cx.inputNumber} />
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="태그">
                  <input value={tag} onChange={(e) => setTag(e.target.value)} placeholder="적립 / 추세 …" className={cx.input} />
                </Field>
                <Field label="메모">
                  <input value={memo} onChange={(e) => setMemo(e.target.value)} className={cx.input} />
                </Field>
              </div>
            </div>
          )}

          {error && <p className="text-sm text-rose-300">{error}</p>}
        </div>
      )}
    </Modal>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs text-slate-400">
        {label}
        {hint && <span className="text-slate-600"> · {hint}</span>}
      </span>
      {children}
    </label>
  );
}
