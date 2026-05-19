import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/stock-sets/${id}`);
  if (!res) return unauthorized();
  const data = await safeJson(res);
  if (!res.ok) return NextResponse.json({ error: (data as { detail?: string })?.detail ?? "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(data);
}

export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const body = await req.json().catch(() => ({}));
  const res = await backendFetch(`/stock-sets/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  const data = await safeJson(res);
  if (!res.ok) return NextResponse.json({ error: (data as { detail?: string })?.detail ?? "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(data);
}

export async function DELETE(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/stock-sets/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  if (!res.ok) {
    const data = await safeJson(res);
    return NextResponse.json({ error: (data as { detail?: string })?.detail ?? "SERVER_ERROR" }, { status: res.status });
  }
  return new NextResponse(null, { status: 204 });
}
