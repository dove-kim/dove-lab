import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import StockSetListClient from "@/containers/stock-search/stock-sets/StockSetListClient";
import { backendFetch } from "@/services/backend";
import { StockSet } from "@/types/filter";

async function fetchStockSets(): Promise<StockSet[]> {
  const res = await backendFetch("/stock-sets");
  if (!res || !res.ok) return [];
  return res.json();
}

export default async function StockSetsPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const stockSets = await fetchStockSets();

  return (
    <AppShell>
      <StockSetListClient stockSets={stockSets} />
    </AppShell>
  );
}
