import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const BASE = process.env.INTERNAL_API_URL ?? "http://localhost:8081";
const ACCESS_MAX_AGE = 60 * 15;
const REFRESH_MAX_AGE = 60 * 60 * 24 * 30;
const REFRESH_COOKIE_PATH = "/api/auth";

/**
 * refresh 프록시. httpOnly 쿠키의 refreshToken 을 백엔드 body 로 전달 →
 * 새 access + 새 refresh 를 받아 쿠키 갱신 (rolling refresh).
 */
export async function POST() {
  const refreshToken = (await cookies()).get("refreshToken")?.value;
  if (!refreshToken) {
    return NextResponse.json({ error: "NO_REFRESH" }, { status: 401 });
  }

  const apiRes = await fetch(`${BASE}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store",
  });

  if (!apiRes.ok) {
    const res = NextResponse.json({ error: "REFRESH_FAILED" }, { status: 401 });
    res.cookies.delete("token");
    res.cookies.set("refreshToken", "", { path: REFRESH_COOKIE_PATH, maxAge: 0 });
    return res;
  }

  const data = (await apiRes.json()) as { accessToken: string; refreshToken: string };
  const res = NextResponse.json({ ok: true });
  res.cookies.set("token", data.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: ACCESS_MAX_AGE,
  });
  res.cookies.set("refreshToken", data.refreshToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: REFRESH_COOKIE_PATH,
    maxAge: REFRESH_MAX_AGE,
  });
  return res;
}
