import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const { id } = await params;
  const res = await backendFetch(`/admin/users/${id}/sub-menus`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
