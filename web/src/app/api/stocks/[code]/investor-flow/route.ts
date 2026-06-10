import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  const { code } = await params;
  const from = req.nextUrl.searchParams.get("from");
  const to = req.nextUrl.searchParams.get("to");
  if (!from || !to) return NextResponse.json([], { status: 400 });

  const res = await backendFetch(`/stocks/${code}/investor-flow?from=${from}&to=${to}`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
