import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";
import type { UserSummary } from "@/types/user";
import AdminUsersClient from "@/containers/admin/AdminUsersClient";

async function fetchUsers(): Promise<UserSummary[]> {
  const res = await backendFetch("/admin/users");
  if (!res || !res.ok) return [];
  return res.json();
}

export default async function AdminUsersPage() {
  const token = (await cookies()).get("token")?.value;
  const role = token ? decodeJwtPayload(token)?.role : null;
  if (!token || (role !== "ADMIN" && role !== "ROOT")) redirect("/");
  const allUsers = await fetchUsers();
  const users = allUsers.filter((u) => u.role !== "ROOT");
  return (
    <AppShell>
      <AdminUsersClient users={users} />
    </AppShell>
  );
}
