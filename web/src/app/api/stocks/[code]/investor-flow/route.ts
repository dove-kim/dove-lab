import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  const { code } = await params;
  const source = req.nextUrl.searchParams.get("source") ?? "KRX";
  const limit = req.nextUrl.searchParams.get("limit") ?? "60";
  const res = await backendFetch(
    `/stocks/${code}/investor-flow?source=${source}&limit=${limit}`
  );
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
