import { NextRequest, NextResponse } from "next/server";

const PUBLIC_PATHS = ["/login", "/register"];

const BOT_UA_PATTERN =
  /bot|crawl|spider|slurp|mediapartners|facebookexternalhit|linkedinbot|twitterbot|whatsapp|telegram|discordbot|applebot|duckduckbot|baiduspider|yandex/i;

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString());
    return typeof payload.exp === "number" && payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export function middleware(req: NextRequest) {
  const ua = req.headers.get("user-agent") ?? "";
  if (BOT_UA_PATTERN.test(ua)) {
    return new NextResponse(null, { status: 403 });
  }

  const tokenCookie = req.cookies.get("token");
  const valid = tokenCookie != null && isTokenValid(tokenCookie.value);
  const isPublic = PUBLIC_PATHS.some((p) => req.nextUrl.pathname.startsWith(p));

  if (!valid && !isPublic) {
    const res = NextResponse.redirect(new URL("/login", req.url));
    if (tokenCookie) res.cookies.delete("token");
    return res;
  }
  if (valid && req.nextUrl.pathname === "/login") {
    return NextResponse.redirect(new URL("/", req.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
