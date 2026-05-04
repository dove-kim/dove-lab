import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import MainLayout from "@/components/main/MainLayout";
import { Stock } from "@/types/stock";

async function fetchStocks(): Promise<Stock[]> {
  try {
    const res = await fetch(`${process.env.INTERNAL_API_URL}/api/stocks`, {
      cache: "no-store",
    });
    if (!res.ok) return [];
    return res.json();
  } catch {
    return [];
  }
}

export default async function StocksPage() {
  const token = (await cookies()).get("token");
  if (!token) redirect("/login");

  const stocks = await fetchStocks();

  return (
    <AppShell>
      <MainLayout stocks={stocks} />
    </AppShell>
  );
}
