import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized, requireRole } from "@/services/backend";

export async function POST(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/models/${id}/deactivate`, { method: "POST" });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
