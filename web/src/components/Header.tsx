import { cookies } from "next/headers";
import { logout } from "@/actions/auth";
import MobileMenuButton from "./MobileMenuButton";

interface JwtPayload {
  sub: string;
  name: string;
  role: string;
}

function decodeTokenPayload(token: string): JwtPayload {
  const payload = token.split(".")[1];
  return JSON.parse(Buffer.from(payload, "base64url").toString());
}

export default async function Header() {
  const token = (await cookies()).get("token")?.value;
  const user = token ? decodeTokenPayload(token) : null;

  return (
    <header className="w-full px-6 py-4 bg-slate-900/80 backdrop-blur-md border-b border-white/10 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <MobileMenuButton />
        <span className="text-white font-semibold tracking-wide">주식 딸깍이</span>
      </div>
      {user && (
        <div className="flex items-center gap-4">
          <span className="text-slate-300 text-sm">{user.name}</span>
          <form action={logout}>
            <button
              type="submit"
              className="text-sm text-slate-400 hover:text-white border border-white/20 hover:border-white/40 px-3 py-1 rounded-lg transition cursor-pointer"
            >
              로그아웃
            </button>
          </form>
        </div>
      )}
    </header>
  );
}
