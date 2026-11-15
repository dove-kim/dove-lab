import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";
import type { FxRateOverview } from "@/types/ops";
import FxRatesClient from "@/containers/root/ops/FxRatesClient";

async function fetchOverview(): Promise<FxRateOverview> {
  const res = await backendFetch("/admin/ops/fx?days=30");
  if (!res || !res.ok) return { backfillStartDate: "", coverages: [], recent: [] };
  return res.json();
}

export default async function FxRatesPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  const initial = await fetchOverview();
  return (
    <AppShell>
      <FxRatesClient initial={initial} />
    </AppShell>
  );
}
