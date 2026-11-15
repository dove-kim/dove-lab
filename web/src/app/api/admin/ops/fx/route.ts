import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function GET(req: NextRequest) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { searchParams } = new URL(req.url);
  const days = searchParams.get("days") ?? "30";

  const res = await backendFetch(`/admin/ops/fx?days=${days}`);
  if (!res) return unauthorized();
  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
