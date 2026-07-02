import {
  GroupNode,
  ExpressionNode,
  ConditionNode,
  LogicOperator,
  INDICATOR_LABELS,
  COMPARE_OP_LABELS,
  PRICE_FIELD_LABELS,
  RANK_TYPE_LABELS,
  STOCK_STATUS_LABELS,
} from "@/types/filter";

/** 오프셋 표기: 0/미지정→없음, 음수→"(N일전)", 양수→"(N일후)". */
function offsetTag(offset?: number): string {
  if (!offset) return "";
  return offset < 0 ? `(${-offset}일전)` : `(${offset}일후)`;
}

export function summarizeCondition(node: ConditionNode): string {
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
    case "MARKET_FILTER":
      return `시장: ${node.markets.join(", ")}`;
    case "MODEL_SCORE_VALUE":
      return `모델#${node.modelId} 점수${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "MODEL_SCORE_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} 모델#${node.modelId} 점수${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "RANK_VALUE":
      return `${RANK_TYPE_LABELS[node.rank]}${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "RANK_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} ${RANK_TYPE_LABELS[node.rank]}${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
    }
    case "BREADTH_VALUE":
      return `당일 상승비율${offsetTag(node.offset)} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "BREADTH_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} 당일 상승비율${offsetTag(node.offset)} ${hi} ${node.maxValue}`;
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
