import { cookies } from "next/headers";
import Header from "./Header";
import ContentLayout from "./ContentLayout";
import { decodeJwtPayload } from "@/utils/jwt";
import { backendFetch } from "@/services/backend";
import type { UserMenu } from "@/types/user";

async function fetchMenu(): Promise<UserMenu> {
  try {
    const res = await backendFetch("/account/menu");
    if (!res || !res.ok) return { modules: [] };
    return res.json();
  } catch {
    // 백엔드 지연/타임아웃 시 빈 메뉴로 degrade — 페이지가 멈추지 않게.
    return { modules: [] };
  }
}

export default async function AppShell({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("token")?.value;
  const payload = token ? decodeJwtPayload(token) : null;
  const role = payload?.role ?? "";
  const mustChangePassword = payload?.mustChangePassword ?? false;
  const menu = token ? await fetchMenu() : { modules: [] };

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950">
      <Header />
      <ContentLayout role={role} menu={menu} mustChangePassword={mustChangePassword}>
        {children}
      </ContentLayout>
    </div>
  );
}

