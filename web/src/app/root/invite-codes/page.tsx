import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import InviteCodesClient from "@/containers/root/InviteCodesClient";
import { decodeJwtPayload } from "@/utils/jwt";

export default async function RootInviteCodesPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeJwtPayload(token)?.role !== "ROOT") redirect("/");

  return (
    <AppShell>
      <InviteCodesClient />
    </AppShell>
  );
}
