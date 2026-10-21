"use client";

import { useEffect, useMemo, useState } from "react";
import Modal from "@/components/Modal";
import CommaInput from "@/components/CommaInput";
import Select from "@/components/Select";
import { cx } from "@/utils/cx";
import { type PortfolioHolding, type PortfolioPosition, CURRENCIES, natMoney, natSigned } from "@/types/portfolio";
import { previewTrade } from "./tradePreview";
import { toSymbolChips } from "./symbolChips";
import SymbolPicker, { type SymbolSelection } from "./SymbolPicker";
import { readLastAccountId, writeLastAccountId } from "./lastAccount";

/** 한 번에 기록할 수 있는 일. */
type Task = "deposit" | "fx" | "buy" | "sell";

/** 안내 흐름의 한 화면. */
type StepKey = "account" | "tasks" | "date" | "deposit" | "fx" | "symbol" | "trade" | "confirm";

const TASK_LABEL: Record<Task, string> = {
  deposit: "돈을 넣었어요",
  fx: "달러로 바꿨어요",
  buy: "주식을 샀어요",
  sell: "주식을 팔았어요",
};

const TASK_ORDER: Task[] = ["deposit", "fx", "buy", "sell"];
const MAX_CHIPS = 8;

interface AccountOption {
  id: number;
  name: string;
}

const today = () => new Date().toISOString().slice(0, 10);

/** 오늘에서 하루 뺀 날짜(ISO). */
function yesterday(): string {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().slice(0, 10);
}

/**
 * 안내 입력 모달 — 한 화면에 하나씩 물어 입금·환전·매매를 한 번에 기록한다.
 *
 * @param accounts      선택 가능한 계좌 목록
 * @param recentSymbols 최근 거래 순 종목명
 * @param createUrl     거래 생성 엔드포인트(공유 계좌면 다름)
 * @param holdingLink   저장 후 종목 식별 정보를 함께 등록할지 여부
 * @param onClose       닫기
 * @param onSaved       저장 성공 후 콜백
 */
