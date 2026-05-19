import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, unauthorized } from "@/services/backend";

export async function PATCH(req: NextRequest) {
  const body = await req.json();
  const res = await backendFetch("/account/password", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  }

  const data = await res.json();
  const cookieStore = await cookies();
  const existing = cookieStore.get("token");

  const response = NextResponse.json({ ok: true });
  response.cookies.set("token", data.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    ...(existing?.value ? {} : { maxAge: 60 * 60 }),
    path: "/",
  });
  return response;
}
