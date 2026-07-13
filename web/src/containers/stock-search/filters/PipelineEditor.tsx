"use client";

import { useEffect, useState } from "react";
import { cx } from "@/utils/cx";
import Select from "@/components/Select";
import {
  GroupNode,
  ConditionType,
  ConditionNames,
  ModelSummary,
  PipelineStageState,
  SortKey,
  SortField,
  SortDirection,
  SORT_FIELDS,
  SORT_FIELD_LABELS,
  SORT_DIRECTION_LABELS,
} from "@/types/filter";
import { createRankStage, createFilterStage, summarizeRankStage } from "@/utils/filter";
import ExpressionTree from "./ExpressionTree";
import ConditionPalette from "./ConditionPalette";

interface Props {
  stages: PipelineStageState[];
  onChange: (stages: PipelineStageState[]) => void;
  names?: ConditionNames;
}

/**
 * 검색식 뒤에 이어지는 순서 단계(정렬·순위/추가 조건) 편집기.
 */
export default function PipelineEditor({ stages, onChange, names }: Props) {
  // 모델 점수 정렬용 모델 목록(1회 로드). 실패/빈 목록이면 드롭다운은 비고 폴백.
  const [models, setModels] = useState<ModelSummary[]>([]);
  useEffect(() => {
    fetch("/api/stocks/models")
      .then((r) => (r.ok ? r.json() : []))
      .then((data: ModelSummary[]) => setModels(Array.isArray(data) ? data : []))
      .catch(() => setModels([]));
  }, []);

  function updateStage(index: number, next: PipelineStageState) {
    onChange(stages.map((s, i) => (i === index ? next : s)));
  }

  function removeStage(index: number) {
    onChange(stages.filter((_, i) => i !== index));
  }

  function moveStage(index: number, dir: -1 | 1) {
    const target = index + dir;
    if (target < 0 || target >= stages.length) return;
    const next = [...stages];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  return (
    <section className="mt-6 pt-6 border-t border-white/10">
      <div className="mb-3">
        <h2 className="text-sm font-semibold text-white">정렬·순위 단계</h2>
        <p className="text-xs text-slate-500 mt-1">
          위 검색식으로 걸러낸 뒤 순서대로 실행됩니다. 정렬 단계로 상위 N개만 남기거나, 조건 단계로 더 좁힐 수 있습니다.
        </p>
      </div>

      {stages.length === 0 ? (
        <div className="border border-dashed border-white/10 rounded-xl px-4 py-8 text-center text-slate-600 text-sm">
          단계가 없습니다. 아래 버튼으로 정렬 단계나 조건 단계를 추가하세요.
        </div>
      ) : (
        <div className="space-y-3">
          {stages.map((stage, i) => (
            <StageCard
              key={stage.id}
              index={i}
              total={stages.length}
              stage={stage}
              onMove={(dir) => moveStage(i, dir)}
              onRemove={() => removeStage(i)}
              onChange={(next) => updateStage(i, next)}
              names={names}
              models={models}
            />
          ))}
        </div>
      )}

      <div className="flex flex-wrap gap-2 mt-3">
        <button
          type="button"
          onClick={() => onChange([...stages, createRankStage()])}
          className={cx.btnToggleOff}
        >
          + 정렬 단계 추가
        </button>
        <button
          type="button"
          onClick={() => onChange([...stages, createFilterStage()])}
          className={cx.btnToggleOff}
        >
          + 조건 단계 추가
        </button>
      </div>
    </section>
  );
}

// ─── 단계 카드(공통 프레임) ────────────────────────────────────────────────────

function StageCard({
  index,
  total,
  stage,
  onMove,
  onRemove,
  onChange,
  names,
  models,
}: {
  index: number;
  total: number;
  stage: PipelineStageState;
  onMove: (dir: -1 | 1) => void;
  onRemove: () => void;
  onChange: (next: PipelineStageState) => void;
  names?: ConditionNames;
  models: ModelSummary[];
}) {
  const typeLabel = stage.type === "RANK" ? "정렬 단계" : "조건 단계";
  const summary =
    stage.type === "RANK" ? summarizeRankStage(stage.sort, stage.limit, names) : "추가 조건 그룹";

  return (
    <div className="rounded-xl border border-white/10 bg-slate-800/50">
      <div className="flex items-center gap-2 px-3 py-2 border-b border-white/5">
        <span className="flex items-center justify-center w-6 h-6 rounded-md bg-white/5 text-xs text-slate-300 font-semibold flex-shrink-0">
          {index + 1}
        </span>
        <span className="text-xs font-medium text-indigo-300 flex-shrink-0">{typeLabel}</span>
        <span className="text-xs text-slate-500 truncate flex-1">{summary}</span>
        <div className="flex items-center gap-1 flex-shrink-0">
          <button
            type="button"
            onClick={() => onMove(-1)}
            disabled={index === 0}
            title="위로"
            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-white/10 transition disabled:opacity-30 disabled:hover:bg-transparent"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m18 15-6-6-6 6" />
            </svg>
          </button>
          <button
            type="button"
            onClick={() => onMove(1)}
            disabled={index === total - 1}
            title="아래로"
            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-white/10 transition disabled:opacity-30 disabled:hover:bg-transparent"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>
          <button
            type="button"
            onClick={onRemove}
            title="단계 삭제"
            className="p-1.5 rounded text-slate-400 hover:text-red-400 hover:bg-red-900/20 transition"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div className="p-3">
        {stage.type === "RANK" ? (
          <RankStageEditor
            sort={stage.sort}
            limit={stage.limit ?? null}
            models={models}
            onChange={(sort, limit) => onChange({ ...stage, sort, limit })}
          />
        ) : (
          <FilterStageEditor
            expression={stage.expression}
            onChange={(expression) => onChange({ ...stage, expression })}
            names={names}
          />
        )}
      </div>
    </div>
  );
}

// ─── RANK 단계 편집기 ──────────────────────────────────────────────────────────

const FIELD_ITEMS = SORT_FIELDS.map((f) => ({ value: f as string, label: SORT_FIELD_LABELS[f] }));

function RankStageEditor({
  sort,
  limit,
  models,
  onChange,
}: {
  sort: SortKey[];
  limit: number | null;
  models: ModelSummary[];
  onChange: (sort: SortKey[], limit: number | null) => void;
}) {
  const modelItems = models.map((m) => ({ value: String(m.id), label: `${m.name} (v${m.version})` }));

  function updateKey(index: number, next: SortKey) {
    onChange(sort.map((k, i) => (i === index ? next : k)), limit);
  }

  // 정렬 필드 변경: MODEL_SCORE로 바꾸면 첫 모델을 기본 선택, 다른 필드로 바꾸면 modelId 제거.
  function changeField(index: number, field: SortField) {
    const key = sort[index];
    if (field === "MODEL_SCORE") {
      updateKey(index, { field, direction: key.direction, modelId: key.modelId ?? models[0]?.id });
    } else {
      updateKey(index, { field, direction: key.direction });
    }
  }

  function removeKey(index: number) {
    onChange(sort.filter((_, i) => i !== index), limit);
  }

  function moveKey(index: number, dir: -1 | 1) {
    const target = index + dir;
    if (target < 0 || target >= sort.length) return;
    const next = [...sort];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next, limit);
  }

  function addKey() {
    onChange([...sort, { field: "CHANGE_RATE", direction: "DESC" }], limit);
  }

  function setLimit(raw: string) {
    const trimmed = raw.trim();
    if (trimmed === "") { onChange(sort, null); return; }
    const n = parseInt(trimmed, 10);
    onChange(sort, Number.isNaN(n) || n < 1 ? null : n);
  }

  return (
    <div className="space-y-3">
      <div>
        <p className="text-xs text-slate-400 mb-1.5">정렬 키 (위에서부터 우선순위)</p>
        {sort.length === 0 ? (
          <p className="text-xs text-slate-600 py-2">정렬 키를 추가하세요.</p>
        ) : (
          <div className="space-y-2">
            {sort.map((key, i) => (
              <div key={i} className="flex flex-wrap items-center gap-2">
                <span className="text-xs text-slate-500 w-4 flex-shrink-0">{i + 1}</span>
                <Select
                  value={key.field}
                  items={FIELD_ITEMS}
                  onChange={(v) => changeField(i, v as SortField)}
                  className="w-36"
                />
                {key.field === "MODEL_SCORE" && (
                  models.length === 0 ? (
                    <span className="text-xs text-amber-400">활성 모델 없음 — 관리자에게 문의</span>
                  ) : (
                    <>
                      <Select
                        value={key.modelId != null ? String(key.modelId) : null}
                        items={modelItems}
                        placeholder="모델 선택"
                        onChange={(v) => updateKey(i, { ...key, modelId: Number(v) })}
                        className="w-44"
                      />
                      {key.modelId == null && (
                        <span className="text-xs text-amber-400">모델을 선택하세요</span>
                      )}
                    </>
                  )
                )}
                <div className="flex gap-1">
                  {(["DESC", "ASC"] as SortDirection[]).map((d) => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => updateKey(i, { ...key, direction: d })}
                      className={key.direction === d ? cx.btnToggleOn : cx.btnToggleOff}
                    >
                      {SORT_DIRECTION_LABELS[d]}
                    </button>
                  ))}
                </div>
                <div className="flex items-center gap-1 ml-auto">
                  <button
                    type="button"
                    onClick={() => moveKey(i, -1)}
                    disabled={i === 0}
                    title="위로"
                    className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-white/10 transition disabled:opacity-30 disabled:hover:bg-transparent"
                  >
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="m18 15-6-6-6 6" />
                    </svg>
                  </button>
                  <button
                    type="button"
                    onClick={() => moveKey(i, 1)}
                    disabled={i === sort.length - 1}
                    title="아래로"
                    className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-white/10 transition disabled:opacity-30 disabled:hover:bg-transparent"
                  >
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="m6 9 6 6 6-6" />
                    </svg>
                  </button>
                  <button
                    type="button"
                    onClick={() => removeKey(i)}
                    title="정렬 키 삭제"
                    className="p-1.5 rounded text-slate-400 hover:text-red-400 hover:bg-red-900/20 transition"
                  >
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M18 6 6 18M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
        <button
          type="button"
          onClick={addKey}
          className="mt-2 px-2.5 py-1 rounded-lg text-xs text-slate-400 border border-white/10 hover:text-white hover:border-white/25 transition"
        >
          + 정렬 키 추가
        </button>
      </div>

      <div className="flex items-center gap-2">
        <label className="text-xs text-slate-400 flex-shrink-0">상위 N</label>
        <input
          type="number"
          min={1}
          value={limit ?? ""}
          onChange={(e) => setLimit(e.target.value)}
          placeholder="제한 없음"
          className={`${cx.inputNumber} w-32`}
        />
        <span className="text-xs text-slate-500">개만 남김 (비우면 정렬만)</span>
      </div>

      <p className="text-xs text-slate-500">시가총액은 밸류에이션 데이터가 있는 거래일에만 정렬됩니다.</p>
    </div>
  );
}

// ─── FILTER 단계 편집기(기존 조건 빌더 재사용) ─────────────────────────────────

function FilterStageEditor({
  expression,
  onChange,
  names,
}: {
  expression: GroupNode;
  onChange: (expression: GroupNode) => void;
  names?: ConditionNames;
}) {
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(expression.id);
  const [pendingAddType, setPendingAddType] = useState<ConditionType | null>(null);

  return (
    <div className="flex flex-col md:flex-row gap-3">
      <div className="flex-1 min-w-0">
        <ExpressionTree
          root={expression}
          onChange={onChange}
          selectedGroupId={selectedGroupId}
          onSelectGroup={setSelectedGroupId}
          pendingAddType={pendingAddType}
          onPendingAddConsumed={() => setPendingAddType(null)}
          names={names}
        />
      </div>
      <div className="md:w-56 flex-shrink-0 h-80 md:h-96 border border-white/10 rounded-lg overflow-hidden">
        <ConditionPalette
          selectedGroupId={selectedGroupId}
          rootId={expression.id}
          onAdd={(type) => setPendingAddType(type)}
        />
      </div>
    </div>
  );
}
