import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";
import type { StockTagsResponse } from "@/types/stock-tag";
import StockTagsClient from "@/containers/root/ops/StockTagsClient";

async function fetchStockTags(): Promise<StockTagsResponse | null> {
  const res = await backendFetch("/stock-tags");
  if (!res || !res.ok) return null;
  return res.json();
}

export default async function StockTagsPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  const tags = await fetchStockTags();
  return (
    <AppShell>
      <StockTagsClient initial={tags} />
    </AppShell>
  );
}
