import { NextResponse } from "next/server";

/**
 * 같은 오리진 내부 경로로 상대 Location 리다이렉트 응답을 만든다.
 *
 * standalone 서버가 절대 URL 호스트를 바인딩 주소(0.0.0.0)로 잡는 문제를 피한다.
 *
 * @param path "/"로 시작하는 내부 경로(쿼리 포함 가능)
 */
export function relativeRedirect(path: string): NextResponse {
  const res = new NextResponse(null, { status: 307 });
  res.headers.set("Location", path);
  return res;
}
