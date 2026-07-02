"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";
import Select from "@/components/Select";
import {
  ConditionNode,
  ConditionType,
  CompareOp,
  IndicatorType,
  PriceField,
  MarketTypeFilter,
  RankType,
  ModelSummary,
  StockStatusExclude,
  INDICATOR_LABELS,
  INDICATOR_GROUPS,
  PRICE_FIELD_LABELS,
  COMPARE_OP_LABELS,
  RANK_TYPE_LABELS,
  ALL_RANK_TYPES,
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
const RANK_ITEMS = ALL_RANK_TYPES.map(r => ({ value: r as string, label: RANK_TYPE_LABELS[r] }));

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

function OffsetInput({ value, onChange, label }: { value: number; onChange: (v: number) => void; label?: string }) {
  return (
    <div>
      <label className="text-xs text-slate-400 mb-1 block">{label ?? "오프셋"} (0=오늘 · 음수=과거 · 양수=미래)</label>
      <input
        type="number"
        value={value}
        onChange={(e) => onChange(parseInt(e.target.value, 10) || 0)}
        className={cx.inputNumber}
        placeholder="0"
      />
    </div>
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

  // 오프셋(거래일): 0=오늘, 음수=과거, 양수=미래
  const [indValue_offset, setIndValue_offset] = useState<number>(
    initial?.conditionType === "INDICATOR_VALUE" ? (initial.offset ?? 0) : 0);
  const [priceVal_offset, setPriceVal_offset] = useState<number>(
    initial?.conditionType === "PRICE_VALUE" ? (initial.offset ?? 0) : 0);
  const [volVal_offset, setVolVal_offset] = useState<number>(
    initial?.conditionType === "VOLUME_VALUE" ? (initial.offset ?? 0) : 0);
  const [cross_leftOffset, setCross_leftOffset] = useState<number>(
    initial?.conditionType === "INDICATOR_CROSS" ? (initial.leftOffset ?? 0) : 0);
  const [cross_rightOffset, setCross_rightOffset] = useState<number>(
    initial?.conditionType === "INDICATOR_CROSS" ? (initial.rightOffset ?? 0) : 0);
  const [pvi_leftOffset, setPvi_leftOffset] = useState<number>(
    initial?.conditionType === "PRICE_VS_INDICATOR" ? (initial.leftOffset ?? 0) : 0);
  const [pvi_rightOffset, setPvi_rightOffset] = useState<number>(
    initial?.conditionType === "PRICE_VS_INDICATOR" ? (initial.rightOffset ?? 0) : 0);
  const [indRange_offset, setIndRange_offset] = useState<number>(
    initial?.conditionType === "INDICATOR_RANGE" ? (initial.offset ?? 0) : 0);
  const [priceRange_offset, setPriceRange_offset] = useState<number>(
    initial?.conditionType === "PRICE_RANGE" ? (initial.offset ?? 0) : 0);
  const [volRange_offset, setVolRange_offset] = useState<number>(
    initial?.conditionType === "VOLUME_RANGE" ? (initial.offset ?? 0) : 0);

  // ── 종목상태 제외 ───────────────────────────────────────────────────────────────
  const initStatusExclude =
    initial?.conditionType === "STOCK_STATUS"
      ? (initial.exclude.length > 0 ? initial.exclude : (["TRADING_HALT", "ADMIN_ITEM"] as StockStatusExclude[]))
      : (["TRADING_HALT", "ADMIN_ITEM"] as StockStatusExclude[]);
  const [excludeHalt, setExcludeHalt] = useState(initStatusExclude.includes("TRADING_HALT"));
  const [excludeAdmin, setExcludeAdmin] = useState(initStatusExclude.includes("ADMIN_ITEM"));

  // ── 모델 점수 ────────────────────────────────────────────────────────────────
  const [models, setModels] = useState<ModelSummary[]>([]);
  const isModelCondition = conditionType === "MODEL_SCORE_VALUE" || conditionType === "MODEL_SCORE_RANGE";

  const [modelScoreVal_modelId, setModelScoreVal_modelId] = useState<number | null>(
    initial?.conditionType === "MODEL_SCORE_VALUE" ? initial.modelId : null);
  const [modelScoreVal_offset, setModelScoreVal_offset] = useState<number>(
    initial?.conditionType === "MODEL_SCORE_VALUE" ? (initial.offset ?? 0) : 0);
  const [modelScoreVal_op, setModelScoreVal_op] = useState<CompareOp>(
    initial?.conditionType === "MODEL_SCORE_VALUE" ? initial.operator : "GTE");
  const [modelScoreVal_val, setModelScoreVal_val] = useState<number>(
    initial?.conditionType === "MODEL_SCORE_VALUE" ? initial.value : 0);

  const [modelScoreRange_modelId, setModelScoreRange_modelId] = useState<number | null>(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? initial.modelId : null);
  const [modelScoreRange_offset, setModelScoreRange_offset] = useState<number>(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? (initial.offset ?? 0) : 0);
  const [modelScoreRange_min, setModelScoreRange_min] = useState<number>(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? initial.minValue : 0);
  const [modelScoreRange_minInc, setModelScoreRange_minInc] = useState(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? initial.minInclusive : true);
  const [modelScoreRange_max, setModelScoreRange_max] = useState<number>(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? initial.maxValue : 1);
  const [modelScoreRange_maxInc, setModelScoreRange_maxInc] = useState(
    initial?.conditionType === "MODEL_SCORE_RANGE" ? initial.maxInclusive : true);

  // ── 순위 ──────────────────────────────────────────────────────────────────────
  const [rankVal_rank, setRankVal_rank] = useState<RankType>(
    initial?.conditionType === "RANK_VALUE" ? initial.rank : "RANK_TURNOVER");
  const [rankVal_offset, setRankVal_offset] = useState<number>(
    initial?.conditionType === "RANK_VALUE" ? (initial.offset ?? 0) : 0);
  const [rankVal_op, setRankVal_op] = useState<CompareOp>(
    initial?.conditionType === "RANK_VALUE" ? initial.operator : "GTE");
  const [rankVal_val, setRankVal_val] = useState<number>(
    initial?.conditionType === "RANK_VALUE" ? initial.value : 0);

  const [rankRange_rank, setRankRange_rank] = useState<RankType>(
    initial?.conditionType === "RANK_RANGE" ? initial.rank : "RANK_TURNOVER");
  const [rankRange_offset, setRankRange_offset] = useState<number>(
    initial?.conditionType === "RANK_RANGE" ? (initial.offset ?? 0) : 0);
  const [rankRange_min, setRankRange_min] = useState<number>(
    initial?.conditionType === "RANK_RANGE" ? initial.minValue : 0);
  const [rankRange_minInc, setRankRange_minInc] = useState(
    initial?.conditionType === "RANK_RANGE" ? initial.minInclusive : true);
  const [rankRange_max, setRankRange_max] = useState<number>(
    initial?.conditionType === "RANK_RANGE" ? initial.maxValue : 1);
  const [rankRange_maxInc, setRankRange_maxInc] = useState(
    initial?.conditionType === "RANK_RANGE" ? initial.maxInclusive : true);

  // ── 당일 상승비율(시장 폭) ──────────────────────────────────────────────────────
  const [breadthVal_offset, setBreadthVal_offset] = useState<number>(
    initial?.conditionType === "BREADTH_VALUE" ? (initial.offset ?? 0) : 0);
  const [breadthVal_op, setBreadthVal_op] = useState<CompareOp>(
    initial?.conditionType === "BREADTH_VALUE" ? initial.operator : "GTE");
  const [breadthVal_val, setBreadthVal_val] = useState<number>(
    initial?.conditionType === "BREADTH_VALUE" ? initial.value : 0.45);

  const [breadthRange_offset, setBreadthRange_offset] = useState<number>(
    initial?.conditionType === "BREADTH_RANGE" ? (initial.offset ?? 0) : 0);
  const [breadthRange_min, setBreadthRange_min] = useState<number>(
    initial?.conditionType === "BREADTH_RANGE" ? initial.minValue : 0);
  const [breadthRange_minInc, setBreadthRange_minInc] = useState(
    initial?.conditionType === "BREADTH_RANGE" ? initial.minInclusive : true);
  const [breadthRange_max, setBreadthRange_max] = useState<number>(
    initial?.conditionType === "BREADTH_RANGE" ? initial.maxValue : 1);
  const [breadthRange_maxInc, setBreadthRange_maxInc] = useState(
    initial?.conditionType === "BREADTH_RANGE" ? initial.maxInclusive : true);

  useEffect(() => {
    if (!isModelCondition) return;
    fetch("/api/stocks/models")
      .then((r) => (r.ok ? r.json() : []))
      .then((data: ModelSummary[]) => {
        setModels(data);
        // 신규 조건이면 첫 모델을 기본 선택
        if (data.length > 0) {
          setModelScoreVal_modelId((cur) => cur ?? data[0].id);
          setModelScoreRange_modelId((cur) => cur ?? data[0].id);
        }
      })
      .catch(() => setModels([]));
  }, [isModelCondition]);

  const MODEL_ITEMS = models.map((m) => ({
    value: String(m.id),
    label: `${m.name} (v${m.version})`,
  }));

  function buildNode(): ConditionNode {
    const id = initial?.id ?? generateId();
    const negated = initial?.negated ?? false;
    switch (conditionType) {
      case "INDICATOR_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, indicator: indValue_indicator, offset: indValue_offset, operator: indValue_op, value: indValue_val };
      case "INDICATOR_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, indicator: indRange_ind, offset: indRange_offset, minValue: indRange_min, minInclusive: indRange_minInc, maxValue: indRange_max, maxInclusive: indRange_maxInc };
      case "INDICATOR_CROSS":
        return { id, nodeType: "CONDITION", negated, conditionType, leftIndicator: cross_left, leftOffset: cross_leftOffset, operator: cross_op, rightIndicator: cross_right, rightOffset: cross_rightOffset };
      case "PRICE_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: price_field, offset: priceVal_offset, operator: priceVal_op, value: priceVal_val };
      case "PRICE_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: price_field, offset: priceRange_offset, minValue: priceRange_min, minInclusive: priceRange_minInc, maxValue: priceRange_max, maxInclusive: priceRange_maxInc };
      case "VOLUME_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, offset: volVal_offset, operator: volVal_op, value: volVal_val };
      case "VOLUME_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, offset: volRange_offset, minValue: volRange_min, minInclusive: volRange_minInc, maxValue: volRange_max, maxInclusive: volRange_maxInc };
      case "PRICE_VS_INDICATOR":
        return { id, nodeType: "CONDITION", negated, conditionType, priceField: priceVsInd_field, leftOffset: pvi_leftOffset, operator: priceVsInd_op, indicator: priceVsInd_ind, rightOffset: pvi_rightOffset } as PriceVsIndicatorCondition;
      case "MARKET_FILTER":
        return { id, nodeType: "CONDITION", negated, conditionType, markets: Array.from(markets) };
      case "MODEL_SCORE_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, modelId: modelScoreVal_modelId ?? 0, offset: modelScoreVal_offset, operator: modelScoreVal_op, value: modelScoreVal_val };
      case "MODEL_SCORE_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, modelId: modelScoreRange_modelId ?? 0, offset: modelScoreRange_offset, minValue: modelScoreRange_min, minInclusive: modelScoreRange_minInc, maxValue: modelScoreRange_max, maxInclusive: modelScoreRange_maxInc };
      case "RANK_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, rank: rankVal_rank, offset: rankVal_offset, operator: rankVal_op, value: rankVal_val };
      case "RANK_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, rank: rankRange_rank, offset: rankRange_offset, minValue: rankRange_min, minInclusive: rankRange_minInc, maxValue: rankRange_max, maxInclusive: rankRange_maxInc };
      case "BREADTH_VALUE":
        return { id, nodeType: "CONDITION", negated, conditionType, offset: breadthVal_offset, operator: breadthVal_op, value: breadthVal_val };
      case "BREADTH_RANGE":
        return { id, nodeType: "CONDITION", negated, conditionType, offset: breadthRange_offset, minValue: breadthRange_min, minInclusive: breadthRange_minInc, maxValue: breadthRange_max, maxInclusive: breadthRange_maxInc };
      case "STOCK_STATUS": {
        const exclude: StockStatusExclude[] = [];
        if (excludeHalt) exclude.push("TRADING_HALT");
        if (excludeAdmin) exclude.push("ADMIN_ITEM");
        return { id, nodeType: "CONDITION", negated, conditionType, exclude };
      }
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
    MODEL_SCORE_VALUE: "모델 점수 비교",
    MODEL_SCORE_RANGE: "모델 점수 범위",
    RANK_VALUE: "순위 비교",
    RANK_RANGE: "순위 범위",
    BREADTH_VALUE: "당일 상승비율 비교",
    BREADTH_RANGE: "당일 상승비율 범위",
    STOCK_STATUS: "종목상태 (거래정지·관리종목 제외)",
  };

  const noModelAvailable = isModelCondition && models.length === 0;

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
              <OffsetInput value={indValue_offset} onChange={setIndValue_offset} />
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
              <OffsetInput value={indRange_offset} onChange={setIndRange_offset} />
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
              <OffsetInput label="좌측 오프셋" value={cross_leftOffset} onChange={setCross_leftOffset} />
              <div>
                <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                <OpSelect value={cross_op} onChange={setCross_op} />
              </div>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">우측 지표</label>
                <IndicatorSelect value={cross_right} onChange={setCross_right} />
              </div>
              <OffsetInput label="우측 오프셋" value={cross_rightOffset} onChange={setCross_rightOffset} />
            </>
          )}

          {conditionType === "PRICE_VALUE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">가격 필드</label>
                <Select value={price_field} items={PRICE_FIELD_ITEMS} onChange={v => setPrice_field(v as PriceField)} className="w-full" />
              </div>
              <OffsetInput value={priceVal_offset} onChange={setPriceVal_offset} />
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
              <OffsetInput value={priceRange_offset} onChange={setPriceRange_offset} />
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
              <OffsetInput label="가격 오프셋" value={pvi_leftOffset} onChange={setPvi_leftOffset} />
              <div>
                <label className="text-xs text-slate-400 mb-1 block">비교 지표</label>
                <IndicatorSelect value={priceVsInd_ind} onChange={setPriceVsInd_ind} />
              </div>
              <OffsetInput label="지표 오프셋" value={pvi_rightOffset} onChange={setPvi_rightOffset} />
              <p className="text-xs text-slate-500">
                예: 종가 &gt; SMA_20 (상향돌파), 종가 &lt; BB_LOWER_20 (하단 터치)
              </p>
            </>
          )}

          {conditionType === "VOLUME_VALUE" && (
            <>
              <OffsetInput value={volVal_offset} onChange={setVolVal_offset} />
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
            </>
          )}

          {conditionType === "VOLUME_RANGE" && (
            <>
              <OffsetInput value={volRange_offset} onChange={setVolRange_offset} />
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

          {conditionType === "MODEL_SCORE_VALUE" && (
            <>
              {noModelAvailable ? (
                <p className="text-sm text-amber-400">활성화된 모델이 없습니다. 관리자에게 모델 등록을 요청하세요.</p>
              ) : (
                <>
                  <div>
                    <label className="text-xs text-slate-400 mb-1 block">모델</label>
                    <Select value={modelScoreVal_modelId != null ? String(modelScoreVal_modelId) : null}
                      items={MODEL_ITEMS} onChange={v => setModelScoreVal_modelId(Number(v))} className="w-full" />
                  </div>
                  <OffsetInput value={modelScoreVal_offset} onChange={setModelScoreVal_offset} />
                  <div className="flex gap-2">
                    <div>
                      <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                      <OpSelect value={modelScoreVal_op} onChange={setModelScoreVal_op} />
                    </div>
                    <div className="flex-1">
                      <label className="text-xs text-slate-400 mb-1 block">점수</label>
                      <NumberInput value={modelScoreVal_val} onChange={setModelScoreVal_val} placeholder="예: 0.7" />
                    </div>
                  </div>
                  <p className="text-xs text-slate-500">확률 모델은 0~1, 회귀 모델은 예측값으로 비교합니다.</p>
                </>
              )}
            </>
          )}

          {conditionType === "MODEL_SCORE_RANGE" && (
            <>
              {noModelAvailable ? (
                <p className="text-sm text-amber-400">활성화된 모델이 없습니다. 관리자에게 모델 등록을 요청하세요.</p>
              ) : (
                <>
                  <div>
                    <label className="text-xs text-slate-400 mb-1 block">모델</label>
                    <Select value={modelScoreRange_modelId != null ? String(modelScoreRange_modelId) : null}
                      items={MODEL_ITEMS} onChange={v => setModelScoreRange_modelId(Number(v))} className="w-full" />
                  </div>
                  <OffsetInput value={modelScoreRange_offset} onChange={setModelScoreRange_offset} />
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-slate-400 mb-1 block">최솟값</label>
                      <NumberInput value={modelScoreRange_min} onChange={setModelScoreRange_min} placeholder="예: 0.6" />
                    </div>
                    <div>
                      <label className="text-xs text-slate-400 mb-1 block">최댓값</label>
                      <NumberInput value={modelScoreRange_max} onChange={setModelScoreRange_max} placeholder="예: 0.9" />
                    </div>
                  </div>
                  <div className="flex gap-4">
                    <InclusiveToggle label="최솟값 포함" value={modelScoreRange_minInc} onChange={setModelScoreRange_minInc} />
                    <InclusiveToggle label="최댓값 포함" value={modelScoreRange_maxInc} onChange={setModelScoreRange_maxInc} />
                  </div>
                </>
              )}
            </>
          )}

          {conditionType === "RANK_VALUE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">순위 종류</label>
                <Select value={rankVal_rank} items={RANK_ITEMS} onChange={v => setRankVal_rank(v as RankType)} className="w-full" />
              </div>
              <OffsetInput value={rankVal_offset} onChange={setRankVal_offset} />
              <div className="flex gap-2">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                  <OpSelect value={rankVal_op} onChange={setRankVal_op} />
                </div>
                <div className="flex-1">
                  <label className="text-xs text-slate-400 mb-1 block">순위 (0~1)</label>
                  <NumberInput value={rankVal_val} onChange={setRankVal_val} placeholder="예: 0.9 (상위 10%)" />
                </div>
              </div>
              <p className="text-xs text-slate-500">순위는 0~1 사이 백분위(1=최상위)입니다.</p>
            </>
          )}

          {conditionType === "RANK_RANGE" && (
            <>
              <div>
                <label className="text-xs text-slate-400 mb-1 block">순위 종류</label>
                <Select value={rankRange_rank} items={RANK_ITEMS} onChange={v => setRankRange_rank(v as RankType)} className="w-full" />
              </div>
              <OffsetInput value={rankRange_offset} onChange={setRankRange_offset} />
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최솟값 (0~1)</label>
                  <NumberInput value={rankRange_min} onChange={setRankRange_min} placeholder="예: 0.8" />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최댓값 (0~1)</label>
                  <NumberInput value={rankRange_max} onChange={setRankRange_max} placeholder="예: 1" />
                </div>
              </div>
              <div className="flex gap-4">
                <InclusiveToggle label="최솟값 포함" value={rankRange_minInc} onChange={setRankRange_minInc} />
                <InclusiveToggle label="최댓값 포함" value={rankRange_maxInc} onChange={setRankRange_maxInc} />
              </div>
            </>
          )}

          {conditionType === "BREADTH_VALUE" && (
            <>
              <OffsetInput value={breadthVal_offset} onChange={setBreadthVal_offset} />
              <div className="flex gap-2">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">비교 연산자</label>
                  <OpSelect value={breadthVal_op} onChange={setBreadthVal_op} />
                </div>
                <div className="flex-1">
                  <label className="text-xs text-slate-400 mb-1 block">당일 상승비율 (0~1)</label>
                  <NumberInput value={breadthVal_val} onChange={setBreadthVal_val} placeholder="예: 0.45" />
                </div>
              </div>
              <p className="text-xs text-slate-500">당일 상승비율은 0~1 사이 값(1=전종목 상승)입니다.</p>
            </>
          )}

          {conditionType === "BREADTH_RANGE" && (
            <>
              <OffsetInput value={breadthRange_offset} onChange={setBreadthRange_offset} />
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최솟값 (0~1)</label>
                  <NumberInput value={breadthRange_min} onChange={setBreadthRange_min} placeholder="예: 0.45" />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">최댓값 (0~1)</label>
                  <NumberInput value={breadthRange_max} onChange={setBreadthRange_max} placeholder="예: 1" />
                </div>
              </div>
              <div className="flex gap-4">
                <InclusiveToggle label="최솟값 포함" value={breadthRange_minInc} onChange={setBreadthRange_minInc} />
                <InclusiveToggle label="최댓값 포함" value={breadthRange_maxInc} onChange={setBreadthRange_maxInc} />
              </div>
            </>
          )}

          {conditionType === "STOCK_STATUS" && (
            <>
              <label className="text-xs text-slate-400 mb-1 block">결과에서 제외할 종목상태</label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setExcludeHalt((v) => !v)}
                  className={excludeHalt ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  거래정지 제외
                </button>
                <button
                  type="button"
                  onClick={() => setExcludeAdmin((v) => !v)}
                  className={excludeAdmin ? cx.btnToggleOn : cx.btnToggleOff}
                >
                  관리종목 제외
                </button>
              </div>
              <p className="text-xs text-slate-500">
                현재 상태 기준 — 기준일이 최신일자일 때만 적용됩니다(과거일자에선 무시).
              </p>
            </>
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
            disabled={noModelAvailable}
            className={`flex-1 ${cx.btnPrimary}${noModelAvailable ? " opacity-40 cursor-not-allowed" : ""}`}
          >
            {initial ? "수정" : "추가"}
          </button>
        </div>
      </div>
    </div>
  );
}
