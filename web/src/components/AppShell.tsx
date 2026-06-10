import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import Header from "./Header";
import ContentLayout from "./ContentLayout";
import { decodeJwtPayload } from "@/utils/jwt";
import { backendFetch } from "@/services/backend";
import type { UserMenu } from "@/types/user";

async function fetchMenu(): Promise<UserMenu | "unauthorized"> {
  try {
    const res = await backendFetch("/account/menu");
    if (!res) return { modules: [] };
    if (res.status === 401) return "unauthorized";
    if (!res.ok) return { modules: [] };
    return res.json();
  } catch {
    return { modules: [] };
  }
}

export default async function AppShell({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("token")?.value;
  const payload = token ? decodeJwtPayload(token) : null;
  const role = payload?.role ?? "";
  const mustChangePassword = payload?.mustChangePassword ?? false;

  let menu: UserMenu = { modules: [] };
  if (token) {
    const result = await fetchMenu();
    if (result === "unauthorized") {
      const pathname = (await headers()).get("x-pathname") ?? "/";
      redirect(`/api/auth/refresh-and-redirect?to=${encodeURIComponent(pathname)}`);
    }
    menu = result;
  }

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950">
      <Header />
      <ContentLayout role={role} menu={menu} mustChangePassword={mustChangePassword}>
        {children}
      </ContentLayout>
    </div>
  );
}

