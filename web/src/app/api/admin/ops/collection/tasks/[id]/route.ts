import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized, requireRole } from "@/services/backend";

export async function GET(_: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/collection/tasks/${id}`);
  if (!res) return unauthorized();
  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}

export async function POST(_: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const { id } = await params;
  const res = await backendFetch(`/admin/ops/collection/tasks/${id}/retry`, { method: "POST" });
  if (!res) return unauthorized();
  const data = await res.json();
  return NextResponse.json(data, { status: res.status });
}
