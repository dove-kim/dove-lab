"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  DndContext, closestCenter, PointerSensor, useSensor, useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext, useSortable, verticalListSortingStrategy, arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { INDICATOR_META, PANEL_LABELS, SELECTOR_GROUPS, type PanelId } from "@/components/chart/indicatorMeta";
import type { IndicatorType } from "@/types/filter";
import type { IndicatorPreset, IndicatorPresetItem } from "@/types/indicator-preset";
import { COLOR_PALETTE, LINE_WIDTHS } from "@/types/indicator-preset";

const DEFAULT_PANEL_ORDER: PanelId[] = [
  "RSI", "MACD", "STOCH", "ADX", "OSCILLATOR", "VOLUME_IND", "OBV", "VOLATILITY", "BB_MISC",
];

function buildDefaultItems(): IndicatorPresetItem[] {
  return Object.entries(INDICATOR_META).map(([type, meta]) => ({
    type: type as IndicatorType,
    enabled: false,
    color: meta.color,
    lineWidth: 1.5,
  }));
}

function buildMergedItems(saved: IndicatorPresetItem[]): IndicatorPresetItem[] {
  return buildDefaultItems().map(def => saved.find(it => it.type === def.type) ?? def);
}

interface Props {
  open: boolean;
  onClose: () => void;
  presets: IndicatorPreset[];
  activePreset: IndicatorPreset | null;
  loading: boolean;
  create: (name: string, preset: { items: IndicatorPresetItem[]; panelOrder: PanelId[] }) => Promise<IndicatorPreset>;
  update: (id: number, name: string, preset: { items: IndicatorPresetItem[]; panelOrder: PanelId[] }) => Promise<IndicatorPreset>;
  remove: (id: number) => Promise<void>;
  reorder: (orderedIds: number[]) => Promise<void>;
}

function SortablePresetRow({ preset, isActive }: { preset: IndicatorPreset; isActive: boolean }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: preset.id });
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1 }}
      className={`flex items-center gap-2 px-2 py-1.5 rounded border ${
        isActive ? "border-violet-500/40 bg-violet-500/10" : "border-white/8 bg-slate-800/40"
      }`}
    >
      <button
        className="text-slate-500 hover:text-slate-300 cursor-grab active:cursor-grabbing flex-shrink-0"
        {...attributes} {...listeners}
      >
        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor">
          <rect x="4" y="6"  width="16" height="2" rx="1"/>
          <rect x="4" y="11" width="16" height="2" rx="1"/>
          <rect x="4" y="16" width="16" height="2" rx="1"/>
        </svg>
      </button>
      <span className={`text-sm flex-1 truncate ${isActive ? "text-violet-300" : "text-slate-300"}`}>
        {preset.name}
      </span>
    </div>
  );
}

