"use client";

import { useState } from "react";
import { cx } from "@/utils/cx";
import Select from "@/components/Select";
import {
  ConditionNode,
  ConditionType,
  CompareOp,
  IndicatorType,
  PriceField,
  MarketTypeFilter,
  INDICATOR_LABELS,
  INDICATOR_GROUPS,
  PRICE_FIELD_LABELS,
  COMPARE_OP_LABELS,
  PriceVsIndicatorCondition,
} from "@/types/filter";
import { generateId } from "@/utils/filter";

interface Props {
  conditionType: ConditionType;
  initial?: ConditionNode;
  onConfirm: (node: ConditionNode) => void;
  onClose: () => void;
}

const COMPARE_OPS: CompareOp[] = ["GT", "GTE", "LT", "LTE", "EQ", "NEQ"];
const PRICE_FIELDS: PriceField[] = ["OPEN", "HIGH", "LOW", "CLOSE"];

const INDICATOR_ITEMS = INDICATOR_GROUPS.map(g => ({
  group: g.label,
  options: g.types.map(t => ({ value: t as string, label: INDICATOR_LABELS[t] })),
}));
const OP_ITEMS = COMPARE_OPS.map(op => ({ value: op as string, label: COMPARE_OP_LABELS[op] }));
const PRICE_FIELD_ITEMS = PRICE_FIELDS.map(f => ({ value: f as string, label: PRICE_FIELD_LABELS[f] }));

function IndicatorSelect({ value, onChange }: { value: IndicatorType; onChange: (v: IndicatorType) => void }) {
  return <Select value={value} items={INDICATOR_ITEMS} onChange={v => onChange(v as IndicatorType)} className="w-full" />;
}

function OpSelect({ value, onChange }: { value: CompareOp; onChange: (v: CompareOp) => void }) {
  return <Select value={value} items={OP_ITEMS} onChange={v => onChange(v as CompareOp)} />;
}

function NumberInput({
  value,
  onChange,
  placeholder,
}: {
  value: number | "";
  onChange: (v: number) => void;
  placeholder?: string;
}) {
  const initNum = typeof value === "number" ? value : 0;
  const [raw, setRaw] = useState<string>(initNum !== 0 ? String(initNum) : "");
  const [focused, setFocused] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const digits = e.target.value.replace(/[^0-9.]/g, "");
    setRaw(digits);
    onChange(parseFloat(digits) || 0);
  };

  const display = focused ? raw : raw ? parseFloat(raw).toLocaleString("ko-KR") : "";

  return (
    <input
      type="text"
      inputMode="decimal"
      value={display}
      onChange={handleChange}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      placeholder={placeholder}
      className={cx.input}
    />
  );
}

function InclusiveToggle({ label, value, onChange }: { label: string; value: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-xs text-slate-400">{label}</span>
      <button
        type="button"
        onClick={() => onChange(!value)}
        className={`px-2 py-1 rounded text-xs font-mono transition ${
          value ? "bg-indigo-600 text-white" : "bg-slate-700 text-slate-400 border border-white/15"
        }`}
      >
        {value ? "이상/이하 (≤/≥)" : "초과/미만 (</>)"}
      </button>
    </div>
  );
}

