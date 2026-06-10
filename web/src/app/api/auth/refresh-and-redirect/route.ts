import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const BASE               = process.env.INTERNAL_API_URL ?? "http://localhost:8081";
const ACCESS_MAX_AGE     = 60 * 15;
const REFRESH_MAX_AGE    = 60 * 60 * 24 * 30;
const REFRESH_COOKIE_PATH = "/api/auth";

/**
 * 미들웨어에서 access token 만료 감지 시 경유하는 갱신·리다이렉트 엔드포인트.
 * refresh token으로 새 토큰을 발급받아 쿠키를 갱신하고 원래 경로로 돌아간다.
 * refresh 실패(토큰 없음·만료) 시 /login으로 이동한다.
 */
export async function GET(req: NextRequest) {
  const rawTo = req.nextUrl.searchParams.get("to") ?? "/";
  // open redirect 방지 — 반드시 같은 오리진의 내부 경로여야 함
  const to = rawTo.startsWith("/") && !rawTo.startsWith("//") ? rawTo : "/";

  const refreshToken = (await cookies()).get("refreshToken")?.value;
  if (!refreshToken) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  const apiRes = await fetch(`${BASE}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store",
  });

  if (!apiRes.ok) {
    const res = NextResponse.redirect(new URL("/login", req.url));
    res.cookies.delete("token");
    res.cookies.set("refreshToken", "", { path: REFRESH_COOKIE_PATH, maxAge: 0 });
    return res;
  }

  const data = (await apiRes.json()) as { accessToken: string; refreshToken: string };
  const res  = NextResponse.redirect(new URL(to, req.url));
  res.cookies.set("token", data.accessToken, {
    httpOnly: true,
    secure:   process.env.NODE_ENV === "production",
    sameSite: "strict",
    path:     "/",
    maxAge:   ACCESS_MAX_AGE,
  });
  res.cookies.set("refreshToken", data.refreshToken, {
    httpOnly: true,
    secure:   process.env.NODE_ENV === "production",
    sameSite: "strict",
    path:     REFRESH_COOKIE_PATH,
    maxAge:   REFRESH_MAX_AGE,
  });
  return res;
}
