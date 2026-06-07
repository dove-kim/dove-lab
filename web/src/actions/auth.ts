"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

const BASE = process.env.INTERNAL_API_URL ?? "http://localhost:8081";
const REFRESH_COOKIE_PATH = "/api/auth";

/**
 * 로그아웃 server action.
 *
 * <p>현재 access token 으로 백엔드 cutoff 설정(이전 세션의 access 즉시 401) 후
 * httpOnly 쿠키 두 개(token, refreshToken) 모두 제거. 마지막에 /login 으로 리다이렉트.
 */
export async function logout() {
  const cookieStore = await cookies();
  const token = cookieStore.get("token")?.value;
  if (token) {
    try {
      await fetch(`${BASE}/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      });
    } catch {
      // 백엔드 실패해도 쿠키는 정리
    }
  }
  cookieStore.delete("token");
  cookieStore.set("refreshToken", "", { path: REFRESH_COOKIE_PATH, maxAge: 0 });
  redirect("/login");
}
