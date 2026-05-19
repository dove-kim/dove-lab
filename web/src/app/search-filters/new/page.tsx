import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterEditorClient from "@/containers/stock-search/filters/FilterEditorClient";
import { backendFetch } from "@/services/backend";
import { StockSet, StockSetSummary } from "@/types/filter";

async function fetchStockSets(): Promise<StockSetSummary[]> {
  const res = await backendFetch("/stock-sets");
  if (!res || !res.ok) return [];
  const sets: StockSet[] = await res.json();
  return sets.map((s) => ({ id: s.id, name: s.name, codeCount: s.codes.length }));
}

export default async function NewSearchFilterPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const stockSets = await fetchStockSets();

  return (
    <AppShell>
      <FilterEditorClient stockSets={stockSets} />
    </AppShell>
  );
}
