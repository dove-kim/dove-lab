import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, safeJson } from "@/services/backend";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/filters/${id}`);
  if (!res) return unauthorized();
  if (res.status === 404) return NextResponse.json({ error: "NOT_FOUND" }, { status: 404 });
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function PUT(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/filters/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  if (res.status === 409) return NextResponse.json({ error: "FILTER_NAME_DUPLICATE" }, { status: 409 });
  if (!res.ok) return NextResponse.json({ error: "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(await res.json());
}

export async function DELETE(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/filters/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  if (res.status === 404) return NextResponse.json({ error: "NOT_FOUND" }, { status: 404 });
  if (!res.ok) return NextResponse.json({ error: "SERVER_ERROR" }, { status: res.status });
  return new NextResponse(null, { status: 204 });
}
