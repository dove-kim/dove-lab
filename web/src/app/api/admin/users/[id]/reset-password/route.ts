import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function POST(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;

  const { id } = await params;
  const res = await backendFetch(`/admin/users/${id}/reset-password`, { method: "POST" });
  if (!res) return unauthorized();
  if (!res.ok) return new NextResponse(null, { status: res.status });

  const data = await res.json();
  return NextResponse.json(data);
}
