import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { decodeJwtPayload } from "@/utils/jwt";
import CustomMetricsClient from "@/containers/root/custom-metrics/CustomMetricsClient";

export default async function CustomMetricsPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  return (
    <AppShell>
      <CustomMetricsClient />
    </AppShell>
  );
}
