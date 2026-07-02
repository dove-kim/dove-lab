import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import { decodeJwtPayload } from "@/utils/jwt";
import ModelsClient from "@/containers/root/models/ModelsClient";

export default async function ModelsPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  return (
    <AppShell>
      <ModelsClient />
    </AppShell>
  );
}
