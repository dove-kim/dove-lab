import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function PATCH(req: NextRequest) {
  const body = await req.json();
  const res = await backendFetch("/filters/reorder", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  return new NextResponse(null, { status: res.status });
}
