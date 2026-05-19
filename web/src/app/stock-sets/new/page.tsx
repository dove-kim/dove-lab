import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import StockSetEditorClient from "@/containers/stock-search/stock-sets/StockSetEditorClient";
import { backendFetch } from "@/services/backend";
import { Stock } from "@/types/stock";

async function fetchStocks(): Promise<Stock[]> {
  const res = await backendFetch("/stocks");
  if (!res || !res.ok) return [];
  return res.json();
}

export default async function NewStockSetPage() {
  if (!(await cookies()).get("token")) redirect("/login");

  const stocks = await fetchStocks();

  return (
    <AppShell>
      <StockSetEditorClient stocks={stocks} />
    </AppShell>
  );
}
