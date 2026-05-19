import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const BASE = process.env.INTERNAL_API_URL ?? "http://localhost:8081";

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
  });
}

export function unauthorized() {
  const res = NextResponse.json({ error: "UNAUTHORIZED" }, { status: 401 });
  res.cookies.delete("token");
  return res;
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
