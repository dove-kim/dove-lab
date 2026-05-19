import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import StockSearchLayout from "@/containers/stock-search/main/StockSearchLayout";
import { backendFetch } from "@/services/backend";
import { SearchFilter } from "@/types/filter";

interface TradingDaysResponse {
  latestDate: string;
  tradingDays: string[];
}

async function fetchFilters(): Promise<SearchFilter[]> {
  const res = await backendFetch("/filters");
  if (!res || !res.ok) return [];
  return res.json();
}

async function fetchTradingDays(): Promise<TradingDaysResponse> {
  const fallback = { latestDate: new Date().toISOString().slice(0, 10), tradingDays: [] };
  const res = await backendFetch("/market/trading-days?limit=500");
  if (!res || !res.ok) return fallback;
  return res.json();
}

export default async function StockSearchPage({
  searchParams,
}: {
  searchParams: Promise<{ filterId?: string }>;
}) {
  if (!(await cookies()).get("token")) redirect("/login");

  const { filterId } = await searchParams;
  const initialFilterId = filterId ? parseInt(filterId) : null;

  const [filters, tradingDaysData] = await Promise.all([fetchFilters(), fetchTradingDays()]);

  return (
    <AppShell>
      <StockSearchLayout
        filters={filters}
        tradingDays={tradingDaysData.tradingDays}
        latestDate={tradingDaysData.latestDate}
        initialFilterId={initialFilterId}
      />
    </AppShell>
  );
}