export default function GuidedEntryModal({
  accounts,
  recentSymbols = [],
  createUrl = "/api/portfolio/transactions",
  holdingLink = true,
  onClose,
  onSaved,
}: {
  accounts: AccountOption[];
  recentSymbols?: string[];
  createUrl?: string;
  holdingLink?: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const firstAccount = (): number | "" => {
    const last = readLastAccountId();
    if (last != null && accounts.some((a) => a.id === last)) return last;
    return accounts[0]?.id ?? "";
  };

  const [accountId, setAccountId] = useState<number | "">(firstAccount());
  const [tasks, setTasks] = useState<Task[]>([]);
  const [date, setDate] = useState(today());

  const [depositAmount, setDepositAmount] = useState("");
  const [depositCurrency, setDepositCurrency] = useState("KRW");

  const [fxFrom, setFxFrom] = useState("");
  const [fxTo, setFxTo] = useState("");
  const [fxFromCurrency, setFxFromCurrency] = useState("KRW");
  const [fxToCurrency, setFxToCurrency] = useState("USD");

  const [picked, setPicked] = useState<SymbolSelection>({ symbol: "", market: "", ticker: "", currency: "KRW" });
  const [quantity, setQuantity] = useState("");
  const [price, setPrice] = useState("");

  const [holdings, setHoldings] = useState<PortfolioHolding[]>([]);
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [stepIndex, setStepIndex] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
  const isSell = tasks.includes("sell");
  const hasTrade = tasks.includes("buy") || isSell;
  const accountName = accounts.find((a) => a.id === accountId)?.name ?? null;

  const steps = useMemo<StepKey[]>(() => {
    const list: StepKey[] = [];
    if (accounts.length > 1) list.push("account");
    list.push("tasks");
    if (tasks.length > 0) list.push("date");
    if (tasks.includes("deposit")) list.push("deposit");
    if (tasks.includes("fx")) list.push("fx");
    if (hasTrade) list.push("symbol", "trade");
    if (tasks.length > 0) list.push("confirm");
    return list;
  }, [accounts.length, tasks, hasTrade]);

  // 뒤로 가서 할 일을 빼면 단계가 줄어든다. 머물던 자리가 사라지면 마지막 단계로 당긴다.
  useEffect(() => {
    setStepIndex((i) => Math.min(i, steps.length - 1));
  }, [steps.length]);

  const step = steps[Math.min(stepIndex, steps.length - 1)];
  const isLast = step === "confirm";

  // 지금 들고 있는 수량 — 전량·반절 버튼의 근거.
  const heldQuantity = useMemo(() => {
    if (!isSell || !picked.symbol.trim()) return null;
    const matched = positions.filter(
      (p) => p.symbol === picked.symbol.trim() && (accountName == null || p.account === accountName)
    );
    return matched.length === 0 ? null : matched.reduce((s, p) => s + p.quantity, 0);
  }, [isSell, picked.symbol, positions, accountName]);

  const tradeAmount = useMemo(() => {
    const q = parseFloat(quantity);
    const p = parseFloat(price);
    return isFinite(q) && isFinite(p) ? Number((q * p).toFixed(8)) : null;
  }, [quantity, price]);

  const fxRate =
    Number(fxFrom) > 0 && Number(fxTo) > 0 ? Number(fxFrom) / Number(fxTo) : null;

  // 저장하면 보유가 어떻게 바뀌는지 — 확인 화면에서 평단 변화를 보여준다.
  const preview = useMemo(
    () =>
      hasTrade
        ? previewTrade(
            positions,
            picked.symbol,
            accountName,
            picked.currency,
            isSell,
            parseFloat(quantity),
            parseFloat(price)
          )
        : null,
    [hasTrade, positions, picked.symbol, picked.currency, accountName, isSell, quantity, price]
  );

  function toggleTask(task: Task) {
    setError(null);
    setTasks((prev) => (prev.includes(task) ? prev.filter((t) => t !== task) : [...prev, task]));
  }

  /** 지금 화면의 입력이 다음으로 넘어갈 만한지. 문제가 있으면 메시지를 돌려준다. */
  function validate(): string | null {
    switch (step) {
      case "account":
        return accountId === "" ? "계좌를 골라주세요." : null;
      case "tasks":
        return tasks.length === 0 ? "하신 일을 하나 이상 골라주세요." : null;
      case "deposit":
        return parseFloat(depositAmount) > 0 ? null : "넣으신 금액을 적어주세요.";
      case "fx":
        if (!(parseFloat(fxFrom) > 0) || !(parseFloat(fxTo) > 0)) return "바꾸기 전후 금액을 모두 적어주세요.";
        if (fxFromCurrency === fxToCurrency) return "서로 다른 통화를 골라주세요.";
        return null;
      case "symbol":
        return picked.symbol.trim() ? null : "종목을 골라주세요.";
      case "trade":
        if (!(parseFloat(quantity) > 0)) return "몇 주인지 적어주세요.";
        if (!(parseFloat(price) > 0)) return "한 주에 얼마였는지 적어주세요.";
        return null;
      default:
        return null;
    }
  }

  function next() {
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    setError(null);
    setStepIndex((i) => i + 1);
  }

  function back() {
    setError(null);
    setStepIndex((i) => Math.max(0, i - 1));
  }

  async function save() {
    if (accountId === "") {
      setError("계좌를 골라주세요.");
      return;
    }
    setSaving(true);
    setError(null);
    const done: string[] = [];

    const fail = (what: string) => {
      setError(
        done.length === 0
          ? `${what} 기록에 실패했습니다. 다시 시도해주세요.`
          : `${done.join(", ")}까지 저장했고 ${what}에서 실패했습니다. 기록 목록을 확인해주세요.`
      );
      setSaving(false);
    };

    if (tasks.includes("deposit")) {
      const ok = await postTransaction({
        type: "DEPOSIT",
        amount: parseFloat(depositAmount),
        currency: depositCurrency,
      });
      if (!ok) return fail("입금");
      done.push("입금");
    }

    if (tasks.includes("fx")) {
      const res = await fetch("/api/portfolio/fx-conversions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          accountId,
          convDate: date,
          fromCurrency: fxFromCurrency,
          fromAmount: parseFloat(fxFrom),
          toCurrency: fxToCurrency,
          toAmount: parseFloat(fxTo),
          fee: 0,
          memo: null,
        }),
      }).catch(() => null);
      if (!res?.ok) return fail("환전");
      done.push("환전");
    }

    if (hasTrade) {
      const ok = await postTransaction({
        type: isSell ? "SELL" : "BUY",
        amount: tradeAmount ?? 0,
        currency: picked.currency,
        symbol: picked.symbol.trim(),
        quantity: parseFloat(quantity),
        price: parseFloat(price),
      });
      if (!ok) return fail(isSell ? "매도" : "매수");
      if (holdingLink && picked.market && picked.ticker.trim()) {
        await fetch("/api/portfolio/holdings", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            accountId,
            symbol: picked.symbol.trim(),
            market: picked.market,
            ticker: picked.ticker.trim(),
          }),
        }).catch(() => {});
      }
    }

    writeLastAccountId(accountId as number);
    onSaved();
  }

  async function postTransaction(body: {
    type: string;
    amount: number;
    currency: string;
    symbol?: string;
    quantity?: number;
    price?: number;
  }): Promise<boolean> {
    const res = await fetch(createUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        accountId,
        tradedAt: date,
        symbol: body.symbol ?? null,
        quantity: body.quantity ?? null,
        price: body.price ?? null,
        fee: 0,
        tag: null,
        memo: null,
        ...body,
      }),
    }).catch(() => null);
    return !!res?.ok;
  }

  const position = steps.length > 1 ? `${Math.min(stepIndex, steps.length - 1) + 1} / ${steps.length}` : "";

  return (
    <Modal
      title="차근차근 기록하기"
      onClose={onClose}
      closeOnEscape={false}
      footer={
        <>
          <button onClick={stepIndex === 0 ? onClose : back} className={cx.btnSecondary} disabled={saving}>
            {stepIndex === 0 ? "그만두기" : "이전"}
          </button>
          <button onClick={isLast ? save : next} className={cx.btnPrimary} disabled={saving}>
            {isLast ? (saving ? "저장 중…" : "네, 맞아요. 저장할게요") : "다음"}
          </button>
        </>
      }
    >
      <div className="flex flex-col gap-5">
        {position && <div className="text-xs text-slate-500">{position}</div>}

        {step === "account" && (
          <Question title="어느 계좌인가요?">
            <div className="flex flex-col gap-2">
              {accounts.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  onClick={() => setAccountId(a.id)}
                  className={accountId === a.id ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  {a.name}
                </button>
              ))}
            </div>
          </Question>
        )}

        {step === "tasks" && (
          <Question title="오늘 무엇을 하셨나요?" hint="해당하는 것을 모두 골라주세요. 여러 개여도 괜찮아요.">
            <div className="flex flex-col gap-2">
              {TASK_ORDER.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => toggleTask(t)}
                  className={tasks.includes(t) ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  {TASK_LABEL[t]}
                </button>
              ))}
            </div>
          </Question>
        )}

        {step === "date" && (
          <Question title="언제 하셨나요?">
            <div className="flex flex-col gap-3">
              <div className="flex flex-wrap gap-2">
                <button type="button" onClick={() => setDate(today())} className={date === today() ? cx.btnToggleOn : cx.btnToggleOff}>
                  오늘
                </button>
                <button
                  type="button"
                  onClick={() => setDate(yesterday())}
                  className={date === yesterday() ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  어제
                </button>
              </div>
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className={cx.inputDate + " w-full"} />
            </div>
          </Question>
        )}

        {step === "deposit" && (
          <Question title="얼마를 넣으셨나요?" hint="계좌로 옮긴 금액을 그대로 적어주세요.">
            <div className="flex flex-col gap-3">
              <CommaInput decimal value={depositAmount} onChange={setDepositAmount} className={cx.inputNumber} />
              <Select
                value={depositCurrency}
                onChange={setDepositCurrency}
                items={CURRENCIES.map((c) => ({ value: c, label: c }))}
              />
            </div>
          </Question>
        )}

        {step === "fx" && (
          <Question title="얼마를 바꾸셨나요?" hint="바꾸기 전 금액과 받은 금액을 적으면 환율은 알아서 계산해요.">
            <div className="flex flex-col gap-3">
              <div className="grid grid-cols-2 gap-3">
                <Labeled label="바꾸기 전">
                  <CommaInput decimal value={fxFrom} onChange={setFxFrom} className={cx.inputNumber} />
                </Labeled>
                <Labeled label="통화">
                  <Select value={fxFromCurrency} onChange={setFxFromCurrency} items={CURRENCIES.map((c) => ({ value: c, label: c }))} />
                </Labeled>
                <Labeled label="받은 금액">
                  <CommaInput decimal value={fxTo} onChange={setFxTo} className={cx.inputNumber} />
                </Labeled>
                <Labeled label="통화">
                  <Select value={fxToCurrency} onChange={setFxToCurrency} items={CURRENCIES.map((c) => ({ value: c, label: c }))} />
                </Labeled>
              </div>
              {fxRate && (
                <p className="text-sm text-slate-400">
                  환율 약 {fxRate.toFixed(2)}원이네요.
                </p>
              )}
            </div>
          </Question>
        )}

        {step === "symbol" && (
          <Question
            title={isSell ? "어떤 주식을 파셨나요?" : "어떤 주식을 사셨나요?"}
            hint="전에 거래한 종목은 버튼 한 번으로 고를 수 있어요."
          >
            {/* 계좌는 앞 단계에서 직접 답했으므로 종목 칩이 덮어쓰지 않는다. */}
            <SymbolPicker chips={chips} value={picked} onChange={setPicked} />
          </Question>
        )}

        {step === "trade" && (
          <Question
            title={`${picked.symbol}, 몇 주를 얼마에 ${isSell ? "파셨나요" : "사셨나요"}?`}
            hint={`한 주 가격은 ${picked.currency} 기준으로 적어주세요.`}
          >
            <div className="flex flex-col gap-3">
              <Labeled label="몇 주">
                <CommaInput decimal value={quantity} onChange={setQuantity} className={cx.inputNumber} />
              </Labeled>
              {heldQuantity != null && heldQuantity > 0 && (
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs text-slate-500">지금 {heldQuantity}주 있어요</span>
                  <button type="button" onClick={() => setQuantity(String(heldQuantity))} className={cx.btnSm}>
                    전부
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
              <Labeled label={`한 주에 얼마 (${picked.currency})`}>
                <CommaInput decimal value={price} onChange={setPrice} className={cx.inputNumber} />
              </Labeled>
              {tradeAmount != null && (
                <p className="text-sm text-slate-400">
                  모두 {natMoney(tradeAmount, picked.currency)}네요.
                </p>
              )}
            </div>
          </Question>
        )}

        {step === "confirm" && (
          <Question title="이게 맞나요?" hint="맞으면 아래 버튼을 눌러주세요. 틀렸으면 이전으로 돌아가 고칠 수 있어요.">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Summary label="계좌">{accountName ?? "—"}</Summary>
                <Summary label="날짜">{date}</Summary>
                {tasks.includes("deposit") && (
                  <Summary label="입금">{natMoney(parseFloat(depositAmount) || 0, depositCurrency)}</Summary>
                )}
                {tasks.includes("fx") && (
                  <Summary label="환전">
                    {natMoney(parseFloat(fxFrom) || 0, fxFromCurrency)} →{" "}
                    {natMoney(parseFloat(fxTo) || 0, fxToCurrency)}
                  </Summary>
                )}
                {hasTrade && (
                  <Summary label={isSell ? "매도" : "매수"}>
                    {picked.symbol} {quantity}주 × {natMoney(parseFloat(price) || 0, picked.currency)} ={" "}
                    {natMoney(tradeAmount ?? 0, picked.currency)}
                  </Summary>
                )}
              </div>

              {preview && (
                <div className="flex flex-col gap-2 rounded-xl border border-indigo-500/25 bg-indigo-600/10 p-4">
                  <div className="text-sm font-medium text-indigo-200">기록하면 이렇게 바뀌어요</div>
                  <Change label="보유 수량">
                    {preview.beforeQty}주 → <strong className="font-medium text-white">{preview.afterQty}주</strong>
                  </Change>
                  {preview.afterAvg != null && (
                    <Change label="평단가">
                      {preview.beforeAvg == null ? "처음 매수" : natMoney(preview.beforeAvg, preview.currency)} →{" "}
                      <strong className="font-medium text-white">
                        {natMoney(preview.afterAvg, preview.currency)}
                      </strong>
                    </Change>
                  )}
                  {preview.realizedNat != null && (
                    <Change label="이번에 확정되는 손익">
                      <strong className={"font-medium " + (preview.realizedNat >= 0 ? "text-rose-300" : "text-sky-300")}>
                        {natSigned(preview.realizedNat, preview.currency)}
                      </strong>
                    </Change>
                  )}
                  {preview.overSelling && (
                    <p className="text-sm text-amber-300">
                      지금 {preview.beforeQty}주를 들고 있는데 {quantity}주를 파는 것으로 적혀 있어요. 수량을 다시
                      확인해주세요.
                    </p>
                  )}
                </div>
              )}
            </div>
          </Question>
        )}

        {error && <p className="text-sm text-rose-300">{error}</p>}
      </div>
    </Modal>
  );
}

/** 질문 한 화면 — 큰 제목과 보조 설명을 붙인다. */
function Question({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-medium text-white">{title}</h2>
        {hint && <p className="text-sm text-slate-400">{hint}</p>}
      </div>
      {children}
    </div>
  );
}

/** 입력 한 칸에 붙이는 작은 라벨. */
function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs text-slate-400">{label}</span>
      {children}
    </label>
  );
}

/** 저장 후 변화를 보여주는 한 줄. */
function Change({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-3 text-sm">
      <span className="text-slate-400">{label}</span>
      <span className="text-right tabular-nums text-slate-300">{children}</span>
    </div>
  );
}

/** 확인 화면의 한 줄. */
function Summary({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-lg bg-white/5 px-3 py-2.5">
      <span className="text-sm text-slate-400">{label}</span>
      <span className="text-right text-sm text-white">{children}</span>
    </div>
  );
}
