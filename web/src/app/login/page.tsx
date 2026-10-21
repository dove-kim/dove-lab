"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { EyeIcon, EyeOffIcon } from "lucide-react";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUsername(e.target.value.replace(/[^a-z0-9]/g, ""));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password, rememberMe }),
      });

      if (res.ok) {
        router.push("/");
        router.refresh();
      } else {
        setError("아이디 또는 비밀번호가 올바르지 않습니다.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-gradient-to-br from-gray-950 via-slate-900 to-indigo-950 flex items-center justify-center">
      <div className="w-full max-w-sm mx-4 bg-white/8 backdrop-blur-md border border-white/10 rounded-2xl shadow-2xl p-8">
        <h1 className="text-2xl font-semibold text-white text-center mb-1 tracking-wide">
          도브 연구실
        </h1>
        <p className="text-sm text-slate-400 text-center mb-8">
          계속하려면 로그인하세요
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-slate-300">아이디</label>
            <Input
              type="text"
              value={username}
              onChange={handleUsernameChange}
              placeholder="영문 소문자, 숫자만 입력"
              required
              autoComplete="username"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-slate-300">비밀번호</label>
            <div className="relative">
              <Input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호를 입력하세요"
                required
                autoComplete="current-password"
                className="pr-10"
              />
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200"
                tabIndex={-1}
                aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
              >
                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
              </Button>
            </div>
          </div>

          <label className="group flex cursor-pointer items-center gap-2.5 select-none">
            <Checkbox checked={rememberMe} onCheckedChange={(v) => setRememberMe(v === true)} />
            <span className="text-sm text-slate-400 transition group-hover:text-slate-300">로그인 유지</span>
          </label>

          {error && (
            <p className="text-sm text-red-400 text-center">{error}</p>
          )}

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? "로그인 중..." : "로그인"}
          </Button>
        </form>
      </div>
    </main>
  );
}

