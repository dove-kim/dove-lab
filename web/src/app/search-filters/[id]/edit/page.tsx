import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterEditorClient from "@/containers/stock-search/filters/FilterEditorClient";
import { backendFetch } from "@/services/backend";
import { SearchFilter } from "@/types/filter";

interface StockFilterSummary { id: number; name: string; scope: "SYSTEM" | "MEMBER"; }

async function fetchFilter(id: string): Promise<SearchFilter | null> {
  const res = await backendFetch("/filters");
  if (!res || !res.ok) return null;
  const filters: SearchFilter[] = await res.json();
  return filters.find((f) => f.id === parseInt(id)) ?? null;
}

async function fetchStockFilters(): Promise<StockFilterSummary[]> {
  const res = await backendFetch("/stock-filters/available");
  if (!res || !res.ok) return [];
  const data = await res.json();
  return data.map((f: { id: number; name: string; scope: "SYSTEM" | "MEMBER" }) => ({ id: f.id, name: f.name, scope: f.scope }));
}

export default async function EditSearchFilterPage({ params }: { params: Promise<{ id: string }> }) {
  if (!(await cookies()).get("token")) redirect("/login");

  const { id } = await params;
  const [filter, stockFilters] = await Promise.all([fetchFilter(id), fetchStockFilters()]);
  if (!filter) notFound();

  return (
    <AppShell>
      <FilterEditorClient initial={filter} stockFilters={stockFilters} />
    </AppShell>
  );
}
