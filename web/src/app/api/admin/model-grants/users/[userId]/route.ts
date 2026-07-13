import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson, requireRole } from "@/services/backend";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ userId: string }> }) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const { userId } = await params;
  const res = await backendFetch(`/admin/model-grants/users/${userId}`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ userId: string }> }) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const { userId } = await params;
  const res = await backendFetch(`/admin/model-grants/users/${userId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
