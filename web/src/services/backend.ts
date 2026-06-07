import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { decodeJwtPayload, type JwtPayload } from "@/utils/jwt";

const BASE = process.env.INTERNAL_API_URL ?? "http://localhost:8081";

// 백엔드 응답 상한 — 배치 등으로 API가 느릴 때 SSR/프록시가 무한 대기하지 않도록.
const TIMEOUT_MS = Number(process.env.INTERNAL_API_TIMEOUT_MS ?? 10000);

export async function backendFetch(
  path: string,
  init?: Omit<RequestInit, "cache">
): Promise<Response | null> {
  const token = (await cookies()).get("token")?.value;
  if (!token) return null;
  return fetch(`${BASE}${path}`, {
    ...init,
    headers: { ...(init?.headers ?? {}), Authorization: `Bearer ${token}` },
    cache: "no-store",
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
}

export function unauthorized() {
  const res = NextResponse.json({ error: "UNAUTHORIZED" }, { status: 401 });
  res.cookies.delete("token");
  return res;
}

export function forbidden() {
  return NextResponse.json({ error: "FORBIDDEN" }, { status: 403 });
}

/**
 * 토큰 쿠키를 읽어 역할을 검사한다. 인증된 사용자가 권한이 없으면 403 응답을, 그렇지 않으면 디코드된 페이로드를 반환한다.
 *
 * @param roles 허용 역할 목록
 * @return 권한 부족 시 403 Response, 통과 시 JWT 페이로드
 */
export async function requireRole(roles: string[]): Promise<NextResponse | JwtPayload> {
  const token = (await cookies()).get("token")?.value;
  const payload = token ? decodeJwtPayload(token) : null;
  if (!payload || !roles.includes(payload.role)) return forbidden();
  return payload;
}

export async function safeJson(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
