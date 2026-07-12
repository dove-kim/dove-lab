package com.dove.scheduler.service;

import com.dove.custommetric.application.service.MetricComputer;
import com.dove.custommetric.application.service.CustomMetricDailyService;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.custommetric.application.service.MetricSeries;
import com.dove.custommetric.domain.entity.CustomMetricDaily;
import com.dove.custommetric.domain.entity.CustomMetricDailyId;
import com.dove.custommetric.domain.entity.CustomMetricDef;
import com.dove.custommetric.domain.entity.MetricShape;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.MetricSpecWindows;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.screening.application.service.StockFilterQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 활성 커스텀 지표(SERIES)를 저장된 마지막 거래일 다음부터 증분 계산·저장하는 스케줄러 단계 서비스.
 * universe 해석(screening)만 여기서 엮고, 실제 계산은 {@link CustomMetricComputer}에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomMetricComputeService {

    private final CustomMetricDefService defService;
    private final CustomMetricDailyService dailyService;
    private final MetricComputer computer;
    private final StockFilterQueryService stockFilterQueryService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 활성 지표를 모두 증분 계산한다. 상한은 시장 최신 거래일로 자동 결정되므로 {@code today}는 상한 캡·트리거 표시용이다.
     */
    public void calculateAll(LocalDate today) {
        List<CustomMetricDef> defs = defService.findActive();
        jobStatusRegistry.start(SchedulerJobName.CUSTOM_METRIC.name(), defs.size());
        log.info("커스텀 지표 계산 시작: {}개", defs.size());

        int done = 0;
        for (CustomMetricDef def : defs) {
            try {
                if (def.getShape() == MetricShape.SERIES) {
                    computeSeries(def, today);
                }
            } catch (Exception e) {
                log.warn("[{}] 커스텀 지표 계산 실패: {}", def.getName(), e.getMessage());
                defService.recordError(def.getId(), e.getMessage());
            }
            jobStatusRegistry.progress(SchedulerJobName.CUSTOM_METRIC.name(), ++done);
        }

        jobStatusRegistry.complete(SchedulerJobName.CUSTOM_METRIC.name());
        log.info("커스텀 지표 계산 완료");
    }

    private void computeSeries(CustomMetricDef def, LocalDate today) {
        MetricSpec spec = defService.parseSpec(def.getSpec());
        int lookback = MetricSpecWindows.maxLookback(spec);
        LocalDate last = def.getLastComputedDate();
        // 증분: 재개 지점 이전으로 lookback 거래일치 여유(달력일 근사). null=전 이력 백필.
        LocalDate from = last == null ? null : last.minusDays((long) lookback * 2 + 30);

        MetricSeries series = computer.evaluate(spec, def.getPriceType(), from, today,
                id -> stockFilterQueryService.resolveTickers(id, null));
        List<LocalDate> dates = series.dates();
        if (dates.isEmpty()) return;
        LocalDate frontier = dates.get(dates.size() - 1);
        if (last != null && !last.isBefore(frontier)) return; // 새 거래일 없음

        double[] out = series.values();
        LocalDateTime now = LocalDateTime.now();
        List<CustomMetricDaily> rows = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            if (last != null && !d.isAfter(last)) continue; // 이미 저장된 과거
            if (Double.isNaN(out[i])) continue;             // 미확정 구간
            rows.add(new CustomMetricDaily(new CustomMetricDailyId(def.getId(), d), out[i], now));
        }
        dailyService.saveAll(rows);
        defService.recordComputed(def.getId(), frontier);
    }
}
