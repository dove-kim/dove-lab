import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function GET(req: NextRequest) {
  const guard = await requireRole(["ADMIN", "ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { searchParams } = new URL(req.url);
  const page = searchParams.get("page") ?? "0";
  const size = searchParams.get("size") ?? "50";

  const res = await backendFetch(`/admin/ops/system-events?page=${page}&size=${size}`);
  if (!res) return unauthorized();
  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
