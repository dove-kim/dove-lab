import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";
import InviteCodesClient from "./InviteCodesClient";

function decodeRole(token: string): string {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(Buffer.from(payload, "base64url").toString()).role ?? "";
  } catch {
    return "";
  }
}

export default async function InviteCodesPage() {
  const token = (await cookies()).get("token")?.value;
  if (!token || decodeRole(token) !== "ADMIN") redirect("/");

  return (
    <AppShell>
      <InviteCodesClient />
    </AppShell>
  );
}
