import { NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson, requireRole } from "@/services/backend";

export async function GET() {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const res = await backendFetch(`/admin/model-grants/models`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
