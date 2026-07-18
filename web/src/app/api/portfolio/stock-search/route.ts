import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest) {
  const q = req.nextUrl.searchParams.get("q") ?? "";
  const res = await backendFetch(`/portfolio/stock-search?q=${encodeURIComponent(q)}`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
