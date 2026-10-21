"use client";

import { useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import CommaInput from "@/components/CommaInput";
import Select from "@/components/Select";
import Panel from "@/components/Panel";
import { cx } from "@/utils/cx";
import {
  type PortfolioFxConversion,
  type PortfolioHolding,
  type PortfolioPosition,
  type PortfolioTx,
  type TxType,
  CURRENCIES,
  TX_TYPE_LABEL,
} from "@/types/portfolio";
import { toSymbolChips } from "./symbolChips";
import SymbolPicker from "./SymbolPicker";
import { readLastAccountId, writeLastAccountId } from "./lastAccount";

/** 빠른 입력이 받는 기록 종류 — 거래 유형에 환전을 더한 것. */
export type EntryKind = TxType | "FX";

const TX_KINDS: EntryKind[] = ["BUY", "SELL", "DEPOSIT", "WITHDRAW", "DIVIDEND", "INTEREST"];
const KIND_LABEL: Record<EntryKind, string> = { ...TX_TYPE_LABEL, FX: "환전" };
const MAX_CHIPS = 8;

interface AccountOption {
  id: number;
  name: string;
}

const numText = (v?: number | null) => (v == null ? "" : String(v));
const today = () => new Date().toISOString().slice(0, 10);

/**
 * 빠른 입력 모달 — 매매·입출금·배당·환전을 한 장에서 받는다. 종목 칩을 누르면 거래소·티커·통화·계좌가 함께 정해진다.
 *
 * @param accounts      선택 가능한 계좌 목록
 * @param recentSymbols 최근 거래 순 종목명(칩 정렬에 쓴다)
 * @param initialTx     수정 대상 거래(없으면 신규)
 * @param initialFx     수정 대상 환전(없으면 신규)
 * @param defaultKind   신규일 때 처음 고를 종류
 * @param createUrl     거래 생성 엔드포인트(공유 계좌면 다름)
 * @param holdingLink   저장 후 종목 식별 정보를 함께 등록할지 여부
 * @param onClose       닫기
 * @param onSaved       저장 성공 후 콜백
 */
export default function QuickEntryModal({
  accounts,
  recentSymbols = [],
  initialTx,
  initialFx,
  defaultKind,
  createUrl = "/api/portfolio/transactions",
  holdingLink = true,
  onClose,
  onSaved,
}: {
  accounts: AccountOption[];
  recentSymbols?: string[];
  initialTx?: PortfolioTx;
  initialFx?: PortfolioFxConversion;
  defaultKind?: EntryKind;
  createUrl?: string;
  holdingLink?: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!initialTx || !!initialFx;
  const lockedToFx = !!initialFx;

  const defaultAccountId = (): number | "" => {
    if (initialTx?.accountId != null) return initialTx.accountId;
    if (initialFx?.accountId != null) return initialFx.accountId;
    const last = readLastAccountId();
    if (last != null && accounts.some((a) => a.id === last)) return last;
    return accounts[0]?.id ?? "";
  };

  const [kind, setKind] = useState<EntryKind>(
    lockedToFx ? "FX" : initialTx?.type ?? defaultKind ?? "BUY"
  );
  const [accountId, setAccountId] = useState<number | "">(defaultAccountId());
  const [tradedAt, setTradedAt] = useState(initialTx?.tradedAt ?? initialFx?.convDate ?? today());

  const [symbol, setSymbol] = useState(initialTx?.symbol ?? "");
  const [market, setMarket] = useState("");
  const [ticker, setTicker] = useState("");
  const [currency, setCurrency] = useState(initialTx?.currency ?? "KRW");

  const [quantity, setQuantity] = useState(numText(initialTx?.quantity));
  const [price, setPrice] = useState(numText(initialTx?.price));
  const [amount, setAmount] = useState(numText(initialTx?.amount));

  const [fromCurrency, setFromCurrency] = useState(initialFx?.fromCurrency ?? "KRW");
  const [fromAmount, setFromAmount] = useState(numText(initialFx?.fromAmount));
  const [toCurrency, setToCurrency] = useState(initialFx?.toCurrency ?? "USD");
  const [toAmount, setToAmount] = useState(numText(initialFx?.toAmount));

  const [fee, setFee] = useState(numText(initialTx?.fee ?? initialFx?.fee));
  const [tag, setTag] = useState(initialTx?.tag ?? "");
  const [memo, setMemo] = useState(initialTx?.memo ?? initialFx?.memo ?? "");

  const [holdings, setHoldings] = useState<PortfolioHolding[]>([]);
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [showDetail, setShowDetail] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isTrade = kind === "BUY" || kind === "SELL";
  const isFx = kind === "FX";
  const hasSymbol = isTrade || kind === "DIVIDEND";

  useEffect(() => {
    fetch("/api/portfolio/holdings")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioHolding[]) => setHoldings(Array.isArray(d) ? d : []))
      .catch(() => {});
    fetch("/api/portfolio/positions")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: PortfolioPosition[]) => setPositions(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  const chips = useMemo(() => toSymbolChips(holdings, recentSymbols, MAX_CHIPS), [holdings, recentSymbols]);

  // 매매 금액은 수량×단가로 자동 계산한다(직접 수정 가능).
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

  const accountName = accounts.find((a) => a.id === accountId)?.name ?? null;

  // 매도 수량 보조 — 지금 들고 있는 수량(계좌가 정해졌으면 그 계좌 기준).
  const heldQuantity = useMemo(() => {
    if (kind !== "SELL" || !symbol.trim()) return null;
    const matched = positions.filter(
      (p) => p.symbol === symbol.trim() && (accountName == null || p.account === accountName)
    );
    if (matched.length === 0) return null;
    return matched.reduce((s, p) => s + p.quantity, 0);
  }, [kind, symbol, positions, accountName]);


  const fxRate =
    Number(fromAmount) > 0 && Number(toAmount) > 0 ? Number(fromAmount) / Number(toAmount) : null;

  async function submitFx() {
    if (accountId === "") {
      setError("계좌를 선택하세요.");
      return false;
    }
    if (!fromAmount || !toAmount) {
      setError("보낸 금액과 받은 금액을 입력하세요.");
      return false;
    }
    if (fromCurrency === toCurrency) {
      setError("보낸 통화와 받은 통화가 같습니다.");
      return false;
    }
    const body = JSON.stringify({
      accountId,
      convDate: tradedAt,
      fromCurrency,
      fromAmount: parseFloat(fromAmount),
      toCurrency,
      toAmount: parseFloat(toAmount),
      fee: fee ? parseFloat(fee) : 0,
      memo: memo.trim() || null,
    });
    const res = initialFx
      ? await fetch(`/api/portfolio/fx-conversions/${initialFx.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body,
        })
      : await fetch("/api/portfolio/fx-conversions", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body,
        });
    if (!res.ok) {
      setError("저장에 실패했습니다.");
      return false;
    }
    return true;
  }

  async function submitTx() {
    if (!isEdit && accountId === "") {
      setError("계좌를 선택하세요.");
      return false;
    }
    if (hasSymbol && !symbol.trim()) {
      setError("종목을 입력하세요.");
      return false;
    }
    // 거래소는 신규 매매에서만 필요하다(수정은 이미 종목 식별 정보가 붙어 있다).
    if (isTrade && !initialTx && !market) {
      setError("거래소를 고르세요. 종목 칩을 누르면 함께 정해집니다.");
      return false;
    }
    if (!amount || parseFloat(amount) < 0) {
      setError("금액을 입력하세요.");
      return false;
    }
    const common = {
      type: kind as TxType,
      tradedAt,
      symbol: hasSymbol && symbol.trim() ? symbol.trim() : null,
      currency,
      quantity: isTrade && quantity ? parseFloat(quantity) : null,
      price: isTrade && price ? parseFloat(price) : null,
      amount: parseFloat(amount),
      fee: fee ? parseFloat(fee) : 0,
      tag: tag.trim() || null,
      memo: memo.trim() || null,
    };
    const res = initialTx
      ? await fetch(`/api/portfolio/transactions/${initialTx.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(common),
        })
      : await fetch(createUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ accountId, ...common }),
        });
    if (!res.ok) {
      setError("저장에 실패했습니다. 입력값을 확인하세요.");
      return false;
    }
    if (holdingLink && !initialTx && common.symbol && market && ticker.trim()) {
      await fetch("/api/portfolio/holdings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ accountId, symbol: common.symbol, market, ticker: ticker.trim() }),
      }).catch(() => {});
    }
    return true;
  }

  async function submit() {
    setError(null);
    setSaving(true);
    const ok = isFx ? await submitFx() : await submitTx();
    if (!ok) {
      setSaving(false);
      return;
    }
    if (accountId !== "") writeLastAccountId(accountId);
    onSaved();
  }

  const kindChoices: EntryKind[] = lockedToFx ? ["FX"] : initialTx ? TX_KINDS : [...TX_KINDS, "FX"];

  return (
    <Modal
      title={isEdit ? "기록 수정" : "기록 추가"}
      onClose={onClose}
      footer={
        <>
          <button onClick={onClose} className={cx.btnSecondary} disabled={saving}>
            취소
          </button>
          <button onClick={submit} className={cx.btnPrimary} disabled={saving}>
            {saving ? "저장 중…" : "저장"}
          </button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-2">
          {kindChoices.map((k) => (
            <button
              key={k}
              type="button"
              onClick={() => {
                setKind(k);
                setError(null);
              }}
              className={kind === k ? cx.btnToggleOn : cx.btnToggleOff}
            >
              {KIND_LABEL[k]}
            </button>
          ))}
        </div>

        {hasSymbol && (
          <Field label="종목">
            <SymbolPicker
              chips={chips}
              value={{ symbol, market, ticker, currency }}
              onChange={(next) => {
                setSymbol(next.symbol);
                setMarket(next.market);
                setTicker(next.ticker);
                setCurrency(next.currency);
                if (next.accountId != null && accounts.some((a) => a.id === next.accountId)) {
                  setAccountId(next.accountId);
                }
              }}
            />
          </Field>
        )}

        <div className="grid grid-cols-2 gap-3">
          <Field label="계좌">
            <Select
              value={String(accountId)}
              onChange={(v) => setAccountId(Number(v))}
              placeholder="계좌 없음"
              disabled={isEdit}
              items={accounts.map((a) => ({ value: String(a.id), label: a.name }))}
            />
          </Field>
          <Field label="일자">
            <input
              type="date"
              value={tradedAt}
              onChange={(e) => setTradedAt(e.target.value)}
              className={cx.inputDate + " w-full"}
            />
          </Field>
        </div>

        {isFx ? (
          <div className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-3">
              <Field label="보낸 통화">
                <Select value={fromCurrency} onChange={setFromCurrency} items={CURRENCIES.map((c) => ({ value: c, label: c }))} />
              </Field>
              <Field label="보낸 금액">
                <CommaInput decimal value={fromAmount} onChange={setFromAmount} className={cx.inputNumber} />
              </Field>
              <Field label="받은 통화">
                <Select value={toCurrency} onChange={setToCurrency} items={CURRENCIES.map((c) => ({ value: c, label: c }))} />
              </Field>
              <Field label="받은 금액">
                <CommaInput decimal value={toAmount} onChange={setToAmount} className={cx.inputNumber} />
              </Field>
            </div>
            {fxRate && (
              <p className="text-xs text-slate-500">
                적용 환율 ≈ {fxRate.toFixed(2)} {fromCurrency}/{toCurrency}
              </p>
            )}
          </div>
        ) : isTrade ? (
          <div className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-3">
              <Field label="수량">
                <CommaInput decimal value={quantity} onChange={setQuantity} className={cx.inputNumber} />
              </Field>
              <Field label={`단가 (${currency})`}>
                <CommaInput decimal value={price} onChange={setPrice} className={cx.inputNumber} />
              </Field>
            </div>
            {heldQuantity != null && heldQuantity > 0 && (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs text-slate-500">보유 {heldQuantity}</span>
                <button type="button" onClick={() => setQuantity(String(heldQuantity))} className={cx.btnSm}>
                  전량
                </button>
                <button
                  type="button"
                  onClick={() => setQuantity(String(Number((heldQuantity / 2).toFixed(8))))}
                  className={cx.btnSm}
                >
                  반절
                </button>
              </div>
            )}
            <Field label={`금액 (${currency})`} hint="수량×단가 자동 · 직접 수정 가능">
              <CommaInput decimal value={amount} onChange={setAmount} className={cx.inputNumber} />
            </Field>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            <Field label="통화">
              <Select value={currency} onChange={setCurrency} items={CURRENCIES.map((c) => ({ value: c, label: c }))} />
            </Field>
            <Field label={`금액 (${currency})`}>
              <CommaInput decimal value={amount} onChange={setAmount} className={cx.inputNumber} />
            </Field>
          </div>
        )}

        <button
          type="button"
          onClick={() => setShowDetail((v) => !v)}
          className="self-start text-xs text-slate-400 transition hover:text-white"
        >
          {showDetail ? "▴ 상세 접기" : "▾ 상세 (수수료 · 태그 · 메모)"}
        </button>
        {showDetail && (
          <Panel className="flex flex-col gap-3 rounded-xl bg-white/3 p-3">
            <Field label={`수수료 (${isFx ? fromCurrency : currency})`}>
              <CommaInput decimal value={fee} onChange={setFee} placeholder="0" className={cx.inputNumber} />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              {!isFx && (
                <Field label="태그">
                  <input value={tag} onChange={(e) => setTag(e.target.value)} placeholder="적립 / 추세 …" className={cx.input} />
                </Field>
              )}
              <Field label="메모">
                <input value={memo} onChange={(e) => setMemo(e.target.value)} className={cx.input} />
              </Field>
            </div>
          </Panel>
        )}

        {error && <p className="text-sm text-rose-300">{error}</p>}
      </div>
    </Modal>
  );
}

/** 라벨·보조설명을 붙인 입력 한 칸. */
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
