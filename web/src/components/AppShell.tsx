import { cookies } from "next/headers";
import Header from "./Header";
import ContentLayout from "./ContentLayout";

function decodeRole(token: string): string {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(Buffer.from(payload, "base64url").toString()).role ?? "";
  } catch {
    return "";
  }
}

export default async function AppShell({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("token")?.value;
  const role = token ? decodeRole(token) : "";

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950">
      <Header />
      <ContentLayout role={role}>
        {children}
      </ContentLayout>
    </div>
  );
}
