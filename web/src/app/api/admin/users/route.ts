import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";

async function requireAdmin(): Promise<NextResponse | null> {
  const token = (await cookies()).get("token")?.value;
  const role = token ? decodeJwtPayload(token)?.role : null;
  if (!token || (role !== "ADMIN" && role !== "ROOT")) {
    return NextResponse.json({ error: "FORBIDDEN" }, { status: 403 });
  }
  return null;
}

export async function GET() {
  const denied = await requireAdmin();
  if (denied) return denied;
  const res = await backendFetch("/admin/users");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
