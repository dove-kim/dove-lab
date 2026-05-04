"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";

const ERROR_MESSAGES: Record<string, string> = {
  INVITE_CODE_INVALID: "유효하지 않은 초대 코드입니다.",
  USERNAME_DUPLICATE: "이미 사용 중인 아이디입니다.",
  EMAIL_DUPLICATE: "이미 사용 중인 이메일입니다.",
};

function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [form, setForm] = useState({ inviteCode: searchParams.get("code") ?? "", username: "", password: "", email: "", name: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    if (name === "username") {
      setForm((f) => ({ ...f, username: value.replace(/[^a-z0-9]/g, "") }));
    } else {
      setForm((f) => ({ ...f, [name]: value }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      if (!res.ok) {
        const data = await res.json();
        setError(ERROR_MESSAGES[data.code] ?? "회원가입에 실패했습니다.");
        return;
      }
      router.push("/");
      router.refresh();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950 px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-white tracking-wide">주식 딸깍이</h1>
          <p className="text-slate-400 text-sm mt-1">초대 코드로 계정을 만들어보세요</p>
        </div>

        <div className="bg-white/5 backdrop-blur-md border border-white/10 rounded-2xl p-8 shadow-2xl">
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">

            {/* 초대 코드 */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-slate-400 font-medium">초대 코드</label>
              <input
                name="inviteCode"
                value={form.inviteCode}
                onChange={handleChange}
                placeholder="초대 코드를 입력하세요"
                required
                className="px-4 py-2.5 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
              />
            </div>

            {/* 아이디 */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-slate-400 font-medium">아이디</label>
              <input
                name="username"
                value={form.username}
                onChange={handleChange}
                placeholder="영어 소문자, 숫자 (3자 이상)"
                required
                minLength={3}
                className="px-4 py-2.5 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
              />
            </div>

            {/* 비밀번호 */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-slate-400 font-medium">비밀번호</label>
              <div className="relative">
                <input
                  name="password"
                  type={showPassword ? "text" : "password"}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="4자 이상"
                  required
                  minLength={4}
                  className="w-full px-4 py-2.5 pr-10 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition cursor-pointer"
                >
                  {showPassword ? (
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {/* 이메일 */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-slate-400 font-medium">이메일</label>
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                placeholder="example@email.com"
                required
                className="px-4 py-2.5 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
              />
            </div>

            {/* 이름 */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-slate-400 font-medium">이름</label>
              <input
                name="name"
                value={form.name}
                onChange={handleChange}
                placeholder="표시될 이름"
                required
                className="px-4 py-2.5 rounded-lg bg-white/8 border border-white/15 text-white placeholder-white/25 outline-none focus:ring-2 focus:ring-indigo-400/50 focus:border-transparent transition text-sm"
              />
            </div>

            {error && <p className="text-red-400 text-xs text-center">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="mt-2 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm transition disabled:opacity-50 cursor-pointer"
            >
              {loading ? "처리 중..." : "가입하기"}
            </button>
          </form>

          <p className="text-center text-slate-500 text-xs mt-5">
            이미 계정이 있으신가요?{" "}
            <Link href="/login" className="text-indigo-400 hover:text-indigo-300 transition">
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default function RegisterPage() {
  return (
    <Suspense>
      <RegisterForm />
    </Suspense>
  );
}
