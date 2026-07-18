import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import AppShell from "@/components/AppShell";

/** 포트폴리오 하위 화면 공통 셸 — 인증 확인 + 컨테이너. */
export default async function PortfolioSectionLayout({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("token")?.value;
  if (!token) redirect("/login");
  return (
    <AppShell>
      <div className="flex-1 overflow-y-auto p-4 sm:p-6">
        <div className="max-w-6xl mx-auto w-full">{children}</div>
      </div>
    </AppShell>
  );
}
