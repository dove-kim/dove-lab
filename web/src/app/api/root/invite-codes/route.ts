import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";

async function requireRoot(): Promise<NextResponse | null> {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") {
    return NextResponse.json({ error: "FORBIDDEN" }, { status: 403 });
  }
  return null;
}

export async function GET() {
  const denied = await requireRoot();
  if (denied) return denied;
  const res = await backendFetch("/root/invite-codes");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function POST(req: NextRequest) {
  const denied = await requireRoot();
  if (denied) return denied;
  const res = await backendFetch("/root/invite-codes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
