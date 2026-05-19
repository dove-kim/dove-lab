import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  // 개발 환경에서 React StrictMode의 이중 마운트를 비활성화.
  // StrictMode는 mount→unmount→remount를 반복해 API 호출이 2배로 발생하고
  // Next.js dev server route handler 큐 대기로 7초+ 지연이 생긴다.
  // 프로덕션 빌드에는 영향 없음.
  reactStrictMode: false,
};

export default nextConfig;
