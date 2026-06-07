import { NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function GET() {
  const guard = await requireRole(["ADMIN", "ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const res = await backendFetch(`/admin/ops/api-quota`);
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json({}, { status: res.status });

  const body = await res.json() as { quotas: { name: string; used: number; limit: number; remaining: number; lastLimitAt: string | null }[] };
  const krx = body.quotas.find((q) => q.name === "KRX");
  if (!krx) return NextResponse.json({ error: "KRX_QUOTA_NOT_FOUND" }, { status: 404 });

  return NextResponse.json({
    used: krx.used,
    quota: krx.limit,
    remaining: krx.remaining,
    lastRateLimitAt: krx.lastLimitAt ?? null,
  });
}
