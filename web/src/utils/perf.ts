/**
 * 개발 환경 전용 성능 측정 유틸리티.
 * NODE_ENV !== 'development' 이면 아무것도 하지 않는다.
 */

const DEV = typeof window !== "undefined" && process.env.NODE_ENV === "development";

type Category = "API" | "Render" | "Mount";

const COLOR: Record<Category, string> = {
  API:    "color:#818cf8;font-weight:bold",
  Render: "color:#34d399;font-weight:bold",
  Mount:  "color:#fb923c;font-weight:bold",
};

function log(category: Category, label: string, ms: number) {
  const icon = category === "API" ? "🌐" : category === "Render" ? "🎨" : "⚙️";
  console.log(
    `%c${icon} [${category}] ${label}`,
    COLOR[category],
    `→ ${ms.toFixed(1)} ms`,
  );
}

async function measure<T>(category: Category, label: string, fn: () => Promise<T>): Promise<T> {
  if (!DEV) return fn();
  const t0 = performance.now();
  try {
    return await fn();
  } finally {
    log(category, label, performance.now() - t0);
  }
}

function start(): number {
  return DEV ? performance.now() : 0;
}

function end(category: Category, label: string, t0: number) {
  if (!DEV || t0 === 0) return;
  log(category, label, performance.now() - t0);
}

// ── Pipeline 타이머 ────────────────────────────────────────────────────────────
// 클릭 → 화면까지 각 단계의 누적 시간을 추적한다.
// pipe.start()로 파이프라인 시작, pipe.mark()로 각 체크포인트 기록.

let _pipeT0 = 0;
let _pipeLabel = "";

const PIPE_HEAD  = "color:#f59e0b;font-weight:bold;font-size:11px";
const PIPE_MARK  = "color:#94a3b8;font-size:11px";
const PIPE_WARN  = "color:#f87171;font-size:11px";

const pipe = {
  /** 파이프라인 측정 시작. 콘솔에 헤더를 출력한다. */
  start(label: string) {
    if (!DEV) return;
    _pipeT0 = performance.now();
    _pipeLabel = label;
    console.groupCollapsed(`%c⏱ Pipeline: ${label}`, PIPE_HEAD);
  },
  /** 체크포인트. start() 기준 누적 시간을 출력한다. */
  mark(step: string) {
    if (!DEV || _pipeT0 === 0) return;
    const ms = performance.now() - _pipeT0;
    const warn = ms > 100;
    console.log(
      `%c  +${ms.toFixed(1).padStart(7)} ms  │  ${step}`,
      warn ? PIPE_WARN : PIPE_MARK,
    );
  },
  /** 파이프라인 종료. 총 시간을 출력하고 그룹을 닫는다. */
  end(step: string) {
    if (!DEV || _pipeT0 === 0) return;
    const ms = performance.now() - _pipeT0;
    console.log(
      `%c  +${ms.toFixed(1).padStart(7)} ms  ✓  ${step}  [total: ${ms.toFixed(1)} ms]`,
      ms > 200 ? PIPE_WARN : "color:#4ade80;font-weight:bold;font-size:11px",
    );
    console.groupEnd();
    _pipeT0 = 0;
    _pipeLabel = "";
  },
};

export const perf = { measure, start, end, pipe };
