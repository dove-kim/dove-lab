"use client";

import { useCallback, useEffect, useState } from "react";
import type { IndicatorPreset } from "@/types/indicator-preset";
import { clientFetch } from "@/services/client";

export function useIndicatorPresets() {
  const [presets, setPresets]           = useState<IndicatorPreset[]>([]);
  const [activePreset, setActivePreset] = useState<IndicatorPreset | null>(null);
  const [loading, setLoading]           = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await clientFetch("/api/indicator-presets");
      if (!res || !res.ok) return;
      const data: IndicatorPreset[] = await res.json();
      setPresets(data);
      if (data.length > 0 && !activePreset) setActivePreset(data[0]);
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load(); }, [load]);

  const create = useCallback(async (name: string, preset: Omit<IndicatorPreset, "id" | "name" | "createdAt" | "updatedAt">) => {
    const res = await clientFetch("/api/indicator-presets", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, items: preset.items, panelOrder: preset.panelOrder }),
    });
    if (!res) throw new Error("PRESET_CREATE_FAILED");
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail ?? "PRESET_CREATE_FAILED");
    }
    const created: IndicatorPreset = await res.json();
    setPresets(prev => [...prev, created]);
    setActivePreset(created);
    return created;
  }, []);

  const update = useCallback(async (id: number, name: string, preset: Omit<IndicatorPreset, "id" | "name" | "createdAt" | "updatedAt">) => {
    const res = await clientFetch(`/api/indicator-presets/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, items: preset.items, panelOrder: preset.panelOrder }),
    });
    if (!res) throw new Error("PRESET_UPDATE_FAILED");
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail ?? "PRESET_UPDATE_FAILED");
    }
    const updated: IndicatorPreset = await res.json();
    setPresets(prev => prev.map(p => p.id === id ? updated : p));
    setActivePreset(prev => prev?.id === id ? updated : prev);
    return updated;
  }, []);

  const remove = useCallback(async (id: number) => {
    const res = await clientFetch(`/api/indicator-presets/${id}`, { method: "DELETE" });
    if (!res) throw new Error("PRESET_DELETE_FAILED");
    if (!res.ok) throw new Error("PRESET_DELETE_FAILED");
    setPresets(prev => {
      const next = prev.filter(p => p.id !== id);
      setActivePreset(cur => cur?.id === id ? (next[0] ?? null) : cur);
      return next;
    });
  }, []);

  const reorder = useCallback(async (orderedIds: number[]) => {
    const res = await clientFetch("/api/indicator-presets/reorder", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ids: orderedIds }),
    });
    if (!res) throw new Error("PRESET_REORDER_FAILED");
    if (!res.ok) throw new Error("PRESET_REORDER_FAILED");
    setPresets(prev => {
      const map = new Map(prev.map(p => [p.id, p]));
      return orderedIds.flatMap(id => map.has(id) ? [map.get(id)!] : []);
    });
  }, []);

  return { presets, activePreset, setActivePreset, loading, create, update, remove, reorder };
}

export type UseIndicatorPresetsReturn = ReturnType<typeof useIndicatorPresets>;
