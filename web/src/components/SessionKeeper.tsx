"use client";

import { useEffect } from "react";

/** access token(15분)보다 짧은 주기로 갱신 — 만료 전에 미리 새 토큰을 받아 reload/리다이렉트를 없앤다. */
const REFRESH_INTERVAL_MS = 12 * 60 * 1000;

/**
 * 세션 유지 — 백그라운드에서 access token 을 만료 전 주기적으로 갱신하고, 탭 복귀 시에도 갱신한다.
 * 새로고침 없이 세션을 살아있게 유지한다(refresh token 30일 한도 내).
 */
export default function SessionKeeper() {
  useEffect(() => {
    let lastRefresh = Date.now();
    const refresh = () => {
      lastRefresh = Date.now();
      fetch("/api/auth/refresh", { method: "POST", cache: "no-store" }).catch(() => {});
    };

    const interval = setInterval(refresh, REFRESH_INTERVAL_MS);

    // 탭이 오래 백그라운드였다 복귀하면(만료 임박) 즉시 갱신.
    const onVisible = () => {
      if (document.visibilityState === "visible" && Date.now() - lastRefresh > REFRESH_INTERVAL_MS) {
        refresh();
      }
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, []);

  return null;
}
