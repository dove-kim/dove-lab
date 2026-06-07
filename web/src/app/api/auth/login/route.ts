import { NextRequest, NextResponse } from "next/server";
import { safeJson } from "@/services/backend";

const ACCESS_MAX_AGE = 60 * 15;          // 15분 (access token TTL)
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30; // 30일 (refresh token TTL)
const REFRESH_COOKIE_PATH = "/api/auth";

export async function POST(req: NextRequest) {
  const body = await req.json();

  const apiRes = await fetch(`${process.env.INTERNAL_API_URL ?? "http://localhost:8081"}/auth/login`, {
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

  const data = await safeJson(apiRes) as {
    accessToken: string;
    refreshToken: string;
    username: string;
    name: string;
    role: string;
    rememberMe: boolean;
  };

  const response = NextResponse.json({ username: data.username, name: data.name, role: data.role });
  // access token (15분, 전체 경로)
  response.cookies.set("token", data.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: ACCESS_MAX_AGE,
  });
  // refresh token (30일, /api/auth 경로에서만 전송)
  response.cookies.set("refreshToken", data.refreshToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: REFRESH_COOKIE_PATH,
    maxAge: REFRESH_MAX_AGE,
  });

  return response;
}
