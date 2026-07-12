import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized, requireRole } from "@/services/backend";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/custom-metrics/${id}`);
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function PUT(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const body = await req.json();
  const res = await backendFetch(`/admin/ops/custom-metrics/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function DELETE(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/custom-metrics/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
