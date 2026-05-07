import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import Dashboard from "@/containers/dashboard/Dashboard";

export default async function MainPage() {
  const token = (await cookies()).get("token");
  if (!token) redirect("/login");

  return (
    <AppShell>
      <Dashboard />
    </AppShell>
  );
}
