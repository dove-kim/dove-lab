import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, unauthorized } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";

async function requireRoot(): Promise<NextResponse | null> {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") {
    return NextResponse.json({ error: "FORBIDDEN" }, { status: 403 });
  }
  return null;
}

export async function POST(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const denied = await requireRoot();
  if (denied) return denied;

  const { id } = await params;
  const res = await backendFetch(`/root/users/${id}/reset-password`, { method: "POST" });
  if (!res) return unauthorized();
  if (!res.ok) return new NextResponse(null, { status: res.status });

  const data = await res.json();
  return NextResponse.json(data);
}
