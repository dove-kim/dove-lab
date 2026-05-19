import { NextRequest, NextResponse } from "next/server";

const PUBLIC_PATHS = ["/login", "/register"];

const BOT_UA_PATTERN =
  /bot|crawl|spider|slurp|mediapartners|facebookexternalhit|linkedinbot|twitterbot|whatsapp|telegram|discordbot|applebot|duckduckbot|baiduspider|yandex/i;

// ─── Rate Limiting ────────────────────────────────────────────────────────────
// 단일 서버 인메모리 방식. 분산 환경이 필요하면 Upstash Redis로 교체.

type RateLimitEntry = { count: number; resetAt: number };
const rateLimitStore = new Map<string, RateLimitEntry>();

const RATE_LIMITS = {
  auth:    { max: 10,  windowMs: 60_000 },  // 로그인·회원가입: 분당 10회
  default: { max: 120, windowMs: 60_000 },  // 일반 페이지: 분당 120회
};

function checkRateLimit(ip: string, type: keyof typeof RATE_LIMITS): boolean {
  const now = Date.now();
  const key = `${type}:${ip}`;
  const limit = RATE_LIMITS[type];
  const entry = rateLimitStore.get(key);

  if (!entry || now > entry.resetAt) {
    rateLimitStore.set(key, { count: 1, resetAt: now + limit.windowMs });
    return true;
  }
  if (entry.count >= limit.max) return false;
  entry.count++;
  return true;
}

// 오래된 항목 주기적 정리 (메모리 누수 방지)
setInterval(() => {
  const now = Date.now();
  for (const [key, entry] of rateLimitStore) {
    if (now > entry.resetAt) rateLimitStore.delete(key);
  }
}, 5 * 60_000);

// ─── Token ────────────────────────────────────────────────────────────────────

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString());
    return typeof payload.exp === "number" && payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

// ─── Middleware ───────────────────────────────────────────────────────────────

export function middleware(req: NextRequest) {
  // 1. 봇 차단
  const ua = req.headers.get("user-agent") ?? "";
  if (BOT_UA_PATTERN.test(ua)) {
    return new NextResponse(null, { status: 403 });
  }

  // 2. Rate Limiting
  const ip =
    req.headers.get("x-forwarded-for")?.split(",")[0].trim() ??
    req.headers.get("x-real-ip") ??
    "unknown";
  const isAuthPath = PUBLIC_PATHS.some((p) => req.nextUrl.pathname.startsWith(p));
  const limitType = isAuthPath ? "auth" : "default";

  if (!checkRateLimit(ip, limitType)) {
    return new NextResponse(null, { status: 429 });
  }

  // 3. 인증 검사
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
