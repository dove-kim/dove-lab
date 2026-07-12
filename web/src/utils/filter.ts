import {
  GroupNode,
  ExpressionNode,
  ConditionNode,
  LogicOperator,
  SortField,
  SortDirection,
  SortKey,
  PipelineStage,
  PipelineStageState,
  SORT_FIELDS,
  SORT_FIELD_LABELS,
  SORT_DIRECTION_LABELS,
  INDICATOR_LABELS,
  COMPARE_OP_LABELS,
  PRICE_FIELD_LABELS,
  RANK_TYPE_LABELS,
  STOCK_STATUS_LABELS,
  ConditionNames,
} from "@/types/filter";

/** 오프셋 표기: 0/미지정→없음, 음수→"(N일전)", 양수→"(N일후)". */
function offsetTag(offset?: number): string {
  if (!offset) return "";
  return offset < 0 ? `(${-offset}일전)` : `(${offset}일후)`;
}

/**
 * 조건 노드를 사람이 읽는 한 줄 요약으로 만든다. names가 주어지면 커스텀 지표·모델을 이름으로, 없으면 #id로 표시한다.
 */
export function summarizeCondition(node: ConditionNode, names?: ConditionNames): string {
  switch (node.conditionType) {
    case "INDICATOR_VALUE":
      return `${INDICATOR_LABELS[node.indicator]}${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "INDICATOR_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} ${INDICATOR_LABELS[node.indicator]}${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "INDICATOR_CROSS":
      return `${INDICATOR_LABELS[node.leftIndicator]}${offsetTag(node.leftOffset)} ${COMPARE_OP_LABELS[node.operator]} ${INDICATOR_LABELS[node.rightIndicator]}${offsetTag(node.rightOffset)}`;
    case "PRICE_VALUE":
      return `${PRICE_FIELD_LABELS[node.priceField]}${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value.toLocaleString()}`;
    case "PRICE_VS_INDICATOR":
      return `${PRICE_FIELD_LABELS[node.priceField]}${offsetTag(node.leftOffset)} ${COMPARE_OP_LABELS[node.operator]} ${INDICATOR_LABELS[node.indicator]}${offsetTag(node.rightOffset)}`;
    case "PRICE_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue.toLocaleString()} ${lo} ${PRICE_FIELD_LABELS[node.priceField]}${offsetTag(node.offset)} ${hi} ${node.maxValue.toLocaleString()}`;
    }
    case "VOLUME_VALUE":
      return `거래량${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value.toLocaleString()}`;
    case "VOLUME_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue.toLocaleString()} ${lo} 거래량${offsetTag(node.offset)} ${hi} ${node.maxValue.toLocaleString()}`;
    }
    case "TURNOVER_VALUE":
      return `거래대금${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value.toLocaleString()}`;
    case "TURNOVER_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue.toLocaleString()} ${lo} 거래대금${offsetTag(node.offset)} ${hi} ${node.maxValue.toLocaleString()}`;
    }
    case "MARKET_FILTER":
      return `시장: ${node.markets.join(", ")}`;
    case "MODEL_SCORE_VALUE": {
      const model = names?.models?.[node.modelId] ?? `모델#${node.modelId}`;
      return `${model} 점수${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    }
    case "MODEL_SCORE_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      const model = names?.models?.[node.modelId] ?? `모델#${node.modelId}`;
      return `${node.minValue} ${lo} ${model} 점수${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "CUSTOM_METRIC_VALUE": {
      const metric = names?.metrics?.[node.metricId] ?? `지표#${node.metricId}`;
      return `${metric}${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    }
    case "CUSTOM_METRIC_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      const metric = names?.metrics?.[node.metricId] ?? `지표#${node.metricId}`;
      return `${node.minValue} ${lo} ${metric}${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "RANK_VALUE":
      return `${RANK_TYPE_LABELS[node.rank]}${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "RANK_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} ${RANK_TYPE_LABELS[node.rank]}${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "STOCK_STATUS": {
      const targets = (node.exclude.length > 0 ? node.exclude : (["TRADING_HALT", "ADMIN_ITEM"] as const))
        .map((s) => STOCK_STATUS_LABELS[s]);
      return `종목상태: ${targets.join("·")} 제외`;
    }
  }
}

export function generateId(): string {
  return Math.random().toString(36).slice(2, 11);
}

export function createEmptyRoot(): GroupNode {
  return { id: generateId(), nodeType: "GROUP", negated: false, children: [], childOps: [] };
}

/**
 * 백엔드 식(@JsonRawValue라 JSON 객체로 내려옴, 또는 문자열)을 GroupNode로 파싱한다. 실패 시 null.
 */
export function parseExpression(expression: unknown): GroupNode | null {
  if (!expression) return null;
  if (typeof expression === "object") return expression as GroupNode;
  try { return JSON.parse(expression as string) as GroupNode; }
  catch { return null; }
}

export function addNodeToGroup(root: GroupNode, groupId: string, node: ExpressionNode): GroupNode {
  if (root.id === groupId) {
    const newChildOps = root.children.length > 0
      ? [...(root.childOps ?? []), "AND" as LogicOperator]
      : [...(root.childOps ?? [])];
    return { ...root, children: [...root.children, node], childOps: newChildOps };
  }
  return {
    ...root,
    children: root.children.map((c) =>
      c.nodeType === "GROUP" ? addNodeToGroup(c, groupId, node) : c
    ),
  };
}

