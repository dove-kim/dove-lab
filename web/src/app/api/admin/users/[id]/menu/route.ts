import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson, requireRole } from "@/services/backend";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const { id } = await params;
  const res = await backendFetch(`/admin/users/${id}/menu`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
