"use client";

import {
  ExpressionNode,
  GroupNode,
  ConditionNode,
  ConditionType,
  LogicOperator,
  DateRule,
  ConditionNames,
} from "@/types/filter";
import {
  summarizeCondition,
  generateId,
  addNodeToGroup,
  removeNode,
  updateNode,
  updateChildOp,
} from "@/utils/filter";
import { useState, useEffect } from "react";
import ConditionEditorModal from "./ConditionEditorModal";

interface Props {
  root: GroupNode;
  onChange: (root: GroupNode) => void;
  selectedGroupId: string | null;
  onSelectGroup: (id: string) => void;
  pendingAddType: ConditionType | null;
  onPendingAddConsumed: () => void;
  dateRule?: DateRule;
  names?: ConditionNames;
}

export default function ExpressionTree({
  root,
  onChange,
  selectedGroupId,
  onSelectGroup,
  pendingAddType,
  onPendingAddConsumed,
  dateRule = "LATEST",
  names,
}: Props) {
  const [editingCondition, setEditingCondition] = useState<{ node: ConditionNode } | null>(null);
  const [addingToGroup, setAddingToGroup] = useState<{ groupId: string; type: ConditionType } | null>(null);

  useEffect(() => {
    if (!selectedGroupId) onSelectGroup(root.id);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!pendingAddType || !selectedGroupId) return;
    setAddingToGroup({ groupId: selectedGroupId, type: pendingAddType });
    onPendingAddConsumed();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingAddType]);

  function handleAddGroup(groupId: string) {
    const newGroup: GroupNode = { id: generateId(), nodeType: "GROUP", negated: false, children: [], childOps: [] };
    onChange(addNodeToGroup(root, groupId, newGroup));
  }

  function handleRemove(nodeId: string) {
    onChange(removeNode(root, nodeId));
  }

  function handleToggleNegated(nodeId: string) {
    onChange(
      updateNode(root, nodeId, (n) => ({ ...n, negated: !n.negated })) as GroupNode
    );
  }

  function handleUpdateChildOp(groupId: string, opIndex: number, current: LogicOperator) {
    const next: LogicOperator = current === "AND" ? "OR" : "AND";
    onChange(updateChildOp(root, groupId, opIndex, next));
  }

  function handleConfirmAdd(node: ConditionNode) {
    if (!addingToGroup) return;
    onChange(addNodeToGroup(root, addingToGroup.groupId, node));
    setAddingToGroup(null);
  }

  function handleConfirmEdit(node: ConditionNode) {
    onChange(updateNode(root, node.id, () => node) as GroupNode);
    setEditingCondition(null);
  }

  return (
    <>
      {/* 루트 컨트롤 바 */}
      <div className="flex items-center gap-2 mb-3">
        <NotBadge
          negated={root.negated}
          onClick={() => handleToggleNegated(root.id)}
        />
        <button
          onClick={() => onSelectGroup(root.id)}
          className={`px-2.5 py-1 rounded-lg text-xs transition border ${
            selectedGroupId === root.id
              ? "text-indigo-300 border-indigo-500/40 bg-indigo-600/15"
              : "text-slate-400 border-white/10 hover:text-indigo-300 hover:border-indigo-500/30"
          }`}
        >
          + 조건
        </button>
        <button
          onClick={() => handleAddGroup(root.id)}
          className="px-2.5 py-1 rounded-lg text-xs text-slate-400 border border-white/10 hover:text-white hover:border-white/25 transition"
        >
          + 그룹
        </button>
        {selectedGroupId === root.id && (
          <span className="text-xs text-indigo-400 font-medium">← 루트에 추가</span>
        )}
      </div>

      {/* 루트의 자식들 */}
      <div
        className="space-y-2 min-h-12 rounded-xl p-1 cursor-default"
        onClick={() => onSelectGroup(root.id)}
      >
        {root.children.length === 0 && (
          <div className="border border-dashed border-white/10 rounded-xl px-4 py-10 text-center text-slate-600 text-sm">
            오른쪽 패널에서 조건을 추가하거나 그룹을 만들어보세요
          </div>
        )}
        {root.children.map((child, i) => (
          <div key={child.id}>
            {i > 0 && (
              <ChildOpBadge
                op={(root.childOps ?? [])[i - 1] ?? "AND"}
                onClick={(e) => {
                  e.stopPropagation();
                  handleUpdateChildOp(root.id, i - 1, (root.childOps ?? [])[i - 1] ?? "AND");
                }}
              />
            )}
            {child.nodeType === "GROUP" ? (
              <GroupNodeView
                node={child as GroupNode}
                selectedGroupId={selectedGroupId}
                onSelectGroup={onSelectGroup}
                onAddGroup={handleAddGroup}
                onRemove={handleRemove}
                onToggleNegated={handleToggleNegated}
                onUpdateChildOp={handleUpdateChildOp}
                onEditCondition={(n) => setEditingCondition({ node: n })}
                onAddCondition={(groupId, type) => setAddingToGroup({ groupId, type })}
                dateRule={dateRule}
                names={names}
              />
            ) : (
              <ConditionNodeView
                node={child as ConditionNode}
                onRemove={handleRemove}
                onEdit={(n) => setEditingCondition({ node: n })}
                onToggleNegated={handleToggleNegated}
                dateRule={dateRule}
                names={names}
              />
            )}
          </div>
        ))}
      </div>

      {addingToGroup && (
        <ConditionEditorModal
          conditionType={addingToGroup.type}
          onConfirm={handleConfirmAdd}
          onClose={() => setAddingToGroup(null)}
        />
      )}
      {editingCondition && (
        <ConditionEditorModal
          conditionType={editingCondition.node.conditionType}
          initial={editingCondition.node}
          onConfirm={handleConfirmEdit}
          onClose={() => setEditingCondition(null)}
        />
      )}
    </>
  );
}

