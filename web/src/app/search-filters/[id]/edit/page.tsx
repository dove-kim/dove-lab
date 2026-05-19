import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import AppShell from "@/components/AppShell";
import FilterEditorClient from "@/containers/stock-search/filters/FilterEditorClient";
import { backendFetch } from "@/services/backend";
import { SearchFilter, StockSet, StockSetSummary } from "@/types/filter";

async function fetchFilter(id: string): Promise<SearchFilter | null> {
  const res = await backendFetch("/filters");
  if (!res || !res.ok) return null;
  const filters: SearchFilter[] = await res.json();
  return filters.find((f) => f.id === parseInt(id)) ?? null;
}

async function fetchStockSets(): Promise<StockSetSummary[]> {
  const res = await backendFetch("/stock-sets");
  if (!res || !res.ok) return [];
  const sets: StockSet[] = await res.json();
  return sets.map((s) => ({ id: s.id, name: s.name, codeCount: s.codes.length }));
}

export default async function EditSearchFilterPage({ params }: { params: Promise<{ id: string }> }) {
  if (!(await cookies()).get("token")) redirect("/login");

  const { id } = await params;
  const [filter, stockSets] = await Promise.all([fetchFilter(id), fetchStockSets()]);
  if (!filter) notFound();

  return (
    <AppShell>
      <FilterEditorClient initial={filter} stockSets={stockSets} />
    </AppShell>
  );
}
