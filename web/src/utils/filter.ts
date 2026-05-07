import {
  GroupNode,
  ExpressionNode,
  ConditionNode,
  LogicOperator,
  INDICATOR_LABELS,
  COMPARE_OP_LABELS,
  PRICE_FIELD_LABELS,
} from "@/types/filter";

export function summarizeCondition(node: ConditionNode): string {
  switch (node.conditionType) {
    case "INDICATOR_VALUE":
      return `${INDICATOR_LABELS[node.indicator]} ${COMPARE_OP_LABELS[node.operator]} ${node.value}`;
    case "INDICATOR_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue} ${lo} ${INDICATOR_LABELS[node.indicator]} ${hi} ${node.maxValue}`;
    }
    case "INDICATOR_CROSS":
      return `${INDICATOR_LABELS[node.leftIndicator]} ${COMPARE_OP_LABELS[node.operator]} ${INDICATOR_LABELS[node.rightIndicator]}`;
    case "PRICE_VALUE":
      return `${PRICE_FIELD_LABELS[node.priceField]} ${COMPARE_OP_LABELS[node.operator]} ${node.value.toLocaleString()}`;
    case "PRICE_VS_INDICATOR":
      return `${PRICE_FIELD_LABELS[node.priceField]} ${COMPARE_OP_LABELS[node.operator]} ${INDICATOR_LABELS[node.indicator]}`;
    case "PRICE_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue.toLocaleString()} ${lo} ${PRICE_FIELD_LABELS[node.priceField]} ${hi} ${node.maxValue.toLocaleString()}`;
    }
    case "VOLUME_VALUE":
      return `거래량 ${COMPARE_OP_LABELS[node.operator]} ${node.value.toLocaleString()}`;
    case "VOLUME_RANGE": {
      const lo = node.minInclusive ? "≤" : "<";
      const hi = node.maxInclusive ? "≤" : "<";
      return `${node.minValue.toLocaleString()} ${lo} 거래량 ${hi} ${node.maxValue.toLocaleString()}`;
    }
    case "MARKET_FILTER":
      return `시장: ${node.markets.join(", ")}`;
  }
}

export function generateId(): string {
  return Math.random().toString(36).slice(2, 11);
}

export function createEmptyRoot(): GroupNode {
  return { id: generateId(), nodeType: "GROUP", negated: false, children: [], childOps: [] };
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