// ─── NOT Badge ────────────────────────────────────────────────────────────────

function NotBadge({
  negated,
  onClick,
}: {
  negated: boolean;
  onClick: (e: React.MouseEvent) => void;
}) {
  return (
    <button
      onClick={onClick}
      title="NOT 토글 — 이 노드의 결과를 반전"
      className={`px-2 py-0.5 rounded-md text-xs font-bold tracking-wide transition border ${
        negated
          ? "bg-rose-700/70 text-rose-100 border-rose-600/50 hover:bg-rose-600"
          : "bg-transparent text-slate-600 border-white/10 hover:text-slate-300 hover:border-white/25"
      }`}
    >
      NOT
    </button>
  );
}

// ─── Group Node ───────────────────────────────────────────────────────────────

function GroupNodeView({
  node,
  selectedGroupId,
  onSelectGroup,
  onAddGroup,
  onRemove,
  onToggleNegated,
  onUpdateChildOp,
  onEditCondition,
  onAddCondition,
  dateRule,
  names,
}: {
  node: GroupNode;
  selectedGroupId: string | null;
  onSelectGroup: (id: string) => void;
  onAddGroup: (groupId: string) => void;
  onRemove: (id: string) => void;
  onToggleNegated: (id: string) => void;
  onUpdateChildOp: (groupId: string, opIndex: number, current: LogicOperator) => void;
  onEditCondition: (n: ConditionNode) => void;
  onAddCondition: (groupId: string, type: ConditionType) => void;
  dateRule: DateRule;
  names?: ConditionNames;
}) {
  const isSelected = selectedGroupId === node.id;

  return (
    <div
      className={`rounded-xl border transition-all ${
        isSelected ? "border-indigo-500/60 bg-slate-800/50" : "border-white/10 bg-slate-800/50"
      }`}
      onClick={(e) => { e.stopPropagation(); onSelectGroup(node.id); }}
    >
      <div className="flex items-center gap-2 px-3 py-2 select-none">
        <NotBadge
          negated={node.negated}
          onClick={(e) => { e.stopPropagation(); onToggleNegated(node.id); }}
        />
        <span className="text-xs text-slate-400 flex-1">
          그룹{node.children.length > 0 && ` · ${node.children.length}개`}
        </span>
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => onSelectGroup(node.id)}
            className={`px-2 py-0.5 rounded text-xs transition ${
              isSelected
                ? "text-indigo-300 bg-indigo-600/20 hover:bg-indigo-600/30"
                : "text-slate-500 hover:text-indigo-300 hover:bg-indigo-600/15"
            }`}
          >
            + 조건
          </button>
          <button
            onClick={() => onAddGroup(node.id)}
            className="px-2 py-0.5 rounded text-xs text-slate-500 hover:text-white hover:bg-white/8 transition"
          >
            + 그룹
          </button>
          <button
            onClick={() => onRemove(node.id)}
            className="p-1 rounded text-slate-500 hover:text-red-400 hover:bg-red-900/20 transition"
          >
            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div className="px-3 pb-3 space-y-1">
        {node.children.map((child, i) => (
          <div key={child.id}>
            {i > 0 && (
              <ChildOpBadge
                op={(node.childOps ?? [])[i - 1] ?? "AND"}
                onClick={(e) => {
                  e.stopPropagation();
                  onUpdateChildOp(node.id, i - 1, (node.childOps ?? [])[i - 1] ?? "AND");
                }}
              />
            )}
            {child.nodeType === "GROUP" ? (
              <GroupNodeView
                node={child as GroupNode}
                selectedGroupId={selectedGroupId}
                onSelectGroup={onSelectGroup}
                onAddGroup={onAddGroup}
                onRemove={onRemove}
                onToggleNegated={onToggleNegated}
                onUpdateChildOp={onUpdateChildOp}
                onEditCondition={onEditCondition}
                onAddCondition={onAddCondition}
                dateRule={dateRule}
                names={names}
              />
            ) : (
              <ConditionNodeView
                node={child as ConditionNode}
                onRemove={onRemove}
                onEdit={onEditCondition}
                onToggleNegated={onToggleNegated}
                dateRule={dateRule}
                names={names}
              />
            )}
          </div>
        ))}
        {node.children.length === 0 && (
          <p className="text-xs text-slate-600 py-2 text-center">
            오른쪽 패널에서 조건을 추가하세요
          </p>
        )}
      </div>
    </div>
  );
}

