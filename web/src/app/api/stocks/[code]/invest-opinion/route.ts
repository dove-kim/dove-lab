import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  const { code } = await params;
  const res = await backendFetch(`/stocks/${code}/invest-opinion`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
