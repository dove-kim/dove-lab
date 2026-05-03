import { NextRequest, NextResponse } from "next/server";
import { parseJsonSafely } from "@/lib/api";

function authHeaders(req: NextRequest) {
  const token = req.cookies.get("token")?.value;
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function GET(req: NextRequest) {
  const apiRes = await fetch(`${process.env.INTERNAL_API_URL}/api/admin/invite-codes`, {
    headers: authHeaders(req),
    cache: "no-store",
  });
  return NextResponse.json(await parseJsonSafely(apiRes), { status: apiRes.status });
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const apiRes = await fetch(`${process.env.INTERNAL_API_URL}/api/admin/invite-codes`, {
    method: "POST",
    headers: authHeaders(req),
    body: JSON.stringify(body),
  });
  return NextResponse.json(await parseJsonSafely(apiRes), { status: apiRes.status });
}
