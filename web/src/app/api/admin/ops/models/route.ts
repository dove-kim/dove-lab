import { NextRequest, NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized, requireRole } from "@/services/backend";

export async function GET() {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  const res = await backendFetch("/admin/ops/models");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}

export async function POST(req: NextRequest) {
  const guard = await requireRole(["ROOT"]);
  if (guard instanceof NextResponse) return guard;

  // multipart(.pkl artifact + meta.json + scoreExchanges/scorePriceType)를 그대로 전달.
  const form = await req.formData();
  const res = await backendFetch("/admin/ops/models", {
    method: "POST",
    body: form,
  });
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
