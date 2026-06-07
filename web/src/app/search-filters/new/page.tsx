import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterEditorClient from "@/containers/stock-search/filters/FilterEditorClient";
import { backendFetch } from "@/services/backend";

interface StockFilterSummary { id: number; name: string; scope: "SYSTEM" | "MEMBER"; }

async function fetchStockFilters(): Promise<StockFilterSummary[]> {
  const res = await backendFetch("/stock-filters/available");
  if (!res || !res.ok) return [];
  const data = await res.json();
  return data.map((f: { id: number; name: string; scope: "SYSTEM" | "MEMBER" }) => ({ id: f.id, name: f.name, scope: f.scope }));
}

export default async function NewSearchFilterPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const stockFilters = await fetchStockFilters();

  return (
    <AppShell>
      <FilterEditorClient stockFilters={stockFilters} />
    </AppShell>
  );
}