function ColorPicker({ color, onChange }: { color: string; onChange: (c: string) => void }) {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const btnRef = useRef<HTMLButtonElement>(null);

  function handleToggle() {
    if (!open && btnRef.current) {
      const rect = btnRef.current.getBoundingClientRect();
      const left = Math.min(rect.left, window.innerWidth - 216);
      setPos({ top: rect.bottom + 6, left });
    }
    setOpen(v => !v);
  }

  return (
    <div className="flex-shrink-0">
      <button
        ref={btnRef}
        onClick={handleToggle}
        className="w-6 h-6 rounded-full border-2 border-white/20 hover:border-white/60 transition"
        style={{ backgroundColor: color }}
        title="색상 변경"
      />
      {open && (
        <>
          <div className="fixed inset-0 z-[60]" onClick={() => setOpen(false)} />
          <div
            className="fixed z-[61] grid grid-cols-5 gap-2 p-3 bg-slate-800 rounded-xl border border-white/10 shadow-2xl"
            style={{ top: pos.top, left: pos.left }}
          >
            {COLOR_PALETTE.map(c => (
              <button
                key={c}
                onClick={() => { onChange(c); setOpen(false); }}
                className={`w-8 h-8 rounded-full border-2 transition ${
                  c === color ? "border-white scale-110" : "border-transparent hover:border-white/50"
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function IndicatorRow({
  type, item, indent = false, onChange,
}: {
  type: IndicatorType;
  item: IndicatorPresetItem;
  indent?: boolean;
  onChange: (patch: Partial<IndicatorPresetItem>) => void;
}) {
  const meta = INDICATOR_META[type];
  return (
    <div className={`flex items-center gap-2 py-2 rounded hover:bg-white/3 ${indent ? "pl-10 pr-2" : "px-2"}`}>
      <button
        onClick={() => onChange({ enabled: !item.enabled })}
        className={`w-5 h-5 rounded border flex-shrink-0 flex items-center justify-center transition ${
          item.enabled ? "bg-violet-500 border-violet-500" : "border-white/20"
        }`}
      >
        {item.enabled && (
          <svg className="w-3 h-3 text-white" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2.5">
            <polyline points="2 6 5 9 10 3" />
          </svg>
        )}
      </button>
      <ColorPicker color={item.color} onChange={c => onChange({ color: c })} />
      <span className="text-sm text-slate-400 flex-1">{meta.label}</span>
      <select
        value={item.lineWidth}
        onChange={e => onChange({ lineWidth: Number(e.target.value) })}
        className="bg-slate-700 border border-white/10 rounded px-1.5 py-1.5 text-sm text-white"
      >
        {LINE_WIDTHS.map(lw => (
          <option key={lw.value} value={lw.value}>{lw.label}</option>
        ))}
      </select>
    </div>
  );
}

function PanelRow({
  panelId, items, onChange,
}: {
  panelId: PanelId;
  items: IndicatorPresetItem[];
  onChange: (updated: IndicatorPresetItem[]) => void;
}) {
  const types = SELECTOR_GROUPS
    .find(g => g.types.some(t => INDICATOR_META[t]?.panel === panelId))?.types
    .filter(t => INDICATOR_META[t]?.panel === panelId) ?? [];

  function updateItem(type: IndicatorType, patch: Partial<IndicatorPresetItem>) {
    onChange(items.map(it => it.type === type ? { ...it, ...patch } : it));
  }

  const panelEnabled = types.some(t => items.find(it => it.type === t)?.enabled);

  return (
    <div className="border border-white/8 rounded-lg mb-1.5 bg-slate-800/40">
      <div className="flex items-center gap-2 px-2 py-2.5">
        <button
          onClick={() => {
            const next = !panelEnabled;
            onChange(items.map(it => types.includes(it.type) ? { ...it, enabled: next } : it));
          }}
          className={`w-5 h-5 rounded border flex-shrink-0 flex items-center justify-center transition ${
            panelEnabled ? "bg-violet-500 border-violet-500" : "border-white/20 bg-transparent"
          }`}
        >
          {panelEnabled && (
            <svg className="w-3 h-3 text-white" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="2 6 5 9 10 3" />
            </svg>
          )}
        </button>
        <span className="text-sm text-slate-300 font-medium">{PANEL_LABELS[panelId]}</span>
      </div>
      {types.map(type => {
        const item = items.find(it => it.type === type);
        if (!item) return null;
        return (
          <IndicatorRow
            key={type}
            type={type}
            item={item}
            indent
            onChange={patch => updateItem(type, patch)}
          />
        );
      })}
    </div>
  );
}

export default function IndicatorManager({
  open, onClose, presets, activePreset, loading, create, update, remove, reorder,
}: Props) {
  const [items, setItems]           = useState<IndicatorPresetItem[]>(buildDefaultItems());
  const [presetName, setPresetName] = useState("");
  const [newName, setNewName]       = useState("");
  const [creating, setCreating]     = useState(false);
  const [saving, setSaving]         = useState(false);
  const [savedOk, setSavedOk]       = useState(false);
  const [sortingPresets, setSortingPresets] = useState(false);
  const [error, setError]           = useState("");

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  useEffect(() => {
    if (!loading && presets.length === 0) setCreating(true);
  }, [loading, presets.length]);

  useEffect(() => {
    setError("");
    if (!activePreset) {
      setItems(buildDefaultItems());
      setPresetName("");
      return;
    }
    setItems(buildMergedItems(activePreset.items));
    setPresetName(activePreset.name);
  }, [activePreset]);

  const isDirty = useMemo(() => {
    if (!activePreset) return false;
    if (presetName !== activePreset.name) return true;
    const baseline = buildMergedItems(activePreset.items);
    for (const item of items) {
      const base = baseline.find(b => b.type === item.type)!;
      if (item.enabled !== base.enabled || item.color !== base.color || item.lineWidth !== base.lineWidth) return true;
    }
    return false;
  }, [activePreset, presetName, items]);

  function updateItem(type: IndicatorType, patch: Partial<IndicatorPresetItem>) {
    setItems(prev => prev.map(it => it.type === type ? { ...it, ...patch } : it));
  }

  function updateItemsForTypes(types: IndicatorType[], patch: Partial<IndicatorPresetItem>) {
    setItems(prev => prev.map(it => types.includes(it.type) ? { ...it, ...patch } : it));
  }

  function handlePresetDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIdx = presets.findIndex(p => p.id === active.id);
    const newIdx = presets.findIndex(p => p.id === over.id);
    const next = arrayMove(presets, oldIdx, newIdx);
    reorder(next.map(p => p.id)).catch(() => setError("순서 저장에 실패했습니다."));
  }

  async function handleSave() {
    if (!activePreset || !isDirty) return;
    setSaving(true);
    setError("");
    try {
      await update(activePreset.id, presetName, {
        items,
        panelOrder: activePreset.panelOrder as PanelId[],
      });
      setSavedOk(true);
      setTimeout(() => setSavedOk(false), 2000);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "";
      setError(msg === "PRESET_NAME_DUPLICATE" ? "같은 이름의 프리셋이 있어 저장할 수 없습니다." : "저장 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleCreate() {
    if (!newName.trim()) return;
    setSaving(true);
    setError("");
    try {
      await create(newName.trim(), { items: buildDefaultItems(), panelOrder: DEFAULT_PANEL_ORDER });
      setNewName("");
      setCreating(false);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "";
      setError(msg === "PRESET_NAME_DUPLICATE" ? "같은 이름의 프리셋이 있어 생성할 수 없습니다." : "생성 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!activePreset || presets.length <= 1) return;
    if (!confirm(`"${activePreset.name}" 프리셋을 삭제하시겠습니까?`)) return;
    try {
      await remove(activePreset.id);
    } catch {
      setError("삭제 중 오류가 발생했습니다.");
    }
  }

  const subPanelIds = Array.from(new Set(
    SELECTOR_GROUPS.flatMap(g => g.types)
      .map(t => INDICATOR_META[t]?.panel)
      .filter((p): p is PanelId => !!p && p !== "OVERLAY")
  ));

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4"
      onMouseDown={onClose}
    >
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />

      <div
        className="relative w-full sm:max-w-lg max-h-[92dvh] sm:max-h-[85vh] bg-slate-900 sm:rounded-2xl rounded-t-2xl border border-white/10 flex flex-col shadow-2xl"
        onMouseDown={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-4 border-b border-white/10 flex-shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="text-base text-white font-semibold flex-shrink-0">지표 관리</span>
            {activePreset && !creating && (
              <span className="text-xs text-slate-500 truncate">— {activePreset.name}</span>
            )}
            {creating && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-violet-500/20 text-violet-300 font-medium border border-violet-500/30 flex-shrink-0">
                새 프리셋 생성 중
              </span>
            )}
          </div>
          <div className="flex items-center gap-1 flex-shrink-0">
            {!sortingPresets && (
              <button
                onClick={() => { setCreating(v => !v); setError(""); setNewName(""); }}
                className={`text-sm px-2.5 py-1.5 rounded-lg transition border ${
                  creating
                    ? "bg-violet-500/20 text-violet-300 border-violet-500/40"
                    : "text-slate-300 hover:text-white hover:bg-white/8 border-white/10 hover:border-white/20"
                }`}
              >
                + 새 프리셋
              </button>
            )}
            <button
              onClick={() => { setSortingPresets(v => !v); setError(""); }}
              className={`transition p-2 rounded-lg hover:bg-white/8 ${sortingPresets ? "text-violet-400" : "text-slate-400 hover:text-white"}`}
              title={sortingPresets ? "순서 변경 완료" : "프리셋 셀렉트 순서 변경"}
            >
              <svg className="w-4.5 h-4.5 w-[18px] h-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="3" y1="6"  x2="21" y2="6"  /><line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
                <polyline points="17 3 21 6 17 9" /><polyline points="7 15 3 18 7 21" />
              </svg>
            </button>
            {!sortingPresets && activePreset && (
              <button
                onClick={handleDelete}
                disabled={presets.length <= 1}
                className="text-slate-400 hover:text-red-400 transition p-2 rounded-lg hover:bg-red-500/10 disabled:opacity-30 disabled:cursor-not-allowed"
                title="프리셋 삭제"
              >
                <svg className="w-[18px] h-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" />
                </svg>
              </button>
            )}
            <button onClick={onClose} className="text-slate-400 hover:text-white transition p-2 rounded-lg hover:bg-white/8">
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        {(activePreset || creating || sortingPresets) && (
          <div className="px-4 py-3 border-b border-white/10 flex-shrink-0 space-y-2.5">
            {activePreset && !creating && !sortingPresets && (
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-500 flex-shrink-0 w-8 text-right">이름</span>
                <input
                  value={presetName}
                  onChange={e => { setPresetName(e.target.value); setSavedOk(false); }}
                  className="flex-1 bg-slate-800 border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500"
                  placeholder="프리셋 이름"
                />
              </div>
            )}

            {creating && (
              <div className="p-3 rounded-lg bg-violet-500/8 border border-violet-500/20 space-y-2">
                <p className="text-xs text-violet-400/80">기본 설정으로 새 프리셋을 만듭니다.</p>
                <div className="flex items-center gap-2">
                  <input
                    autoFocus
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === "Enter") handleCreate();
                      if (e.key === "Escape") { setCreating(false); setNewName(""); setError(""); }
                    }}
                    placeholder="프리셋 이름"
                    className="flex-1 bg-slate-800 border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500"
                  />
                  <button
                    onClick={handleCreate}
                    disabled={!newName.trim() || saving}
                    className="text-sm px-3 py-2 rounded-lg bg-violet-600 hover:bg-violet-500 text-white disabled:opacity-40 flex-shrink-0 font-medium"
                  >
                    {saving ? "생성 중..." : "생성"}
                  </button>
                  <button
                    onClick={() => { setCreating(false); setNewName(""); setError(""); }}
                    className="text-sm px-3 py-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/8 flex-shrink-0"
                  >취소</button>
                </div>
              </div>
            )}

            {sortingPresets && presets.length > 0 && (
              <>
                <p className="text-xs text-slate-500">셀렉트 박스에 표시될 프리셋 순서를 변경합니다.</p>
                <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handlePresetDragEnd}>
                  <SortableContext items={presets.map(p => p.id)} strategy={verticalListSortingStrategy}>
                    <div className="space-y-1">
                      {presets.map(p => (
                        <SortablePresetRow key={p.id} preset={p} isActive={activePreset?.id === p.id} />
                      ))}
                    </div>
                  </SortableContext>
                </DndContext>
              </>
            )}

            {error && <p className="text-sm text-red-400">{error}</p>}
          </div>
        )}

        <div className="flex-1 overflow-y-auto py-2 px-3">
          {loading ? (
            <div className="flex items-center justify-center h-32 text-slate-500 text-sm">불러오는 중...</div>
          ) : !activePreset ? null : (
            <>
              <p className="text-xs text-slate-500 uppercase tracking-wider px-1 mb-1.5">가격 오버레이</p>
              {SELECTOR_GROUPS.filter(g => g.types.some(t => INDICATOR_META[t]?.panel === "OVERLAY")).map(group => {
                const overlayTypes = group.types.filter(t => INDICATOR_META[t]?.panel === "OVERLAY");
                if (group.bundled) {
                  const groupEnabled = overlayTypes.some(t => items.find(it => it.type === t)?.enabled);
                  return (
                    <div key={group.label} className="border border-white/8 rounded-lg mb-1.5 bg-slate-800/40">
                      <div className="flex items-center gap-2 px-2 py-2.5">
                        <button
                          onClick={() => updateItemsForTypes(overlayTypes, { enabled: !groupEnabled })}
                          className={`w-5 h-5 rounded border flex-shrink-0 flex items-center justify-center transition ${
                            groupEnabled ? "bg-violet-500 border-violet-500" : "border-white/20 bg-transparent"
                          }`}
                        >
                          {groupEnabled && (
                            <svg className="w-3 h-3 text-white" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2">
                              <polyline points="2 6 5 9 10 3" />
                            </svg>
                          )}
                        </button>
                        <span className="text-sm text-slate-300 font-medium">{group.label}</span>
                      </div>
                      {overlayTypes.map(type => {
                        const item = items.find(it => it.type === type);
                        if (!item) return null;
                        return (
                          <IndicatorRow
                            key={type}
                            type={type}
                            item={item}
                            indent
                            onChange={patch => updateItem(type, patch)}
                          />
                        );
                      })}
                    </div>
                  );
                }
                return (
                  <div key={group.label} className="mb-1">
                    <p className="text-xs text-slate-500 px-2 mb-1">{group.label}</p>
                    {overlayTypes.map(type => {
                      const item = items.find(it => it.type === type);
                      if (!item) return null;
                      return (
                        <IndicatorRow
                          key={type}
                          type={type}
                          item={item}
                          onChange={patch => updateItem(type, patch)}
                        />
                      );
                    })}
                  </div>
                );
              })}

              <p className="text-xs text-slate-500 uppercase tracking-wider px-1 mt-3 mb-1">서브패널</p>
              <p className="text-xs text-slate-600 px-1 mb-2">순서는 툴바의 보조지표 순서 버튼으로 변경</p>
              {subPanelIds.map(panelId => (
                <PanelRow
                  key={panelId}
                  panelId={panelId}
                  items={items}
                  onChange={updated => setItems(prev => prev.map(it => {
                    const u = updated.find(u => u.type === it.type);
                    return u ?? it;
                  }))}
                />
              ))}
            </>
          )}
        </div>

        {activePreset && !creating && (
          <div className="px-4 py-3 border-t border-white/10 flex-shrink-0">
            <button
              onClick={handleSave}
              disabled={saving || !isDirty}
              className={`w-full py-2.5 rounded-lg text-sm font-medium transition ${
                isDirty
                  ? "bg-violet-600 hover:bg-violet-500 text-white"
                  : savedOk
                  ? "bg-emerald-600/20 text-emerald-400 cursor-default"
                  : "bg-slate-800 text-slate-500 cursor-default"
              }`}
            >
              {saving ? "저장 중..." : isDirty ? "변경사항 저장" : savedOk ? "저장됨 ✓" : "저장됨"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

