import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/indicator-presets");
  if (!res) return unauthorized();
  return NextResponse.json(await res.json(), { status: res.status });
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const res = await backendFetch("/indicator-presets", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  return NextResponse.json(await res.json(), { status: res.status });
}
