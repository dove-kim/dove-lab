import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import AllStocksLayout from "@/containers/stock-browse/AllStocksLayout";

export default async function AllStocksPage() {
  if (!(await cookies()).get("token")) redirect("/login");
  return (
    <AppShell>
      <AllStocksLayout />
    </AppShell>
  );
}
