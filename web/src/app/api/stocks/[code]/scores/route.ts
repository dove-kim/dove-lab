import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest, { params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  const sp      = req.nextUrl.searchParams;
  const modelId = sp.get("modelId") ?? "";
  const from    = sp.get("from");
  const to      = sp.get("to");

  const qs = new URLSearchParams({ modelId });
  if (from) qs.set("from", from);
  if (to)   qs.set("to", to);

  const res = await backendFetch(`/stocks/${encodeURIComponent(code)}/scores?${qs.toString()}`);
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
