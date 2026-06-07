import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/stock-filters/personal");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const res = await backendFetch("/stock-filters/personal", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
