import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";

export async function POST(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const body = await req.json().catch(() => ({}));
  const res = await backendFetch(`/filters/${id}/execute`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  const data = await safeJson(res);
  if (!res.ok) return NextResponse.json({ error: (data as { detail?: string })?.detail ?? "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(data);
}
