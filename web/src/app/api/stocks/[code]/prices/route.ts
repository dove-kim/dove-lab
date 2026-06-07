import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET(req: NextRequest, { params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  const source   = req.nextUrl.searchParams.get("source")   ?? "KRX";
  const adjusted = req.nextUrl.searchParams.get("adjusted") ?? "true";
  const limit    = req.nextUrl.searchParams.get("limit")    ?? "60";
  const res = await backendFetch(
    `/stocks/${encodeURIComponent(code)}/prices?source=${source}&adjusted=${adjusted}&limit=${limit}`
  );
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
