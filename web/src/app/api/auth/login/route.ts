import { NextRequest, NextResponse } from "next/server";
import { parseJsonSafely } from "@/lib/api";

const REMEMBER_ME_MAX_AGE = 60 * 60 * 24 * 30; // 30일

export async function POST(req: NextRequest) {
  const body = await req.json();

  const apiRes = await fetch(`${process.env.INTERNAL_API_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!apiRes.ok) {
    return NextResponse.json(
      { message: "아이디 또는 비밀번호가 올바르지 않습니다." },
      { status: 401 }
    );
  }

  const data = await parseJsonSafely(apiRes) as { accessToken: string; username: string; name: string; role: string; rememberMe: boolean };

  const response = NextResponse.json({ username: data.username, name: data.name, role: data.role });
  response.cookies.set("token", data.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    ...(data.rememberMe ? { maxAge: REMEMBER_ME_MAX_AGE } : {}),
    path: "/",
  });

  return response;
}
