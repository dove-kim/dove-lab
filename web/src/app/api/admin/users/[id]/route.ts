import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function DELETE(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireRole(["ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const { id } = await params;
  const res = await backendFetch(`/admin/users/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
