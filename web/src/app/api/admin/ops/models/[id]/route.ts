import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function DELETE(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/models/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
