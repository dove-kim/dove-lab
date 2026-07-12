"use client";

import { useEffect, useState } from "react";
import { ConditionNames, MetricSummary, ModelSummary } from "@/types/filter";

/**
 * 커스텀 지표·모델 목록을 한 번 조회해 조건 요약용 id→이름 맵을 만든다. 조회 실패 시 빈 맵(요약이 #id로 폴백).
 */
export function useConditionNames(): ConditionNames {
  const [names, setNames] = useState<ConditionNames>({});

  useEffect(() => {
    let cancelled = false;

    const load = (url: string) =>
      fetch(url)
        .then((r) => (r.ok ? r.json() : []))
        .catch(() => []);

    Promise.all([load("/api/stocks/custom-metrics"), load("/api/stocks/models")]).then(
      ([metricsData, modelsData]: [MetricSummary[], ModelSummary[]]) => {
        if (cancelled) return;
        const metrics: Record<number, string> = {};
        for (const m of metricsData) metrics[m.id] = m.name;
        const models: Record<number, string> = {};
        for (const m of modelsData) models[m.id] = m.name;
        setNames({ metrics, models });
      }
    );

    return () => {
      cancelled = true;
    };
  }, []);

  return names;
}
