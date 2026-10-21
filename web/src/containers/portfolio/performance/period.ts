/** 성과 화면의 기간 선택 키. */
export type PeriodKey = "all" | "thisYear" | "lastYear" | "last12m";

/**
 * 성과 집계 기간 — 실현 이벤트와 보유 구간을 이 범위로 자른다.
 *
 * @param key   기간 키
 * @param label 화면에 보이는 이름
 * @param from  시작일(ISO, 전체 기간이면 null)
 * @param to    종료일(ISO, 열린 끝이면 null)
 */
export interface Period {
  key: PeriodKey;
  label: string;
  from: string | null;
  to: string | null;
}

const iso = (d: Date) => d.toISOString().slice(0, 10);

/**
 * 기간 키를 실제 날짜 범위로 푼다(오늘 기준).
 */
export function resolvePeriod(key: PeriodKey): Period {
  const now = new Date();
  const year = now.getFullYear();
  switch (key) {
    case "thisYear":
      return { key, label: "올해", from: `${year}-01-01`, to: null };
    case "lastYear":
      return { key, label: "작년", from: `${year - 1}-01-01`, to: `${year - 1}-12-31` };
    case "last12m": {
      const from = new Date(now);
      from.setFullYear(from.getFullYear() - 1);
      return { key, label: "최근 1년", from: iso(from), to: null };
    }
    default:
      return { key: "all", label: "전체 기간", from: null, to: null };
  }
}

/** 모든 기간 선택지(칩 순서). */
export const PERIOD_KEYS: PeriodKey[] = ["all", "thisYear", "lastYear", "last12m"];

/** 날짜가 기간 안에 드는지. */
export function inPeriod(date: string, period: Period): boolean {
  const d = date.slice(0, 10);
  if (period.from && d < period.from) return false;
  if (period.to && d > period.to) return false;
  return true;
}

/**
 * 보유 구간 [entry, end]와 기간이 겹치는 일수(겹치지 않으면 0).
 *
 * @param entry  구간 시작일(ISO)
 * @param end    구간 종료일(ISO, 보유 중이면 오늘)
 * @param period 자를 기간
 */
export function overlapDays(entry: string, end: string, period: Period): number {
  const clipped = clipToPeriod(entry, end, period);
  if (!clipped) return 0;
  return daysBetween(clipped[0], clipped[1]);
}

/**
 * 보유 구간 여러 개가 달력에서 덮는 날수 — 겹치는 구간은 한 번만 센다.
 *
 * <p>같은 종목을 여러 계좌에서 동시에 들고 있어도 그 날은 하루다.
 *
 * @param intervals 보유 구간 [시작일, 종료일] 목록(ISO)
 * @param period    자를 기간
 */
export function unionOverlapDays(intervals: [string, string][], period: Period): number {
  const clipped = intervals
    .map(([from, to]) => clipToPeriod(from, to, period))
    .filter((r): r is [string, string] => r !== null)
    .sort((a, b) => a[0].localeCompare(b[0]));
  if (clipped.length === 0) return 0;

  let total = 0;
  let [spanStart, spanEnd] = clipped[0];
  for (const [start, end] of clipped.slice(1)) {
    if (start <= spanEnd) {
      if (end > spanEnd) spanEnd = end;
      continue;
    }
    total += daysBetween(spanStart, spanEnd);
    spanStart = start;
    spanEnd = end;
  }
  return total + daysBetween(spanStart, spanEnd);
}

/** 구간을 기간으로 자른다(겹치지 않으면 null). */
function clipToPeriod(entry: string, end: string, period: Period): [string, string] | null {
  const start = period.from && period.from > entry ? period.from : entry;
  const finish = period.to && period.to < end ? period.to : end;
  return finish < start ? null : [start, finish];
}

/** 두 날짜 사이의 일수. */
function daysBetween(from: string, to: string): number {
  return Math.max(0, Math.round((Date.parse(to) - Date.parse(from)) / 86_400_000));
}
