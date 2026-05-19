import { useEffect, useRef } from "react";
import { PAD, MAX_BAR_SLOT, MAX_VISIBLE, PRICE_BOT, clamp } from "./chartConstants";

interface Params {
  containerRef:        React.RefObject<HTMLDivElement | null>;
  vcRef:               React.MutableRefObject<number>;
  riRef:               React.MutableRefObject<number>;
  widthRef:            React.MutableRefObject<number>;
  totalRef:            React.MutableRefObject<number>;
  setVisibleCount:     (n: number) => void;
  setRightIndex:       (n: number) => void;
  setHoverIdx:         (n: number | null) => void;
  /** 스크롤/줌 중 React 리렌더 없이 canvas를 직접 다시 그리는 트리거 */
  triggerStaticDrawRef: React.MutableRefObject<() => void>;
}

type TouchMode = "undecided" | "pan" | "crosshair" | "two-finger";

interface TouchState {
  mode:      TouchMode;
  startX?:   number;
  startY?:   number;
  startRi?:  number;
  prevDist?: number;
  prevMidX?: number;
  timer?:    ReturnType<typeof setTimeout>;
}

export function useChartInteraction({
  containerRef, vcRef, riRef, widthRef, totalRef,
  setVisibleCount, setRightIndex, setHoverIdx, triggerStaticDrawRef,
}: Params) {
  const touchRef   = useRef<TouchState | null>(null);
  // rAF 핸들 — 스크롤/줌 시 중복 draw 방지
  const drawRafRef = useRef(0);

  /** ref 갱신 후 React 리렌더 없이 직접 canvas를 다시 그린다 */
  function scheduleDraw() {
    cancelAnimationFrame(drawRafRef.current);
    drawRafRef.current = requestAnimationFrame(() => {
      triggerStaticDrawRef.current();
    });
  }

  /** touchEnd / 휠 완료 시 스크롤바 등 React 상태를 한 번만 동기화한다 */
  function syncReactState() {
    setRightIndex(riRef.current);
    setVisibleCount(vcRef.current);
  }

  function xToBarIdx(clientX: number): number | null {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return null;
    const x = clientX - rect.left;
    const plotW = widthRef.current - PAD.left - PAD.right;
    if (plotW <= 0) return null;
    const startIdx = Math.max(0, riRef.current - vcRef.current + 1);
    const n = riRef.current + 1 - startIdx;
    if (n <= 0) return null;
    const slot = plotW / n;
    const idx = Math.floor((x - PAD.left) / slot);
    if (idx < 0 || idx >= n) return null;
    return idx;
  }

  // 휠: 수평 → 팬, 수직 → 줌
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      const total = totalRef.current;
      if (total === 0) return;
      const rect = el.getBoundingClientRect();
      if (e.clientY - rect.top > PRICE_BOT) return;
      e.preventDefault();
      const vc = vcRef.current;
      const ri = riRef.current;
      if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) {
        const step  = Math.max(1, Math.round(vc * 0.08));
        const newRi = clamp(ri + Math.sign(e.deltaX) * step, vc - 1, total - 1);
        riRef.current = newRi;
      } else {
        const plotW      = widthRef.current - PAD.left - PAD.right;
        const minVisible = Math.max(2, Math.ceil(plotW / MAX_BAR_SLOT));
        const factor     = e.deltaY > 0 ? 1.15 : 0.87;
        const newVc      = clamp(Math.round(vc * factor), minVisible, Math.min(total, MAX_VISIBLE));
        if (newVc === vc) return;
        vcRef.current = newVc;
        riRef.current = clamp(riRef.current, newVc - 1, total - 1);
      }
      scheduleDraw();
      // 휠은 터치보다 빈도가 낮으므로 즉시 상태 동기화 (스크롤바 업데이트)
      syncReactState();
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleTouchStart(e: React.TouchEvent) {
    if (touchRef.current?.timer) clearTimeout(touchRef.current.timer);

    if (e.touches.length === 1) {
      const startX = e.touches[0].clientX;
      const startY = e.touches[0].clientY;
      // 250ms 동안 움직이지 않으면 크로스헤어 모드
      const timer = setTimeout(() => {
        const ts = touchRef.current;
        if (ts?.mode === "undecided") {
          ts.mode = "crosshair";
          setHoverIdx(xToBarIdx(ts.startX!));
        }
      }, 250);
      touchRef.current = { mode: "undecided", startX, startY, startRi: riRef.current, timer };
    } else if (e.touches.length === 2) {
      const [t1, t2] = [e.touches[0], e.touches[1]];
      touchRef.current = {
        mode:     "two-finger",
        prevDist: Math.hypot(t2.clientX - t1.clientX, t2.clientY - t1.clientY),
        prevMidX: (t1.clientX + t2.clientX) / 2,
      };
    }
  }

  function handleTouchMove(e: React.TouchEvent) {
    const ts = touchRef.current;
    if (!ts) return;
    const total = totalRef.current;
    if (total === 0) return;

    // 결정 대기 중: 8px 이상 빠르게 움직이면 팬 모드
    if (ts.mode === "undecided" && e.touches.length === 1) {
      const dx = e.touches[0].clientX - ts.startX!;
      const dy = e.touches[0].clientY - ts.startY!;
      if (dx * dx + dy * dy > 64) {
        clearTimeout(ts.timer);
        ts.mode    = "pan";
        ts.startX  = e.touches[0].clientX;
        ts.startRi = riRef.current;
      }
      return;
    }

    if (ts.mode === "pan" && e.touches.length === 1 && ts.startX != null && ts.startRi != null) {
      const dx    = e.touches[0].clientX - ts.startX;
      const plotW = widthRef.current - PAD.left - PAD.right;
      const delta = Math.round((dx * vcRef.current) / plotW);
      const newRi = clamp(ts.startRi - delta, vcRef.current - 1, total - 1);
      // ★ React 상태 업데이트 없음 — ref만 갱신 후 직접 draw
      riRef.current = newRi;
      scheduleDraw();
      return;
    }

    if (ts.mode === "crosshair" && e.touches.length === 1) {
      setHoverIdx(xToBarIdx(e.touches[0].clientX));
      return;
    }

    // 두 손가락: 줌 + 팬 동시
    if (ts.mode === "two-finger" && e.touches.length === 2 && ts.prevDist != null && ts.prevMidX != null) {
      const [t1, t2] = [e.touches[0], e.touches[1]];
      const newDist  = Math.hypot(t2.clientX - t1.clientX, t2.clientY - t1.clientY);
      const newMidX  = (t1.clientX + t2.clientX) / 2;
      const plotW    = widthRef.current - PAD.left - PAD.right;

      const scale      = ts.prevDist / newDist;
      const minVisible = Math.max(2, Math.ceil(plotW / MAX_BAR_SLOT));
      const newVc      = clamp(Math.round(vcRef.current * scale), minVisible, Math.min(total, MAX_VISIBLE));
      if (newVc !== vcRef.current) {
        vcRef.current = newVc;
        riRef.current = clamp(riRef.current, newVc - 1, total - 1);
      }

      const dmidX = newMidX - ts.prevMidX;
      const delta  = Math.round((dmidX * vcRef.current) / plotW);
      if (delta !== 0) {
        riRef.current = clamp(riRef.current - delta, vcRef.current - 1, total - 1);
      }

      ts.prevDist = newDist;
      ts.prevMidX = newMidX;

      // ★ React 상태 업데이트 없음 — ref만 갱신 후 직접 draw
      scheduleDraw();
    }
  }

  function handleTouchEnd() {
    if (touchRef.current?.timer) clearTimeout(touchRef.current.timer);
    touchRef.current = null;
    setHoverIdx(null);
    // ★ 제스처 완료 후 한 번만 React 상태 동기화 (스크롤바 위치 업데이트)
    syncReactState();
  }

  // 마우스 / 스타일러스 펜 호버
  function handlePointerMove(e: React.PointerEvent) {
    if (e.pointerType === "touch") return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) { setHoverIdx(null); return; }
    setHoverIdx(xToBarIdx(e.clientX));
  }

  function handlePointerLeave(e: React.PointerEvent) {
    if (e.pointerType === "touch") return;
    setHoverIdx(null);
  }

  return { handleTouchStart, handleTouchMove, handleTouchEnd, handlePointerMove, handlePointerLeave };
}
