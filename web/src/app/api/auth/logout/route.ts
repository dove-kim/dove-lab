import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const BASE = process.env.INTERNAL_API_URL ?? "http://localhost:8081";
const REFRESH_COOKIE_PATH = "/api/auth";

/**
 * 로그아웃. 백엔드에 cutoff 설정 요청(현재 access token 무효화) 후 쿠키 제거.
 */
export async function POST() {
  const token = (await cookies()).get("token")?.value;
  if (token) {
    try {
      await fetch(`${BASE}/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      });
    } catch {
      // 백엔드 실패해도 클라이언트 쿠키는 제거
    }
  }
  const res = NextResponse.json({ ok: true });
  res.cookies.delete("token");
  res.cookies.set("refreshToken", "", { path: REFRESH_COOKIE_PATH, maxAge: 0 });
  return res;
}