export function removeNode(root: GroupNode, nodeId: string): GroupNode {
  const idx = root.children.findIndex((c) => c.id === nodeId);
  if (idx !== -1) {
    const newChildren = root.children.filter((c) => c.id !== nodeId);
    const ops = [...(root.childOps ?? [])];
    if (idx === 0 && ops.length > 0) ops.splice(0, 1);
    else if (idx > 0) ops.splice(idx - 1, 1);
    return { ...root, children: newChildren, childOps: ops };
  }
  return {
    ...root,
    children: root.children.map((c) => (c.nodeType === "GROUP" ? removeNode(c, nodeId) : c)),
  };
}

export function updateChildOp(
  root: GroupNode,
  groupId: string,
  opIndex: number,
  op: LogicOperator
): GroupNode {
  if (root.id === groupId) {
    const ops = [...(root.childOps ?? [])];
    ops[opIndex] = op;
    return { ...root, childOps: ops };
  }
  return {
    ...root,
    children: root.children.map((c) =>
      c.nodeType === "GROUP" ? updateChildOp(c, groupId, opIndex, op) : c
    ),
  };
}

export function updateNode(
  root: GroupNode,
  nodeId: string,
  updater: (n: ExpressionNode) => ExpressionNode
): GroupNode {
  if (root.id === nodeId) return updater(root) as GroupNode;
  return {
    ...root,
    children: root.children.map((c) => {
      if (c.id === nodeId) return updater(c);
      if (c.nodeType === "GROUP") return updateNode(c, nodeId, updater);
      return c;
    }),
  };
}

// ─── 순서 파이프라인(정렬·순위 단계) ──────────────────────────────────────────

const SORT_DIRECTIONS: SortDirection[] = ["ASC", "DESC"];

export function createRankStage(): PipelineStageState {
  return { id: generateId(), type: "RANK", sort: [{ field: "MARKET_CAP", direction: "DESC" }], limit: 100 };
}

export function createFilterStage(): PipelineStageState {
  return { id: generateId(), type: "FILTER", expression: createEmptyRoot() };
}

function isSortField(v: unknown): v is SortField {
  return typeof v === "string" && (SORT_FIELDS as string[]).includes(v);
}

function isSortDirection(v: unknown): v is SortDirection {
  return typeof v === "string" && (SORT_DIRECTIONS as string[]).includes(v);
}

function parseSortKey(raw: unknown): SortKey | null {
  if (!raw || typeof raw !== "object") return null;
  const r = raw as Record<string, unknown>;
  if (!isSortField(r.field) || !isSortDirection(r.direction)) return null;
  return { field: r.field, direction: r.direction };
}

/**
 * 백엔드 파이프라인(문자열화된 JSON 배열 또는 배열/null)을 편집기 단계 상태 목록으로 파싱한다.
 * 없거나 파싱 실패 시 빈 목록.
 */
export function parsePipeline(pipeline: unknown): PipelineStageState[] {
  if (pipeline == null) return [];
  let array: unknown = pipeline;
  if (typeof pipeline === "string") {
    if (pipeline.trim() === "") return [];
    try { array = JSON.parse(pipeline); }
    catch { return []; }
  }
  if (!Array.isArray(array)) return [];

  const stages: PipelineStageState[] = [];
  for (const raw of array) {
    if (!raw || typeof raw !== "object") continue;
    const r = raw as Record<string, unknown>;
    if (r.type === "RANK") {
      const sort = Array.isArray(r.sort)
        ? r.sort.map(parseSortKey).filter((k): k is SortKey => k !== null)
        : [];
      const limit = typeof r.limit === "number" ? r.limit : null;
      stages.push({ id: generateId(), type: "RANK", sort, limit });
    } else if (r.type === "FILTER") {
      const expression = parseExpression(r.expression);
      if (expression) stages.push({ id: generateId(), type: "FILTER", expression });
    }
  }
  return stages;
}

/**
 * 편집기 단계 상태 목록을 백엔드 계약 JSON 문자열로 직렬화한다. 비어 있으면 null(단순 필터).
 */
export function serializePipeline(stages: PipelineStageState[]): string | null {
  if (stages.length === 0) return null;
  const clean: PipelineStage[] = stages.map((s) =>
    s.type === "RANK"
      ? { type: "RANK", sort: s.sort, limit: s.limit ?? null }
      : { type: "FILTER", expression: s.expression }
  );
  return JSON.stringify(clean);
}

/**
 * RANK 단계를 "시총 내림 · 상위 100" 식 한 줄로 요약한다.
 */
export function summarizeRankStage(sort: SortKey[], limit?: number | null): string {
  const keys = sort.length > 0
    ? sort.map((k) => `${SORT_FIELD_LABELS[k.field]} ${SORT_DIRECTION_LABELS[k.direction]}`).join(", ")
    : "정렬 없음";
  return limit != null ? `${keys} · 상위 ${limit}` : keys;
}
