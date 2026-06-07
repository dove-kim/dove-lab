import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized, requireRole } from "@/services/backend";

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const body = await req.json();
  const res = await backendFetch(`/admin/stock-tags/${id}/label`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  if (res.status === 204) return new NextResponse(null, { status: 204 });
  return NextResponse.json(await safeJson(res), { status: res.status });
}
