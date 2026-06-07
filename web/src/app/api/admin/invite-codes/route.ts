import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson, requireRole } from "@/services/backend";

export async function GET() {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const res = await backendFetch("/admin/invite-codes");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function POST(req: NextRequest) {
  const denied = await requireRole(["ADMIN", "ROOT"]);
  if (denied instanceof NextResponse) return denied;
  const res = await backendFetch("/admin/invite-codes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
