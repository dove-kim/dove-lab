import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/utils/cn";

/**
 * 라벨·값(·보조설명)을 담는 통계 타일.
 */
export function StatTile({
  label,
  value,
  sub,
  tone,
  size = "lg",
}: {
  label: string;
  value: string;
  sub?: string;
  tone?: string;
  size?: "base" | "lg";
}) {
  return (
    <Card className="gap-0 rounded-lg bg-secondary py-3 ring-0 [--card-spacing:--spacing(3)]">
      <CardContent>
        <div className="text-xs text-slate-400">{label}</div>
        <div className={cn("font-semibold tabular-nums", size === "lg" ? "text-lg" : "text-base", tone ?? "text-white")}>
          {value}
        </div>
        {sub && <div className="mt-0.5 text-xs text-slate-500">{sub}</div>}
      </CardContent>
    </Card>
  );
}
