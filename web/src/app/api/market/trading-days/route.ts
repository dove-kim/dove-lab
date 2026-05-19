import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest) {
  const limit = req.nextUrl.searchParams.get("limit") ?? "90";
  const res = await backendFetch(`/market/trading-days?limit=${limit}`);
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json({ latestDate: null, tradingDays: [] }, { status: res.status });
  return NextResponse.json(await res.json());
}
