import { describe, expect, it } from "vitest";
import { type Period, inPeriod, overlapDays, unionOverlapDays } from "./period";

const ALL: Period = { key: "all", label: "전체 기간", from: null, to: null };
const YEAR_2026: Period = { key: "thisYear", label: "올해", from: "2026-01-01", to: "2026-12-31" };

describe("period", () => {
  describe("inPeriod", () => {
    it("전체 기간은 모든 날짜를 받는다", () => {
      expect(inPeriod("2020-01-01", ALL)).toBe(true);
    });

    it("범위 밖 날짜는 거른다", () => {
      expect(inPeriod("2025-12-31", YEAR_2026)).toBe(false);
      expect(inPeriod("2027-01-01", YEAR_2026)).toBe(false);
    });

    it("경계 날짜는 포함한다", () => {
      expect(inPeriod("2026-01-01", YEAR_2026)).toBe(true);
      expect(inPeriod("2026-12-31", YEAR_2026)).toBe(true);
    });
  });

  describe("overlapDays", () => {
    it("전체 기간이면 구간 전체 길이다", () => {
      expect(overlapDays("2026-07-18", "2026-10-21", ALL)).toBe(95);
    });

    it("기간 밖으로 삐져나온 부분은 자른다", () => {
      expect(overlapDays("2025-10-15", "2026-10-21", YEAR_2026)).toBe(293);
    });

    it("겹치지 않으면 0이다", () => {
      expect(overlapDays("2024-01-01", "2024-12-31", YEAR_2026)).toBe(0);
    });
  });

  describe("unionOverlapDays", () => {
    it("겹치는 구간은 한 번만 센다", () => {
      // 같은 종목을 세 계좌에서 같은 기간 들고 있어도 달력으로는 한 번이다.
      const spans: [string, string][] = [
        ["2022-08-03", "2026-10-21"],
        ["2022-08-03", "2026-10-21"],
        ["2026-07-18", "2026-10-21"],
      ];

      expect(unionOverlapDays(spans, ALL)).toBe(1540);
    });

    it("떨어진 구간은 각각 더한다", () => {
      const spans: [string, string][] = [
        ["2026-01-01", "2026-01-11"],
        ["2026-03-01", "2026-03-11"],
      ];

      expect(unionOverlapDays(spans, ALL)).toBe(20);
    });

    it("일부만 겹치면 합집합 길이가 된다", () => {
      const spans: [string, string][] = [
        ["2026-01-01", "2026-01-21"],
        ["2026-01-11", "2026-02-01"],
      ];

      expect(unionOverlapDays(spans, ALL)).toBe(31);
    });

    it("맞닿은 구간은 이어 붙인다", () => {
      const spans: [string, string][] = [
        ["2026-01-01", "2026-01-11"],
        ["2026-01-11", "2026-01-21"],
      ];

      expect(unionOverlapDays(spans, ALL)).toBe(20);
    });

    it("기간 밖 구간은 빼고 센다", () => {
      const spans: [string, string][] = [
        ["2024-01-01", "2024-06-01"],
        ["2026-01-01", "2026-01-11"],
      ];

      expect(unionOverlapDays(spans, YEAR_2026)).toBe(10);
    });

    it("구간이 없으면 0이다", () => {
      expect(unionOverlapDays([], ALL)).toBe(0);
    });
  });
});
