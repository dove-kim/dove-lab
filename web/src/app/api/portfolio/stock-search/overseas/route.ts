import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest) {
  const market = req.nextUrl.searchParams.get("market") ?? "";
  const ticker = req.nextUrl.searchParams.get("ticker") ?? "";
  const res = await backendFetch(
    `/portfolio/stock-search/overseas?market=${encodeURIComponent(market)}&ticker=${encodeURIComponent(ticker)}`
  );
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
