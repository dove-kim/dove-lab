import { NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function GET() {
  const res = await backendFetch("/market/available");
  if (!res) return unauthorized();
  if (!res.ok) return NextResponse.json([], { status: res.status });
  return NextResponse.json(await res.json());
}
