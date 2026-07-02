import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  const { code } = await params;
  const res = await backendFetch(`/stocks/${code}/valuation/latest`);
  if (!res) return unauthorized();
  if (res.status === 204) return new NextResponse(null, { status: 204 });
  return NextResponse.json(await safeJson(res), { status: res.status });
}
