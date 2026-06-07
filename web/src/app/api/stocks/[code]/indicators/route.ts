import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest, { params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  const source   = req.nextUrl.searchParams.get("source")   ?? "KRX";
  const adjusted = req.nextUrl.searchParams.get("adjusted") ?? "true";
  const limit    = req.nextUrl.searchParams.get("limit")    ?? "120";
  const types    = req.nextUrl.searchParams.get("types")    ?? "";
  if (!types) return NextResponse.json([]);

  const qs = new URLSearchParams({ source, adjusted, limit });
  types.split(",").forEach((t) => qs.append("types", t));

  const res = await backendFetch(`/stocks/${encodeURIComponent(code)}/indicators?${qs}`);
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
