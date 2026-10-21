const KEY = "portfolio-last-account";

/** 마지막으로 기록을 넣은 계좌 ID(없으면 null). */
export function readLastAccountId(): number | null {
  try {
    const raw = localStorage.getItem(KEY);
    const id = raw == null ? NaN : Number(raw);
    return Number.isFinite(id) ? id : null;
  } catch {
    return null;
  }
}

/** 마지막으로 기록을 넣은 계좌 ID를 기억한다. */
export function writeLastAccountId(id: number): void {
  try {
    localStorage.setItem(KEY, String(id));
  } catch {
    // 저장 실패는 기본값 편의 기능일 뿐이라 무시한다.
  }
}
