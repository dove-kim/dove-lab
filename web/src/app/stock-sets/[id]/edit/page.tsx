import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import AppShell from "@/components/AppShell";
import StockSetEditorClient from "@/containers/stock-sets/StockSetEditorClient";
import { backendFetch } from "@/services/backend";
import { StockSet } from "@/types/filter";
import { Stock } from "@/types/stock";

async function fetchStockSet(id: string): Promise<StockSet | null> {
  const res = await backendFetch(`/api/stock-filters/${id}`);
  if (!res || !res.ok) return null;
  return res.json();
}

async function fetchStocks(): Promise<Stock[]> {
  const res = await backendFetch("/api/stocks");
  if (!res || !res.ok) return [];
  return res.json();
}

export default async function EditStockSetPage({ params }: { params: Promise<{ id: string }> }) {
  if (!(await cookies()).get("token")) redirect("/login");

  const { id } = await params;
  const [stockSet, stocks] = await Promise.all([fetchStockSet(id), fetchStocks()]);
  if (!stockSet) notFound();

  return (
    <AppShell>
      <StockSetEditorClient initial={stockSet} stocks={stocks} />
    </AppShell>
  );
}
