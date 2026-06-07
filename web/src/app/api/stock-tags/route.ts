import { NextResponse } from "next/server";
import { backendFetch, safeJson, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/stock-tags");
  if (!res) return unauthorized();
  return NextResponse.json(await safeJson(res), { status: res.status });
}
