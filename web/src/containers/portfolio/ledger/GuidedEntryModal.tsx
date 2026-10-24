"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Modal from "@/components/Modal";
import CommaInput from "@/components/CommaInput";
import Select from "@/components/Select";
import { cx } from "@/utils/cx";
import { type PortfolioHolding, type PortfolioPosition, CURRENCIES, natMoney, natSigned } from "@/types/portfolio";
import { toSymbolChips } from "./symbolChips";
import SymbolPicker from "./SymbolPicker";
import { readLastAccountId, writeLastAccountId } from "./lastAccount";
import { type TradeDraft, draftAmount, emptyDraft, syncDrafts, toTradeInput } from "./tradeDrafts";
import { previewTrades } from "./tradePreview";

/** 한 번에 기록할 수 있는 일. */
type Task = "deposit" | "fx" | "buy" | "sell";

const TASK_LABEL: Record<Task, string> = {
  deposit: "돈을 넣었어요",
  fx: "달러로 바꿨어요",
  buy: "주식을 샀어요",
  sell: "주식을 팔았어요",
};

const TASK_ORDER: Task[] = ["deposit", "fx", "buy", "sell"];
const MAX_CHIPS = 8;

/** 안내 흐름의 한 화면. 매매는 칸마다 종목·수량 화면이 한 쌍씩 생긴다. */
type Step =
  | { key: "account" | "tasks" | "date" | "deposit" | "fx" | "confirm" }
  | { key: "symbol" | "trade"; draft: number };

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
 * 안내 입력 모달 — 한 화면에 하나씩 물어 입금·환전·매매를 한 번에 기록한다. 매매는 여러 종목을 이어 적을 수 있다.
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
  const [fxFee, setFxFee] = useState("");

  const [drafts, setDrafts] = useState<TradeDraft[]>([]);
  const nextKey = useRef(1);

  const [holdings, setHoldings] = useState<PortfolioHolding[]>([]);
  const [positions, setPositions] = useState<PortfolioPosition[]>([]);
  const [stepIndex, setStepIndex] = useState(0);
  const [feeOpen, setFeeOpen] = useState(false);
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
  const accountName = accounts.find((a) => a.id === accountId)?.name ?? null;

  // 고른 할 일에 맞춰 매매 칸을 맞춘다.
  useEffect(() => {
    setDrafts((prev) => {
      const next = syncDrafts(prev, tasks.includes("buy"), tasks.includes("sell"), nextKey.current);
      nextKey.current += next.length;
      return next;
    });
  }, [tasks]);

  const steps = useMemo<Step[]>(() => {
    const list: Step[] = [];
    if (accounts.length > 1) list.push({ key: "account" });
    list.push({ key: "tasks" });
    if (tasks.length > 0) list.push({ key: "date" });
    if (tasks.includes("deposit")) list.push({ key: "deposit" });
    if (tasks.includes("fx")) list.push({ key: "fx" });
    drafts.forEach((_, i) => list.push({ key: "symbol", draft: i }, { key: "trade", draft: i }));
    if (tasks.length > 0) list.push({ key: "confirm" });
    return list;
  }, [accounts.length, tasks, drafts]);

  // 뒤로 가서 할 일을 빼면 단계가 줄어든다. 머물던 자리가 사라지면 마지막 단계로 당긴다.
  useEffect(() => {
    setStepIndex((i) => Math.min(i, steps.length - 1));
  }, [steps.length]);

  const step = steps[Math.min(stepIndex, steps.length - 1)];
  const isLast = step?.key === "confirm";
  const draftIndex = step && "draft" in step ? step.draft : -1;
  const draft = draftIndex >= 0 ? drafts[draftIndex] : null;

  useEffect(() => setFeeOpen(false), [stepIndex]);

  const previews = useMemo(
    () => previewTrades(positions, accountName, drafts.map(toTradeInput)),
    [positions, accountName, drafts]
  );

  // 지금 들고 있는 수량 — 전부·반절 버튼의 근거.
  const heldQuantity = useMemo(() => {
    if (!draft?.isSell || !draft.selection.symbol.trim()) return null;
    const matched = positions.filter(
      (p) => p.symbol === draft.selection.symbol.trim() && (accountName == null || p.account === accountName)
    );
    return matched.length === 0 ? null : matched.reduce((s, p) => s + p.quantity, 0);
  }, [draft, positions, accountName]);

  const fxRate = Number(fxFrom) > 0 && Number(fxTo) > 0 ? Number(fxFrom) / Number(fxTo) : null;

  function patchDraft(index: number, patch: Partial<TradeDraft>) {
    setDrafts((prev) => prev.map((d, i) => (i === index ? { ...d, ...patch } : d)));
  }

  function toggleTask(task: Task) {
    setError(null);
    setTasks((prev) => (prev.includes(task) ? prev.filter((t) => t !== task) : [...prev, task]));
  }

  /** 같은 종류의 매매 칸을 하나 더 만들고 그 종목 화면으로 옮긴다. */
  function addDraft() {
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    const isSell = draft?.isSell ?? false;
    setError(null);
    setDrafts((prev) => [...prev, emptyDraft(nextKey.current++, isSell)]);
    setStepIndex(stepIndex + 1);
  }

  /** 지금 화면의 입력이 다음으로 넘어갈 만한지. 문제가 있으면 메시지를 돌려준다. */
  function validate(): string | null {
    switch (step?.key) {
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
        return draft?.selection.symbol.trim() ? null : "종목을 골라주세요.";
      case "trade":
        if (!(parseFloat(draft?.quantity ?? "") > 0)) return "몇 주인지 적어주세요.";
        if (!(parseFloat(draft?.price ?? "") > 0)) return "한 주에 얼마였는지 적어주세요.";
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

  async function postTransaction(body: {
    type: string;
    amount: number;
    currency: string;
    fee: number;
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
        tag: null,
        memo: null,
        ...body,
      }),
    }).catch(() => null);
    return !!res?.ok;
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
        fee: 0,
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
          fee: parseFloat(fxFee) || 0,
          memo: null,
        }),
      }).catch(() => null);
      if (!res?.ok) return fail("환전");
      done.push("환전");
    }

    for (const d of drafts) {
      const label = `${d.selection.symbol} ${d.isSell ? "매도" : "매수"}`;
      const ok = await postTransaction({
        type: d.isSell ? "SELL" : "BUY",
        amount: draftAmount(d) ?? 0,
        currency: d.selection.currency,
        fee: parseFloat(d.fee) || 0,
        symbol: d.selection.symbol.trim(),
        quantity: parseFloat(d.quantity),
        price: parseFloat(d.price),
      });
      if (!ok) return fail(label);
      done.push(label);
      if (holdingLink && d.selection.market && d.selection.ticker.trim()) {
        await fetch("/api/portfolio/holdings", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            accountId,
            symbol: d.selection.symbol.trim(),
            market: d.selection.market,
            ticker: d.selection.ticker.trim(),
          }),
        }).catch(() => {});
      }
    }

    writeLastAccountId(accountId as number);
    onSaved();
  }

  const position = steps.length > 1 ? `${Math.min(stepIndex, steps.length - 1) + 1} / ${steps.length}` : "";
  const sameKindCount = draft ? drafts.filter((d) => d.isSell === draft.isSell).length : 0;

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

        {step?.key === "account" && (
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

        {step?.key === "tasks" && (
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

        {step?.key === "date" && (
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

        {step?.key === "deposit" && (
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

        {step?.key === "fx" && (
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
              {fxRate && <p className="text-sm text-slate-400">환율 약 {fxRate.toFixed(2)}원이네요.</p>}
              <FeeField
                open={feeOpen}
                onOpen={() => setFeeOpen(true)}
                label={`환전 수수료 (${fxFromCurrency})`}
                value={fxFee}
                onChange={setFxFee}
              />
            </div>
          </Question>
        )}

        {step?.key === "symbol" && draft && (
          <Question
            title={draft.isSell ? "어떤 주식을 파셨나요?" : "어떤 주식을 사셨나요?"}
            hint={
              sameKindCount > 1
                ? `${draft.isSell ? "판" : "산"} 종목 ${draftIndex + 1}번째예요.`
                : "전에 거래한 종목은 버튼 한 번으로 고를 수 있어요."
            }
          >
            {/* 계좌는 앞 단계에서 직접 답했으므로 종목 칩이 덮어쓰지 않는다. */}
            <SymbolPicker
              chips={chips}
              value={draft.selection}
              onChange={(selection) => patchDraft(draftIndex, { selection })}
            />
          </Question>
        )}

        {step?.key === "trade" && draft && (
          <Question
            title={`${draft.selection.symbol}, 몇 주를 얼마에 ${draft.isSell ? "파셨나요" : "사셨나요"}?`}
            hint={`한 주 가격은 ${draft.selection.currency} 기준으로 적어주세요.`}
          >
            <div className="flex flex-col gap-3">
              <Labeled label="몇 주">
                <CommaInput
                  decimal
                  value={draft.quantity}
                  onChange={(quantity) => patchDraft(draftIndex, { quantity })}
                  className={cx.inputNumber}
                />
              </Labeled>
              {heldQuantity != null && heldQuantity > 0 && (
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs text-slate-500">지금 {heldQuantity}주 있어요</span>
                  <button
                    type="button"
                    onClick={() => patchDraft(draftIndex, { quantity: String(heldQuantity) })}
                    className={cx.btnSm}
                  >
                    전부
                  </button>
                  <button
                    type="button"
                    onClick={() => patchDraft(draftIndex, { quantity: String(Number((heldQuantity / 2).toFixed(8))) })}
                    className={cx.btnSm}
                  >
                    반절
                  </button>
                </div>
              )}
              <Labeled label={`한 주에 얼마 (${draft.selection.currency})`}>
                <CommaInput
                  decimal
                  value={draft.price}
                  onChange={(price) => patchDraft(draftIndex, { price })}
                  className={cx.inputNumber}
                />
              </Labeled>
              {draftAmount(draft) != null && (
                <p className="text-sm text-slate-400">
                  모두 {natMoney(draftAmount(draft) as number, draft.selection.currency)}네요.
                </p>
              )}
              <FeeField
                open={feeOpen}
                onOpen={() => setFeeOpen(true)}
                label={`매매 수수료 (${draft.selection.currency})`}
                value={draft.fee}
                onChange={(fee) => patchDraft(draftIndex, { fee })}
              />
              <button type="button" onClick={addDraft} className={cx.btnSecondary + " self-start"}>
                ＋ {draft.isSell ? "판" : "산"} 종목이 더 있어요
              </button>
            </div>
          </Question>
        )}

        {step?.key === "confirm" && (
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
                    {parseFloat(fxFee) > 0 && ` · 수수료 ${natMoney(parseFloat(fxFee), fxFromCurrency)}`}
                  </Summary>
                )}
                {drafts.map((d) => (
                  <Summary key={d.key} label={d.isSell ? "매도" : "매수"}>
                    {d.selection.symbol} {d.quantity}주 × {natMoney(parseFloat(d.price) || 0, d.selection.currency)} ={" "}
                    {natMoney(draftAmount(d) ?? 0, d.selection.currency)}
                    {parseFloat(d.fee) > 0 && ` · 수수료 ${natMoney(parseFloat(d.fee), d.selection.currency)}`}
                  </Summary>
                ))}
              </div>

              {previews.some(Boolean) && (
                <div className="flex flex-col gap-3 rounded-xl border border-indigo-500/25 bg-indigo-600/10 p-4">
                  <div className="text-sm font-medium text-indigo-200">기록하면 이렇게 바뀌어요</div>
                  {drafts.map((d, i) => {
                    const preview = previews[i];
                    if (!preview) return null;
                    return (
                      <div key={d.key} className="flex flex-col gap-1">
                        {drafts.length > 1 && (
                          <div className="text-xs text-slate-400">{d.selection.symbol}</div>
                        )}
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
                            <strong
                              className={"font-medium " + (preview.realizedNat >= 0 ? "text-rose-300" : "text-sky-300")}
                            >
                              {natSigned(preview.realizedNat, preview.currency)}
                            </strong>
                          </Change>
                        )}
                        {preview.overSelling && (
                          <p className="text-sm text-amber-300">
                            지금 {preview.beforeQty}주를 들고 있는데 {d.quantity}주를 파는 것으로 적혀 있어요. 수량을
                            다시 확인해주세요.
                          </p>
                        )}
                      </div>
                    );
                  })}
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

/** 평소엔 접혀 있는 수수료 입력 — 필요할 때만 펼친다. */
function FeeField({
  open,
  onOpen,
  label,
  value,
  onChange,
}: {
  open: boolean;
  onOpen: () => void;
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  if (!open && !value) {
    return (
      <button type="button" onClick={onOpen} className="self-start text-xs text-slate-400 transition hover:text-white">
        ＋ 수수료도 적을게요
      </button>
    );
  }
  return (
    <Labeled label={label}>
      <CommaInput decimal value={value} onChange={onChange} placeholder="0" className={cx.inputNumber} />
    </Labeled>
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
