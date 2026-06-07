import { NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function GET() {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const res = await backendFetch(`/admin/ops/scheduler/status`);
  if (!res) return unauthorized();
  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
