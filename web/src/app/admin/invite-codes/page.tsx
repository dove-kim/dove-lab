import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import InviteCodesClient from "@/containers/admin/InviteCodesClient";
import { decodeJwtPayload } from "@/utils/jwt";

export default async function InviteCodesPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ADMIN") redirect("/");

  return (
    <AppShell>
      <InviteCodesClient />
    </AppShell>
  );
}
