import { NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/portfolio/all/roundtrips");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
