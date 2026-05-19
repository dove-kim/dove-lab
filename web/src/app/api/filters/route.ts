import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/filters");
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}

export async function POST(req: NextRequest) {
  const res = await backendFetch("/filters", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(await req.json()),
  });
  if (!res) return unauthorized();
  if (res.status === 409) return NextResponse.json({ error: "FILTER_NAME_DUPLICATE" }, { status: 409 });
  if (!res.ok) return NextResponse.json({ error: "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(await res.json(), { status: 201 });
}
