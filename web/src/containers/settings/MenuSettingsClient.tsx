"use client";

import { useState, useMemo } from "react";
import { clientFetch } from "@/services/client";
import { cx } from "@/utils/cx";
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
  arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import type { UserMenu, MenuModule, MenuFeature } from "@/types/user";

const FEATURE_LABELS: Record<string, string> = {
  STOCK_SEARCH: "주식 종목 검색",
  STOCK_LEDGER: "주식 장부",
  BUDGET: "가계부",
};

const MODULE_LABELS: Record<string, string> = {
  STOCK: "주식",
  BUDGET: "가계부",
};

interface Props {
  menu: UserMenu;
}

function FeatureRow({
  feature,
  moduleCode,
  originalHidden,
  onToggleHidden,
}: {
  feature: MenuFeature;
  moduleCode: string;
  originalHidden: boolean;
  onToggleHidden: (featureCode: string, hidden: boolean) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: feature.featureCode });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const visibilityChanged = feature.hidden !== originalHidden;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`flex items-center gap-3 px-4 py-3 border rounded-xl transition ${
        visibilityChanged
          ? "bg-amber-500/5 border-amber-500/20"
          : "bg-slate-800/40 border-white/8"
      }`}
    >
      <button
        className="flex-shrink-0 text-slate-600 hover:text-slate-400 cursor-grab active:cursor-grabbing transition"
        {...attributes}
        {...listeners}
        title="드래그로 순서 변경"
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <rect x="4" y="6" width="16" height="2" rx="1" />
          <rect x="4" y="11" width="16" height="2" rx="1" />
          <rect x="4" y="16" width="16" height="2" rx="1" />
        </svg>
      </button>

      <span className="flex-1 text-sm text-slate-300">
        {FEATURE_LABELS[feature.featureCode] ?? feature.featureCode}
        {visibilityChanged && (
          <span className="ml-2 text-amber-400 text-xs font-normal">
            ({feature.hidden ? "숨김으로 변경" : "표시로 변경"})
          </span>
        )}
      </span>

      <button
        onClick={() => onToggleHidden(feature.featureCode, !feature.hidden)}
        className={`text-xs px-3 py-1.5 rounded-lg font-medium transition min-w-[56px] text-center ${
          feature.hidden
            ? "bg-slate-700/50 text-slate-500 border border-white/10 hover:bg-indigo-600/20 hover:text-indigo-300 hover:border-indigo-500/30"
            : "bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 hover:bg-slate-700/50 hover:text-slate-500 hover:border-white/10"
        }`}
      >
        {feature.hidden ? "숨김" : "표시"}
      </button>
    </div>
  );
}

function ModuleSection({
  module,
  originalFeatures,
  onToggleFeatureHidden,
  onReorderFeatures,
}: {
  module: MenuModule;
  originalFeatures: MenuFeature[];
  onToggleFeatureHidden: (moduleCode: string, featureCode: string, hidden: boolean) => void;
  onReorderFeatures: (moduleCode: string, newFeatures: MenuFeature[]) => void;
}) {
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
  const features = [...module.features].sort((a, b) => a.displayOrder - b.displayOrder);

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIdx = features.findIndex((f) => f.featureCode === active.id);
    const newIdx = features.findIndex((f) => f.featureCode === over.id);
    const next = arrayMove(features, oldIdx, newIdx).map((f, i) => ({ ...f, displayOrder: i }));
    onReorderFeatures(module.moduleCode, next);
  }

  return (
    <div className="bg-white/5 border border-white/10 rounded-2xl overflow-hidden">
      <div className="px-4 py-3 border-b border-white/10">
        <h3 className="text-slate-200 text-sm font-semibold">
          {MODULE_LABELS[module.moduleCode] ?? module.moduleCode}
        </h3>
      </div>
      {features.length === 0 ? (
        <p className="text-slate-600 text-sm text-center py-6">기능이 없습니다</p>
      ) : (
        <div className="p-3 flex flex-col gap-2">
          <DndContext
            id={`features-${module.moduleCode}`}
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={features.map((f) => f.featureCode)}
              strategy={verticalListSortingStrategy}
            >
              {features.map((feature) => {
                const original = originalFeatures.find((f) => f.featureCode === feature.featureCode);
                return (
                  <FeatureRow
                    key={feature.featureCode}
                    feature={feature}
                    moduleCode={module.moduleCode}
                    originalHidden={original?.hidden ?? feature.hidden}
                    onToggleHidden={(featureCode, hidden) =>
                      onToggleFeatureHidden(module.moduleCode, featureCode, hidden)
                    }
                  />
                );
              })}
            </SortableContext>
          </DndContext>
        </div>
      )}
    </div>
  );
}

