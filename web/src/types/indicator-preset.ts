import type { IndicatorType } from "./filter";
import type { PanelId } from "@/components/chart/indicatorMeta";

export interface IndicatorPresetItem {
  type: IndicatorType;
  enabled: boolean;
  color: string;
  lineWidth: number;
}

export interface IndicatorPreset {
  id: number;
  name: string;
  items: IndicatorPresetItem[];
  panelOrder: PanelId[];
  createdAt: string;
  updatedAt: string;
}

export const COLOR_PALETTE: string[] = [
  "#ef4444", "#f97316", "#f59e0b", "#fbbf24",
  "#84cc16", "#22c55e", "#10b981", "#14b8a6",
  "#06b6d4", "#3b82f6", "#6366f1", "#8b5cf6",
  "#a855f7", "#ec4899", "#f43f5e", "#fb923c",
  "#4ade80", "#38bdf8", "#ffffff", "#94a3b8",
];

export const LINE_WIDTHS = [
  { value: 1, label: "1px" },
  { value: 2, label: "2px" },
  { value: 3, label: "3px" },
  { value: 4, label: "4px" },
  { value: 5, label: "5px" },
];
