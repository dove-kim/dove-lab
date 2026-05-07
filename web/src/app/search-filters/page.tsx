import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterListClient from "@/containers/search-filters/FilterListClient";
import { backendFetch } from "@/services/backend";
import { SearchFilter, StockSet, StockSetSummary } from "@/types/filter";

async function fetchFilters(): Promise<SearchFilter[]> {
  const res = await backendFetch("/api/filters");
  if (!res || !res.ok) return [];
  return res.json();
}

async function fetchStockSets(): Promise<StockSetSummary[]> {
  const res = await backendFetch("/api/stock-filters");
  if (!res || !res.ok) return [];
  const sets: StockSet[] = await res.json();
  return sets.map((s) => ({ id: s.id, name: s.name, codeCount: s.codes.length }));
}

export default async function SearchFiltersPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const [filters, stockSets] = await Promise.all([fetchFilters(), fetchStockSets()]);

  return (
    <AppShell>
      <FilterListClient filters={filters} stockSets={stockSets} />
    </AppShell>
  );
}
