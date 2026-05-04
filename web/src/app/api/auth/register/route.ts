import { NextRequest, NextResponse } from "next/server";
import { parseJsonSafely } from "@/lib/api";

export async function POST(req: NextRequest) {
  const body = await req.json();

  const apiRes = await fetch(`${process.env.INTERNAL_API_URL}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await parseJsonSafely(apiRes) as { detail?: string; username?: string; name?: string; role?: string; accessToken?: string };

  if (!apiRes.ok) {
    return NextResponse.json(
      { code: data.detail ?? "UNKNOWN_ERROR" },
      { status: apiRes.status }
    );
  }

  const response = NextResponse.json({ username: data.username, name: data.name, role: data.role }, { status: 201 });
  response.cookies.set("token", data.accessToken!, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
  });

  return response;
}
