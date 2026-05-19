"use client";

import { useEffect, useState } from "react";
import {
  DndContext, closestCenter, PointerSensor, useSensor, useSensors, type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext, useSortable, verticalListSortingStrategy, arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { PANEL_LABELS, type PanelId } from "@/components/chart/indicatorMeta";

function SortablePanelItem({ panelId, index }: { panelId: PanelId; index: number }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: panelId });
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.4 : 1 }}
      className="flex items-center gap-3 px-3 py-3 bg-slate-800/60 rounded-lg border border-white/8"
    >
      <span className="text-xs text-slate-600 w-4 text-right flex-shrink-0">{index + 1}</span>
      <button
        className="text-slate-500 hover:text-slate-300 cursor-grab active:cursor-grabbing flex-shrink-0"
        {...attributes} {...listeners}
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <rect x="4" y="6"  width="16" height="2" rx="1"/>
          <rect x="4" y="11" width="16" height="2" rx="1"/>
          <rect x="4" y="16" width="16" height="2" rx="1"/>
        </svg>
      </button>
      <span className="text-sm text-slate-200 flex-1">{PANEL_LABELS[panelId]}</span>
    </div>
  );
}

interface Props {
  open: boolean;
  onClose: () => void;
  activePanelIds: PanelId[];
  onReorder: (newOrder: PanelId[]) => Promise<void>;
}

export default function PanelOrderModal({ open, onClose, activePanelIds, onReorder }: Props) {
  const [order, setOrder] = useState<PanelId[]>(activePanelIds);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) setOrder(activePanelIds);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  if (!open) return null;

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const prev = order;
    const next = arrayMove(order, order.indexOf(active.id as PanelId), order.indexOf(over.id as PanelId));
    setOrder(next);
    setSaving(true);
    try {
      await onReorder(next);
    } catch {
      setOrder(prev);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4"
      onMouseDown={onClose}
    >
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <div
        className="relative w-full sm:max-w-xs bg-slate-800 sm:rounded-2xl rounded-t-2xl border border-white/10 shadow-2xl"
        onMouseDown={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-4 border-b border-white/10">
          <div className="flex items-center gap-2">
            <span className="text-base text-white font-semibold">보조지표 순서 변경</span>
            {saving && <span className="text-xs text-slate-500">저장 중...</span>}
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white transition p-2 rounded-lg hover:bg-white/8">
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="p-4">
          {order.length === 0 ? (
            <p className="text-sm text-slate-500 text-center py-6">활성화된 서브패널이 없습니다.</p>
          ) : (
            <>
              <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                <SortableContext items={order} strategy={verticalListSortingStrategy}>
                  <div className="space-y-2">
                    {order.map((panelId, i) => (
                      <SortablePanelItem key={panelId} panelId={panelId} index={i} />
                    ))}
                  </div>
                </SortableContext>
              </DndContext>
              <p className="text-xs text-slate-600 text-center mt-3">드래그로 순서 변경 · 자동 저장</p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

