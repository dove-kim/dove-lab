import Link from "next/link";
import { cookies } from "next/headers";
import { logout } from "@/actions/auth";
import MobileMenuButton from "./MobileMenuButton";
import { decodeJwtPayload } from "@/utils/jwt";
import { cx } from "@/utils/cx";

export default async function Header() {
  const token = (await cookies()).get("token")?.value;
  const user = token ? decodeJwtPayload(token) : null;

  return (
    <header className="w-full px-6 py-4 bg-slate-900/80 backdrop-blur-md border-b border-white/10 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <MobileMenuButton />
        <Link
          href="/"
          className="text-white font-semibold tracking-wide hover:text-indigo-300 transition"
        >
          도브 연구실
        </Link>
      </div>
      {user && (
        <div className="flex items-center gap-4">
          <span className="text-slate-300 text-sm">{user.name}</span>
          <form action={logout}>
            <button
              type="submit"
              className={cx.btnSm + " px-3 py-1 text-sm"}
            >
              로그아웃
            </button>
          </form>
        </div>
      )}
    </header>
  );
}

