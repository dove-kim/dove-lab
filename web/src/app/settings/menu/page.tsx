import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import type { UserMenu } from "@/types/user";
import MenuSettingsClient from "@/containers/settings/MenuSettingsClient";

async function fetchMenu(): Promise<UserMenu> {
  const res = await backendFetch("/account/menu");
  if (!res || !res.ok) return { modules: [] };
  return res.json();
}

export default async function MenuSettingsPage() {
  if (!(await cookies()).get("token")) redirect("/login");
  const menu = await fetchMenu();
  return (
    <AppShell>
      <MenuSettingsClient menu={menu} />
    </AppShell>
  );
}
