import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { backendFetch } from "@/services/backend";
import { decodeJwtPayload } from "@/utils/jwt";
import type { SystemEventPage } from "@/types/ops";
import SystemEventsClient from "@/containers/root/ops/SystemEventsClient";

async function fetchSystemEvents(): Promise<SystemEventPage> {
  const res = await backendFetch("/admin/ops/system-events?page=0&size=10");
  if (!res || !res.ok) return { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 };
  return res.json();
}

export default async function SystemEventsPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  const initialPage = await fetchSystemEvents();
  return (
    <AppShell>
      <SystemEventsClient initialPage={initialPage} />
    </AppShell>
  );
}
