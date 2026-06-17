import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import Header from "./Header";
import ContentLayout from "./ContentLayout";
import { decodeJwtPayload } from "@/utils/jwt";

export default async function AppShell({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("token")?.value;
  const payload = token ? decodeJwtPayload(token) : null;

  // 토큰이 있는데 만료/디코드 불가면 재발급 경로로.
  if (token && (!payload || payload.exp * 1000 < Date.now())) {
    const pathname = (await headers()).get("x-pathname") ?? "/";
    redirect(`/api/auth/refresh-and-redirect?to=${encodeURIComponent(pathname)}`);
  }

  const role = payload?.role ?? "";
  const mustChangePassword = payload?.mustChangePassword ?? false;
  const capabilities = payload?.capabilities ?? [];

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950">
      <Header />
      <ContentLayout role={role} capabilities={capabilities} mustChangePassword={mustChangePassword}>
        {children}
      </ContentLayout>
    </div>
  );
}
