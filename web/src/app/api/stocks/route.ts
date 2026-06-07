import { NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/stocks");
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json({ error: "SERVER_ERROR" }, { status: res.status });
  return NextResponse.json(await res.json());
}
