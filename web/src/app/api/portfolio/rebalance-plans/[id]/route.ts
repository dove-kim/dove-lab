import { NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function DELETE(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/portfolio/rebalance-plans/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  if (res.status === 204) return new NextResponse(null, { status: 204 });
  return NextResponse.json(await safeJson(res), { status: res.status });
}
