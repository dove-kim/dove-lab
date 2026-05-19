import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest, { params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  const market = req.nextUrl.searchParams.get("market") ?? "KOSPI";
  const limit = req.nextUrl.searchParams.get("limit") ?? "60";
  const res = await backendFetch(
    `/stocks/${encodeURIComponent(code)}/prices?market=${market}&limit=${limit}`
  );
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
