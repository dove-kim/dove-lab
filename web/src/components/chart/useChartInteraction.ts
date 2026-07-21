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
  /** 마우스 드래그 줌 선택 영역(컨테이너 기준 px [x0, x1]). null이면 선택 없음. */
  setDragSel:          (sel: [number, number] | null) => void;
  /** 스크롤/줌 중 React 리렌더 없이 canvas를 직접 다시 그리는 트리거 */
  triggerStaticDrawRef: React.MutableRefObject<() => void>;
  /** 뷰포트(스크롤/줌)가 바뀔 때마다 호출 — 좌측 가장자리 프리버퍼 lazy-load 트리거 */
  onViewportChangeRef:  React.MutableRefObject<() => void>;
}

type TouchMode = "undecided" | "pan" | "crosshair" | "two-finger" | "native-scroll";

interface TouchState {
  mode:          TouchMode;
  startX?:       number;
  startY?:       number;
  prevDist?:     number;
  prevMidX?:     number;
  timer?:        ReturnType<typeof setTimeout>;
  /** 팬 중 아직 적용하지 않은 소수 봉 (라운딩 드리프트 방지) */
  panRemainder?: number;
}

export function useChartInteraction({
  containerRef, vcRef, riRef, widthRef, totalRef,
  setVisibleCount, setRightIndex, setHoverIdx, setDragSel, triggerStaticDrawRef, onViewportChangeRef,
}: Params) {
  const touchRef   = useRef<TouchState | null>(null);
  // 마우스 드래그 줌 상태 (좌→우 확대 / 우→좌 축소)
  const dragRef    = useRef<{ startX: number; startClientX: number; moved: boolean } | null>(null);
  // rAF 핸들 — 스크롤/줌 시 중복 draw 방지
  const drawRafRef = useRef(0);

  /** ref 갱신 후 React 리렌더 없이 직접 canvas를 다시 그린다 */
  function scheduleDraw() {
    // 좌측 가장자리 근처면 과거 청크를 미리 당긴다 (rAF 밖에서 즉시 판정)
    onViewportChangeRef.current();
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

  // 휠 = 시간선 이동(팬). 줌은 드래그(확대/축소)·터치 핀치가 담당.
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
      // 세로·가로 휠 모두 우세 축 부호로 팬(세로 휠 아래=미래/오른쪽).
      const delta = Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY;
      const step  = Math.max(1, Math.round(vc * 0.08));
      const newRi = clamp(ri + Math.sign(delta) * step, vc - 1, total - 1);
      if (newRi === ri) return;
      riRef.current = newRi;
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
      touchRef.current = { mode: "undecided", startX, startY, timer };
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

    // 결정 대기 중: 8px 이상 움직이면 우세 축으로 모드 확정
    if (ts.mode === "undecided" && e.touches.length === 1) {
      const dx = e.touches[0].clientX - ts.startX!;
      const dy = e.touches[0].clientY - ts.startY!;
      if (dx * dx + dy * dy > 64) {
        clearTimeout(ts.timer);
        if (Math.abs(dx) >= Math.abs(dy)) {
          // 수평 우세 → 차트 팬 (여기서부터 증분 앵커)
          ts.mode         = "pan";
          ts.startX       = e.touches[0].clientX;
          ts.panRemainder = 0;
        } else {
          // 수직 우세 → 브라우저 세로 스크롤에 양보 (하단 지표 서브패널 보기)
          ts.mode = "native-scroll";
        }
      }
      return;
    }

    // 세로 스크롤 양보 모드: 차트는 손대지 않고 브라우저가 스크롤하게 둔다
    if (ts.mode === "native-scroll") return;

    if (ts.mode === "pan" && e.touches.length === 1 && ts.startX != null) {
      // 증분 팬: 직전 앵커 대비 이동량만 riRef에 누적 → 팬 도중 과거 데이터가
      // prepend되며 riRef가 밀려도(위치 보존) 낡은 절대값으로 덮어쓰지 않는다.
      const dx        = e.touches[0].clientX - ts.startX;
      const plotW     = widthRef.current - PAD.left - PAD.right;
      const deltaBars = (dx * vcRef.current) / plotW + (ts.panRemainder ?? 0);
      const step      = Math.round(deltaBars);
      ts.panRemainder = deltaBars - step;
      if (step !== 0) {
        // ★ React 상태 업데이트 없음 — ref만 갱신 후 직접 draw. total은 라이브(prepend 반영)
        riRef.current = clamp(riRef.current - step, vcRef.current - 1, totalRef.current - 1);
        ts.startX     = e.touches[0].clientX;
      }
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

  /** 드래그로 클릭을 구분하는 최소 이동 픽셀. */
  const DRAG_THRESHOLD = 5;

  /** 현재 뷰포트 기준 플롯 지표(픽셀→봉 인덱스 변환용). */
  function plotMetrics() {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return null;
    const plotW = widthRef.current - PAD.left - PAD.right;
    if (plotW <= 0) return null;
    const vc = vcRef.current, ri = riRef.current;
    const startIdx = Math.max(0, ri - vc + 1);
    const n = ri + 1 - startIdx;
    if (n <= 0) return null;
    return { rect, plotW, vc, ri, startIdx, n, slot: plotW / n };
  }

  /** clientX를 [0, total-1] 범위로 클램프한 절대 봉 인덱스로 변환한다(가장자리 밖은 끝봉으로). */
  function xToClampedAbsIdx(clientX: number, m: NonNullable<ReturnType<typeof plotMetrics>>): number {
    const x   = clientX - m.rect.left;
    const vis = clamp(Math.floor((x - PAD.left) / m.slot), 0, m.n - 1);
    return m.startIdx + vis;
  }

  // 마우스 드래그 시작 — 가격/거래량 영역에서만 줌 선택 개시
  function handlePointerDown(e: React.PointerEvent) {
    if (e.pointerType === "touch" || e.button !== 0) return;
    if (totalRef.current === 0) return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect || e.clientY - rect.top > PRICE_BOT) return;
    dragRef.current = { startX: e.clientX - rect.left, startClientX: e.clientX, moved: false };
    containerRef.current?.setPointerCapture(e.pointerId);
    setHoverIdx(null);
  }

  // 마우스 / 스타일러스 펜 — 드래그 중이면 선택영역 갱신, 아니면 호버
  function handlePointerMove(e: React.PointerEvent) {
    if (e.pointerType === "touch") return;
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) { setHoverIdx(null); return; }
    const drag = dragRef.current;
    if (drag) {
      if (Math.abs(e.clientX - drag.startClientX) > DRAG_THRESHOLD) drag.moved = true;
      setDragSel([drag.startX, e.clientX - rect.left]);
      return;
    }
    setHoverIdx(xToBarIdx(e.clientX));
  }

  // 마우스 드래그 종료 — 좌→우: 시간범위 확대 / 우→좌: 대칭 축소
  function handlePointerUp(e: React.PointerEvent) {
    if (e.pointerType === "touch") return;
    const drag = dragRef.current;
    if (!drag) return;
    dragRef.current = null;
    containerRef.current?.releasePointerCapture(e.pointerId);
    setDragSel(null);
    if (!drag.moved) return; // 클릭 수준 이동 → 무시

    const m = plotMetrics();
    if (!m) return;
    const total      = totalRef.current;
    const minVisible = Math.max(2, Math.ceil(m.plotW / MAX_BAR_SLOT));
    const a0 = xToClampedAbsIdx(drag.startClientX, m);
    const a1 = xToClampedAbsIdx(e.clientX, m);

    if (e.clientX >= drag.startClientX) {
      // 좌→우: 드래그한 봉 구간으로 확대
      const lo = Math.min(a0, a1), hi = Math.max(a0, a1);
      const newVc = clamp(hi - lo + 1, minVisible, Math.min(total, MAX_VISIBLE));
      vcRef.current = newVc;
      riRef.current = clamp(hi, newVc - 1, total - 1);
    } else {
      // 우→좌: 대칭 축소 — 현재 화면이 그 박스 크기로 들어가듯 배율만큼 축소
      const boxPx  = Math.max(1, drag.startClientX - e.clientX);
      const newVc  = clamp(Math.round(m.vc * (m.plotW / boxPx)), minVisible, Math.min(total, MAX_VISIBLE));
      const midAbs = (a0 + a1) / 2;
      vcRef.current = newVc;
      riRef.current = clamp(Math.round(midAbs + newVc / 2), newVc - 1, total - 1);
    }
    scheduleDraw();
    syncReactState();
  }

  function handlePointerLeave(e: React.PointerEvent) {
    if (e.pointerType === "touch") return;
    if (dragRef.current) return; // 드래그 중엔 무시(포인터 캡처 미지원 fallback)
    setHoverIdx(null);
  }

  // 포인터 취소(창 전환 등) — 진행 중 드래그 선택만 정리
  function handlePointerCancel() {
    if (!dragRef.current) return;
    dragRef.current = null;
    setDragSel(null);
  }

  return {
    handleTouchStart, handleTouchMove, handleTouchEnd,
    handlePointerDown, handlePointerMove, handlePointerUp, handlePointerLeave, handlePointerCancel,
  };
}
