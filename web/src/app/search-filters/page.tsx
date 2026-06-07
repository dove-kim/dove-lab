import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterListClient from "@/containers/stock-search/filters/FilterListClient";
import { backendFetch } from "@/services/backend";
import { SearchFilter } from "@/types/filter";

interface StockFilterSummary { id: number; name: string; scope: "SYSTEM" | "MEMBER"; }

async function fetchFilters(): Promise<SearchFilter[]> {
  const res = await backendFetch("/filters");
  if (!res || !res.ok) return [];
  return res.json();
}

async function fetchStockFilters(): Promise<StockFilterSummary[]> {
  const res = await backendFetch("/stock-filters/available");
  if (!res || !res.ok) return [];
  const data = await res.json();
  return data.map((f: { id: number; name: string; scope: "SYSTEM" | "MEMBER" }) => ({ id: f.id, name: f.name, scope: f.scope }));
}

export default async function SearchFiltersPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const [filters, stockFilters] = await Promise.all([fetchFilters(), fetchStockFilters()]);

  return (
    <AppShell>
      <FilterListClient filters={filters} stockFilters={stockFilters} />
    </AppShell>
  );
}
