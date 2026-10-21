"use client";

import { RefObject, UIEvent, useCallback, useRef, useState } from "react";

const ENTER_THRESHOLD = 24; // 이만큼 내려간 뒤부터 숨김 판단
const DELTA_THRESHOLD = 6; // 손떨림 수준의 미세 스크롤은 무시
const SETTLE_MS = 350; // 접힘/펼침 직후, 레이아웃이 흔들리는 동안은 판단 보류

/**
 * 아래로 스크롤하면 true, 위로 스크롤하거나 맨 위면 false 를 돌려주는 훅.
 * 반환된 onScrollCapture 를 상위 요소에 달면 내부의 어떤 스크롤 영역이든 함께 반응한다.
 *
 * @param collapsibleRef 접힐 영역 — 접어서 생기는 여유만큼 스크롤할 게 없으면 접지 않는다
 */
export function useHideOnScroll(collapsibleRef: RefObject<HTMLElement | null>) {
  const [hidden, setHidden] = useState(false);
  const hiddenRef = useRef(false);
  const lastTops = useRef(new WeakMap<EventTarget, number>());
  const settleUntil = useRef(0);

  const onScrollCapture = useCallback(
    (e: UIEvent) => {
      const el = e.target;
      if (!(el instanceof HTMLElement)) return;

      const top = el.scrollTop;
      const last = lastTops.current.get(el) ?? 0;
      lastTops.current.set(el, top);

      if (Date.now() < settleUntil.current) return;

      const delta = top - last;
      if (Math.abs(delta) < DELTA_THRESHOLD) return;

      const next = top > ENTER_THRESHOLD && delta > 0;
      if (next === hiddenRef.current) return;

      // 접으면 보이는 영역이 그만큼 커진다 — 그만큼 스크롤할 게 없으면 스크롤 위치가 튀므로 접지 않는다
      if (next) {
        const collapsibleHeight = collapsibleRef.current?.offsetHeight ?? 0;
        const scrollable = el.scrollHeight - el.clientHeight;
        if (scrollable - collapsibleHeight <= ENTER_THRESHOLD) return;
      }

      hiddenRef.current = next;
      settleUntil.current = Date.now() + SETTLE_MS;
      setHidden(next);
    },
    [collapsibleRef],
  );

  /** 스크롤과 무관하게 접거나 펼친다(화면 전환 등). */
  const setCollapsed = useCallback((next: boolean) => {
    hiddenRef.current = next;
    settleUntil.current = Date.now() + SETTLE_MS;
    setHidden(next);
  }, []);

  return { hidden, onScrollCapture, setCollapsed };
}
