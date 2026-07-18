import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function DELETE(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const res = await backendFetch(`/portfolio/shares/${id}`, { method: "DELETE" });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
