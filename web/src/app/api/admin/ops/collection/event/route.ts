import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized, requireRole } from "@/services/backend";

export async function POST(req: NextRequest) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const body = await req.json();
  const res = await backendFetch("/admin/ops/collection/event", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