// ─── Child Op Badge ───────────────────────────────────────────────────────────

function ChildOpBadge({
  op,
  onClick,
}: {
  op: LogicOperator;
  onClick: (e: React.MouseEvent) => void;
}) {
  return (
    <div className="flex items-center gap-2 py-1 pl-1">
      <button
        onClick={onClick}
        title="클릭해서 AND ↔ OR 전환"
        className={`px-2 py-0.5 rounded text-xs font-bold tracking-wide transition ${
          op === "OR"
            ? "bg-amber-600/40 text-amber-300 hover:bg-amber-500/60"
            : "bg-indigo-600/40 text-indigo-300 hover:bg-white/30"
        }`}
      >
        {op}
      </button>
      <span className="flex-1 h-px bg-white/5" />
    </div>
  );
}

// ─── Condition Node ───────────────────────────────────────────────────────────

const CONDITION_TYPE_ICONS: Record<ConditionType, string> = {
  INDICATOR_VALUE: "📊",
  INDICATOR_RANGE: "↔",
  INDICATOR_CROSS: "⚡",
  PRICE_VALUE: "💰",
  PRICE_RANGE: "📏",
  PRICE_VS_INDICATOR: "📐",
  VOLUME_VALUE: "📈",
  VOLUME_RANGE: "📉",
  TURNOVER_VALUE: "💵",
  TURNOVER_RANGE: "💵",
  MARKET_FILTER: "🏢",
  MODEL_SCORE_VALUE: "🤖",
  MODEL_SCORE_RANGE: "🎯",
  CUSTOM_METRIC_VALUE: "🧮",
  CUSTOM_METRIC_RANGE: "🧮",
  RANK_VALUE: "🏆",
  RANK_RANGE: "📊",
  STOCK_STATUS: "🚫",
};

function ConditionNodeView({
  node,
  onRemove,
  onEdit,
  onToggleNegated,
  dateRule,
  names,
}: {
  node: ConditionNode;
  onRemove: (id: string) => void;
  onEdit: (n: ConditionNode) => void;
  onToggleNegated: (id: string) => void;
  dateRule: DateRule;
  names?: ConditionNames;
}) {
  // 종목상태는 현재값이라 최신일자에서만 유효 — 과거일자 기준이면 무시됨을 반투명으로 표시.
  const inactive = node.conditionType === "STOCK_STATUS" && dateRule !== "LATEST";

  return (
    <div
      className={`flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-700/50 border border-white/8 group${inactive ? " opacity-50" : ""}`}
      title={inactive ? "현재 상태 기준 — 최신일자에서만 적용됩니다" : undefined}
      onClick={(e) => e.stopPropagation()}
    >
      <NotBadge
        negated={node.negated}
        onClick={() => onToggleNegated(node.id)}
      />
      <span className="text-sm leading-none flex-shrink-0">{CONDITION_TYPE_ICONS[node.conditionType]}</span>
      <span className="flex-1 text-sm text-slate-200 font-mono truncate">
        {summarizeCondition(node, names)}
        {inactive && <span className="ml-2 not-italic text-xs text-amber-400/80 font-sans">(최신일자에서만 적용)</span>}
      </span>
      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition">
        <button
          onClick={() => onEdit(node)}
          className="p-1 rounded text-slate-400 hover:text-white hover:bg-white/10 transition"
        >
          <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <button
          onClick={() => onRemove(node.id)}
          className="p-1 rounded text-slate-400 hover:text-red-400 hover:bg-red-900/20 transition"
        >
          <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  );
}
