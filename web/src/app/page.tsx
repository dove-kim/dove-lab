import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import Dashboard from "@/containers/dashboard/Dashboard";
import { decodeJwtPayload } from "@/utils/jwt";

export default async function MainPage() {
  const token = (await cookies()).get("token");
  if (!token) redirect("/login");

  const role = decodeJwtPayload(token.value)?.role ?? "";

  return (
    <AppShell>
      <Dashboard role={role} />
    </AppShell>
  );
}
