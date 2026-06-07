import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { decodeJwtPayload } from "@/utils/jwt";
import BackfillClient from "@/containers/root/ops/BackfillClient";

export default async function BackfillPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  return (
    <AppShell>
      <BackfillClient />
    </AppShell>
  );
}
