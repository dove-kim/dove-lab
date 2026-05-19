import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, unauthorized } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";

async function requireAdmin(): Promise<NextResponse | null> {
  const token = (await cookies()).get("token")?.value;
  const role = token ? decodeJwtPayload(token)?.role : null;
  if (!token || (role !== "ADMIN" && role !== "ROOT")) {
    return NextResponse.json({ error: "FORBIDDEN" }, { status: 403 });
  }
  return null;
}

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireAdmin();
  if (denied) return denied;
  const { id } = await params;
  const res = await backendFetch(`/admin/users/${id}/features`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