function SortableModuleWrapper({
  moduleCode,
  children,
}: {
  moduleCode: string;
  children: React.ReactNode;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: moduleCode });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} className="flex gap-2 items-start">
      <button
        className="mt-3.5 text-slate-600 hover:text-slate-400 cursor-grab active:cursor-grabbing transition flex-shrink-0 pt-1"
        {...attributes}
        {...listeners}
        title="모듈 순서 변경"
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <rect x="4" y="6" width="16" height="2" rx="1" />
          <rect x="4" y="11" width="16" height="2" rx="1" />
          <rect x="4" y="16" width="16" height="2" rx="1" />
        </svg>
      </button>
      <div className="flex-1">{children}</div>
    </div>
  );
}

export default function MenuSettingsClient({ menu: initialMenu }: Props) {
  const [serverMenu] = useState<UserMenu>(initialMenu);
  const [draftMenu, setDraftMenu] = useState<UserMenu>(initialMenu);
  const [saving, setSaving] = useState(false);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
  const sortedModules = [...draftMenu.modules].sort((a, b) => a.displayOrder - b.displayOrder);

  const isDirty = useMemo(() => {
    const serverSorted = [...serverMenu.modules].sort((a, b) => a.displayOrder - b.displayOrder);
    const draftSorted = [...draftMenu.modules].sort((a, b) => a.displayOrder - b.displayOrder);

    // 모듈 순서 변경 여부
    if (serverSorted.map((m) => m.moduleCode).join() !== draftSorted.map((m) => m.moduleCode).join()) {
      return true;
    }

    // 기능 순서·숨김 변경 여부
    for (const serverMod of serverMenu.modules) {
      const draftMod = draftMenu.modules.find((m) => m.moduleCode === serverMod.moduleCode);
      if (!draftMod) continue;

      const serverFeatures = [...serverMod.features].sort((a, b) => a.displayOrder - b.displayOrder);
      const draftFeatures = [...draftMod.features].sort((a, b) => a.displayOrder - b.displayOrder);

      if (serverFeatures.map((f) => f.featureCode).join() !== draftFeatures.map((f) => f.featureCode).join()) {
        return true;
      }
      for (const sf of serverMod.features) {
        const df = draftMod.features.find((f) => f.featureCode === sf.featureCode);
        if (df && df.hidden !== sf.hidden) return true;
      }
    }
    return false;
  }, [serverMenu, draftMenu]);

  function handleModuleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIdx = sortedModules.findIndex((m) => m.moduleCode === active.id);
    const newIdx = sortedModules.findIndex((m) => m.moduleCode === over.id);
    const next = arrayMove(sortedModules, oldIdx, newIdx).map((m, i) => ({ ...m, displayOrder: i }));
    setDraftMenu({ modules: next });
  }

  function handleToggleFeatureHidden(moduleCode: string, featureCode: string, hidden: boolean) {
    setDraftMenu((prev) => ({
      modules: prev.modules.map((m) =>
        m.moduleCode === moduleCode
          ? { ...m, features: m.features.map((f) => f.featureCode === featureCode ? { ...f, hidden } : f) }
          : m
      ),
    }));
  }

  function handleReorderFeatures(moduleCode: string, newFeatures: MenuFeature[]) {
    setDraftMenu((prev) => ({
      modules: prev.modules.map((m) =>
        m.moduleCode === moduleCode ? { ...m, features: newFeatures } : m
      ),
    }));
  }

  function discardChanges() {
    setDraftMenu(serverMenu);
  }

  async function saveChanges() {
    if (!isDirty) return;
    setSaving(true);
    try {
      const requests: Promise<unknown>[] = [];

      // 모듈 순서 변경
      const serverModOrder = [...serverMenu.modules].sort((a, b) => a.displayOrder - b.displayOrder).map((m) => m.moduleCode).join();
      const draftModOrder = sortedModules.map((m) => m.moduleCode).join();
      if (serverModOrder !== draftModOrder) {
        requests.push(
          clientFetch("/api/account/menu/modules/reorder", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ modules: sortedModules.map((m) => m.moduleCode) }),
          })
        );
      }

      // 기능별 순서·숨김 변경
      for (const draftMod of draftMenu.modules) {
        const serverMod = serverMenu.modules.find((m) => m.moduleCode === draftMod.moduleCode);
        if (!serverMod) continue;

        const serverFeatureOrder = [...serverMod.features].sort((a, b) => a.displayOrder - b.displayOrder).map((f) => f.featureCode).join();
        const draftFeatureOrder = [...draftMod.features].sort((a, b) => a.displayOrder - b.displayOrder).map((f) => f.featureCode).join();

        if (serverFeatureOrder !== draftFeatureOrder) {
          const sorted = [...draftMod.features].sort((a, b) => a.displayOrder - b.displayOrder);
          requests.push(
            clientFetch(`/api/account/menu/modules/${draftMod.moduleCode}/features/reorder`, {
              method: "PATCH",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ features: sorted.map((f) => f.featureCode) }),
            })
          );
        }

        for (const draftFeature of draftMod.features) {
          const serverFeature = serverMod.features.find((f) => f.featureCode === draftFeature.featureCode);
          if (serverFeature && serverFeature.hidden !== draftFeature.hidden) {
            requests.push(
              clientFetch(`/api/account/menu/features/${draftFeature.featureCode}/hidden`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ hidden: draftFeature.hidden }),
              })
            );
          }
        }
      }

      await Promise.all(requests);
      // 저장 성공 시 serverMenu를 draftMenu 기준으로 갱신할 수 없으므로 페이지 새로고침
      window.location.reload();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-white text-lg font-semibold">메뉴 설정</h2>
          <p className="text-slate-400 text-sm mt-1">
            메뉴 항목의 표시 여부와 순서를 변경할 수 있습니다
          </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          {isDirty && (
            <button
              onClick={discardChanges}
              disabled={saving}
              className={cx.btnSecondary}
            >
              되돌리기
            </button>
          )}
          <button
            onClick={saveChanges}
            disabled={!isDirty || saving}
            className={`${cx.btnPrimary} disabled:opacity-30 disabled:cursor-not-allowed`}
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>

      {sortedModules.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-14 h-14 rounded-full bg-slate-800 flex items-center justify-center mb-4">
            <svg className="w-7 h-7 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </div>
          <p className="text-slate-400 font-medium">설정 가능한 메뉴가 없습니다</p>
          <p className="text-slate-500 text-sm mt-1">관리자에게 기능 권한을 요청하세요</p>
        </div>
      ) : (
        <div className="max-w-xl flex flex-col gap-4">
          <p className="text-xs text-slate-500">모듈·기능 순서는 드래그로 변경할 수 있습니다</p>

          <DndContext
            id="modules"
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleModuleDragEnd}
          >
            <SortableContext
              items={sortedModules.map((m) => m.moduleCode)}
              strategy={verticalListSortingStrategy}
            >
              <div className="flex flex-col gap-2">
                {sortedModules.map((mod) => {
                  const serverMod = serverMenu.modules.find((m) => m.moduleCode === mod.moduleCode);
                  return (
                    <SortableModuleWrapper key={mod.moduleCode} moduleCode={mod.moduleCode}>
                      <ModuleSection
                        module={mod}
                        originalFeatures={serverMod?.features ?? mod.features}
                        onToggleFeatureHidden={handleToggleFeatureHidden}
                        onReorderFeatures={handleReorderFeatures}
                      />
                    </SortableModuleWrapper>
                  );
                })}
              </div>
            </SortableContext>
          </DndContext>

        </div>
      )}
    </div>
  );
}
