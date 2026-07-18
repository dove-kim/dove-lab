import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ accountId: string }> }) {
  const { accountId } = await params;
  const res = await backendFetch(`/portfolio/shared/${accountId}/roundtrips`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
