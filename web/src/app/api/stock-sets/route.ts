import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/stock-sets");
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json({ error: "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(await res.json());
}

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => ({}));
  const res = await backendFetch("/stock-sets", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  const data = await safeJson(res);
  if (!res.ok) return NextResponse.json({ error: (data as { detail?: string })?.detail ?? "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(data, { status: 201 });
}
