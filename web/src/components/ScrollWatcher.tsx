"use client";

import { useEffect } from "react";

/**
 * 전역 scroll 이벤트를 capture해서 <html>에 is-scrolling 클래스를 토글한다.
 * globals.css에서 이 클래스를 보고 스크롤바 thumb 크기를 확장한다.
 */
export default function ScrollWatcher() {
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    const onScroll = () => {
      document.documentElement.classList.add("is-scrolling");
      clearTimeout(timer);
      timer = setTimeout(() => {
        document.documentElement.classList.remove("is-scrolling");
      }, 400);
    };
    // capture: true → 페이지 내 모든 스크롤 가능 요소까지 감지
    window.addEventListener("scroll", onScroll, { passive: true, capture: true });
    return () => {
      window.removeEventListener("scroll", onScroll, { capture: true });
      clearTimeout(timer);
    };
  }, []);

  return null;
}
