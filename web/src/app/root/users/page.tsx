import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";
import type { UserSummary } from "@/types/user";
import RootUsersClient from "@/containers/root/RootUsersClient";

async function fetchUsers(): Promise<UserSummary[]> {
  const res = await backendFetch("/root/users");
  if (!res || !res.ok) return [];
  return res.json();
}

export default async function RootUsersPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");
  const allUsers = await fetchUsers();
  const users = allUsers.filter((u) => u.role !== "ROOT");
  return (
    <AppShell>
      <RootUsersClient users={users} />
    </AppShell>
  );
}