export default function ConditionEditorModal({ conditionType, initial, onConfirm, onClose }: Props) {
  const defaultIndicator: IndicatorType = "RSI_14";
  const defaultOp: CompareOp = "GT";

  const [indValue_indicator, setIndValue_indicator] = useState<IndicatorType>(
    initial?.conditionType === "INDICATOR_VALUE" ? initial.indicator : defaultIndicator
  );
  const [indValue_op, setIndValue_op] = useState<CompareOp>(
    initial?.conditionType === "INDICATOR_VALUE" ? initial.operator : defaultOp
  );
  const [indValue_val, setIndValue_val] = useState<number>(
    initial?.conditionType === "INDICATOR_VALUE" ? initial.value : 0
  );

  const [indRange_ind, setIndRange_ind] = useState<IndicatorType>(
    initial?.conditionType === "INDICATOR_RANGE" ? initial.indicator : defaultIndicator
  );
  const [indRange_min, setIndRange_min] = useState<number>(
    initial?.conditionType === "INDICATOR_RANGE" ? initial.minValue : 0
  );
  const [indRange_minInc, setIndRange_minInc] = useState(
    initial?.conditionType === "INDICATOR_RANGE" ? initial.minInclusive : true
  );
  const [indRange_max, setIndRange_max] = useState<number>(
    initial?.conditionType === "INDICATOR_RANGE" ? initial.maxValue : 100
  );
  const [indRange_maxInc, setIndRange_maxInc] = useState(
    initial?.conditionType === "INDICATOR_RANGE" ? initial.maxInclusive : true
  );

  const [cross_left, setCross_left] = useState<IndicatorType>(
    initial?.conditionType === "INDICATOR_CROSS" ? initial.leftIndicator : "SMA_5"
  );
  const [cross_op, setCross_op] = useState<CompareOp>(
    initial?.conditionType === "INDICATOR_CROSS" ? initial.operator : "GT"
  );
  const [cross_right, setCross_right] = useState<IndicatorType>(
    initial?.conditionType === "INDICATOR_CROSS" ? initial.rightIndicator : "SMA_20"
  );

  const [price_field, setPrice_field] = useState<PriceField>(
    initial?.conditionType === "PRICE_VALUE" || initial?.conditionType === "PRICE_RANGE"
      ? initial.priceField
      : "CLOSE"
  );
  const [priceVal_op, setPriceVal_op] = useState<CompareOp>(
    initial?.conditionType === "PRICE_VALUE" ? initial.operator : defaultOp
  );
  const [priceVal_val, setPriceVal_val] = useState<number>(
    initial?.conditionType === "PRICE_VALUE" ? initial.value : 0
  );

  const [priceRange_min, setPriceRange_min] = useState<number>(
    initial?.conditionType === "PRICE_RANGE" ? initial.minValue : 0
  );
  const [priceRange_minInc, setPriceRange_minInc] = useState(
    initial?.conditionType === "PRICE_RANGE" ? initial.minInclusive : true
  );
  const [priceRange_max, setPriceRange_max] = useState<number>(
    initial?.conditionType === "PRICE_RANGE" ? initial.maxValue : 0
  );
  const [priceRange_maxInc, setPriceRange_maxInc] = useState(
    initial?.conditionType === "PRICE_RANGE" ? initial.maxInclusive : true
  );

  const [volVal_op, setVolVal_op] = useState<CompareOp>(
    initial?.conditionType === "VOLUME_VALUE" ? initial.operator : defaultOp
  );
  const [volVal_val, setVolVal_val] = useState<number>(
    initial?.conditionType === "VOLUME_VALUE" ? initial.value : 0
  );

  const [volRange_min, setVolRange_min] = useState<number>(
    initial?.conditionType === "VOLUME_RANGE" ? initial.minValue : 0
  );
  const [volRange_minInc, setVolRange_minInc] = useState(
    initial?.conditionType === "VOLUME_RANGE" ? initial.minInclusive : true
  );
  const [volRange_max, setVolRange_max] = useState<number>(
    initial?.conditionType === "VOLUME_RANGE" ? initial.maxValue : 0
  );
  const [volRange_maxInc, setVolRange_maxInc] = useState(
    initial?.conditionType === "VOLUME_RANGE" ? initial.maxInclusive : true
  );

  const [priceVsInd_field, setPriceVsInd_field] = useState<PriceField>(
    initial?.conditionType === "PRICE_VS_INDICATOR" ? initial.priceField : "CLOSE"
  );
  const [priceVsInd_op, setPriceVsInd_op] = useState<CompareOp>(
    initial?.conditionType === "PRICE_VS_INDICATOR" ? initial.operator : "GT"
  );
  const [priceVsInd_ind, setPriceVsInd_ind] = useState<IndicatorType>(
    initial?.conditionType === "PRICE_VS_INDICATOR" ? initial.indicator : "SMA_20"
  );

  const [markets, setMarkets] = useState<Set<MarketTypeFilter>>(
    new Set(
      initial?.conditionType === "MARKET_FILTER"
        ? initial.markets
        : (["KOSPI", "KOSDAQ", "KONEX"] as MarketTypeFilter[])
    )
  );

  function buildNode(): ConditionNode {
    const id = initial?.id ?? generateId();
    const negated = initial?.negated ?? false;
    switch (conditionType) {
      case "INDICATOR_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, indicator: indValue_indicator, operator: indValue_op, value: indValue_val };
      case "INDICATOR_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, indicator: indRange_ind, minValue: indRange_min, minInclusive: indRange_minInc, maxValue: indRange_max, maxInclusive: indRange_maxInc };
      case "INDICATOR_CROSS":
        return { id, nodeType: "CONDITION", negated, conditionType, leftIndicator: cross_left, operator: cross_op, rightIndicator: cross_right };
      case "PRICE_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: price_field, operator: priceVal_op, value: priceVal_val };
      case "PRICE_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: price_field, minValue: priceRange_min, minInclusive: priceRange_minInc, maxValue: priceRange_max, maxInclusive: priceRange_maxInc };
      case "VOLUME_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, operator: volVal_op, value: volVal_val };
      case "VOLUME_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, minValue: volRange_min, minInclusive: volRange_minInc, maxValue: volRange_max, maxInclusive: volRange_maxInc };
      case "PRICE_VS_INDICATOR":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: priceVsInd_field, operator: priceVsInd_op, indicator: priceVsInd_ind } as PriceVsIndicatorCondition;
      case "MARKET_FILTER":
        return { id, nodeType: "CONDITION", negated, conditionType, markets: Array.from(markets) };
    }
  }

  const CONDITION_TYPE_LABELS: Record<ConditionType, string> = {
    PRICE_VS_INDICATOR: "가격 vs 지표",
    INDICATOR_VALUE: "지표 값 비교",
    INDICATOR_RANGE: "지표 범위",
    INDICATOR_CROSS: "지표 교차",
    PRICE_VALUE: "가격 비교",
    PRICE_RANGE: "가격 범위",
    VOLUME_VALUE: "거래량 비교",
    VOLUME_RANGE: "거래량 범위",
    MARKET_FILTER: "시장 필터",
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60" onClick={onClose}>
      <div
        className="bg-slate-800 border border-white/15 rounded-xl shadow-2xl w-full max-w-md mx-4 p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-white font-semibold">{CONDITION_TYPE_LABELS[conditionType]}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-white transition">
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-4">
          {conditionType === "INDICATOR_VALUE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">지표</label>
                <IndicatorSelect value={indValue_indicator} onChange={setIndValue_indicator} />
              </div>
              <div className="flex gap-2">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                  <OpSelect value={indValue_op} onChange={setIndValue_op} />
                </div>
                <div className="flex-1">
                  <label className="text-xs text-slate-400 mb-1 block">값</label>
                  <NumberInput value={indValue_val} onChange={setIndValue_val} placeholder="예: 70" />
                </div>
              </div>
            </>
          )}

          {conditionType === "INDICATOR_RANGE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">지표</label>
                <IndicatorSelect value={indRange_ind} onChange={setIndRange_ind} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최솟값</label>
                  <NumberInput value={indRange_min} onChange={setIndRange_min} placeholder="최솟값" />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최댓값</label>
                  <NumberInput value={indRange_max} onChange={setIndRange_max} placeholder="최댓값" />
                </div>
              </div>
              <div className="flex gap-4">
                <InclusiveToggle label="최솟값 포함" value={indRange_minInc} onChange={setIndRange_minInc} />
                <InclusiveToggle label="최댓값 포함" value={indRange_maxInc} onChange={setIndRange_maxInc} />
              </div>
            </>
          )}

          {conditionType === "INDICATOR_CROSS" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">좌측 지표</label>
                <IndicatorSelect value={cross_left} onChange={setCross_left} />
              </div>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                <OpSelect value={cross_op} onChange={setCross_op} />
              </div>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">우측 지표</label>
                <IndicatorSelect value={cross_right} onChange={setCross_right} />
              </div>
            </>
          )}

          {conditionType === "PRICE_VALUE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">가격 필드</label>
                <Select value={price_field} items={PRICE_FIELD_ITEMS} onChange={v => setPrice_field(v as PriceField)} className="w-full" />
              </div>
              <div className="flex gap-2">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                  <OpSelect value={priceVal_op} onChange={setPriceVal_op} />
                </div>
                <div className="flex-1">
                  <label className="text-xs text-slate-400 mb-1 block">값(원)</label>
                  <NumberInput value={priceVal_val} onChange={setPriceVal_val} placeholder="예: 50000" />
                </div>
              </div>
            </>
          )}

          {conditionType === "PRICE_RANGE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">가격 필드</label>
                <Select value={price_field} items={PRICE_FIELD_ITEMS} onChange={v => setPrice_field(v as PriceField)} className="w-full" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최솟값(원)</label>
                  <NumberInput value={priceRange_min} onChange={setPriceRange_min} placeholder="예: 5000" />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최댓값(원)</label>
                  <NumberInput value={priceRange_max} onChange={setPriceRange_max} placeholder="예: 100000" />
                </div>
              </div>
              <div className="flex gap-4">
                <InclusiveToggle label="최솟값 포함" value={priceRange_minInc} onChange={setPriceRange_minInc} />
                <InclusiveToggle label="최댓값 포함" value={priceRange_maxInc} onChange={setPriceRange_maxInc} />
              </div>
            </>
          )}

          {conditionType === "PRICE_VS_INDICATOR" && (
            <>
              <div className="flex gap-2">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">가격 필드</label>
                  <Select value={priceVsInd_field} items={PRICE_FIELD_ITEMS} onChange={v => setPriceVsInd_field(v as PriceField)} />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                  <OpSelect value={priceVsInd_op} onChange={setPriceVsInd_op} />
                </div>
              </div>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">비교 지표</label>
                <IndicatorSelect value={priceVsInd_ind} onChange={setPriceVsInd_ind} />
              </div>
              <p className="text-xs text-slate-500">
                예: 종가 &gt; SMA_20 (상향돌파), 종가 &lt; BB_LOWER_20 (하단 터치)
              </p>
            </>
          )}

          {conditionType === "VOLUME_VALUE" && (
            <div className="flex gap-2">
              <div>
                <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                <OpSelect value={volVal_op} onChange={setVolVal_op} />
              </div>
              <div className="flex-1">
                <label className="text-xs text-slate-400 mb-1 block">거래량</label>
                <NumberInput value={volVal_val} onChange={setVolVal_val} placeholder="예: 1000000" />
              </div>
            </div>
          )}

          {conditionType === "VOLUME_RANGE" && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최솟값</label>
                  <NumberInput value={volRange_min} onChange={setVolRange_min} placeholder="최솟값" />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최댓값</label>
                  <NumberInput value={volRange_max} onChange={setVolRange_max} placeholder="최댓값" />
                </div>
              </div>
              <div className="flex gap-4">
                <InclusiveToggle label="최솟값 포함" value={volRange_minInc} onChange={setVolRange_minInc} />
                <InclusiveToggle label="최댓값 포함" value={volRange_maxInc} onChange={setVolRange_maxInc} />
              </div>
            </>
          )}

          {conditionType === "MARKET_FILTER" && (
            <div>
              <label className="text-xs text-slate-400 mb-2 block">시장 선택</label>
              <div className="flex gap-3">
                {(["KOSPI", "KOSDAQ", "KONEX"] as MarketTypeFilter[]).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => {
                      const next = new Set(markets);
                      if (next.has(m)) next.delete(m);
                      else next.add(m);
                      setMarkets(next);
                    }}
                    className={markets.has(m) ? cx.btnToggleOn : cx.btnToggleOff}
                  >
                    {m}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="flex gap-3 mt-6">
          <button
            onClick={onClose}
            className={`flex-1 ${cx.btnSecondary}`}
          >
            취소
          </button>
          <button
            onClick={() => onConfirm(buildNode())}
            className={`flex-1 ${cx.btnPrimary}`}
          >
            {initial ? "수정" : "추가"}
          </button>
        </div>
      </div>
    </div>
  );
}

