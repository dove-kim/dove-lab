import { NextRequest, NextResponse } from "next/server";

/**
 * 프록시가 넘긴 실제 호스트 기준으로 절대 URL 리다이렉트 응답을 만든다.
 *
 * standalone 서버는 req.url 호스트를 바인딩 주소(0.0.0.0)로 잡으므로,
 * X-Forwarded-Host / Host 헤더의 실제 호스트로 URL을 구성한다.
 *
 * @param req  현재 요청 (호스트·프로토콜 헤더 출처)
 * @param path "/"로 시작하는 내부 경로(쿼리 포함 가능)
 */
export function redirectTo(req: NextRequest, path: string): NextResponse {
  const host = req.headers.get("x-forwarded-host") ?? req.headers.get("host") ?? req.nextUrl.host;
  const proto = req.headers.get("x-forwarded-proto") ?? req.nextUrl.protocol.replace(":", "");
  return NextResponse.redirect(new URL(path, `${proto}://${host}`));
}
