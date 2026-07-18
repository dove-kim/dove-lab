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
    <div className="bg-white/5 rounded-lg p-3">
      <div className="text-xs text-slate-400">{label}</div>
      <div className={(size === "lg" ? "text-lg" : "text-base") + " font-semibold tabular-nums " + (tone ?? "text-white")}>
        {value}
      </div>
      {sub && <div className="text-xs text-slate-500 mt-0.5">{sub}</div>}
    </div>
  );
}
