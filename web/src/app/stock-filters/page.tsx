import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { decodeJwtPayload } from "@/utils/jwt";
import StockFiltersClient from "@/containers/stock-filters/StockFiltersClient";

export default async function StockFiltersPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token) redirect("/login");
  const payload = decodeJwtPayload(token);
  return (
    <AppShell>
      <StockFiltersClient role={payload?.role ?? "USER"} />
    </AppShell>
  );
}
