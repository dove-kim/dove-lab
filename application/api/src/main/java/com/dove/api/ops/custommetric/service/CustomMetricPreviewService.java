package com.dove.api.ops.custommetric.service;

import com.dove.api.ops.custommetric.dto.MetricPoint;
import com.dove.custommetric.application.service.CustomMetricDefService;
import com.dove.custommetric.application.service.MetricComputer;
import com.dove.custommetric.application.service.MetricSeries;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.MetricSpecWindows;
import com.dove.screening.application.service.StockFilterQueryService;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 초안 커스텀 지표 스펙을 최근 거래일 구간에 대해 시험 계산해 미리보기 시계열을 낸다(저장하지 않음).
 */
@Service
@RequiredArgsConstructor
public class CustomMetricPreviewService {

    /** 미리보기 출력 상한(거래일). */
    private static final int PREVIEW_DAYS = 200;

    private final CustomMetricDefService defService;
    private final MetricComputer computer;
    private final StockFilterQueryService stockFilterQueryService;

    /**
     * 스펙을 최근 {@value #PREVIEW_DAYS} 거래일 구간에 대해 계산해 (거래일, 값) 목록을 반환한다.
     *
     * @throws IllegalArgumentException 스펙이 잘못된 경우
     */
    public List<MetricPoint> preview(String spec, PriceType priceType) {
        MetricSpec parsed = defService.parseSpec(spec);
        int lookback = MetricSpecWindows.maxLookback(parsed);
        LocalDate to = LocalDate.now();
        // 출력 200일 + 롤링 lookback을 채울 만큼 거래일 여유(달력일 근사).
        LocalDate from = to.minusDays((long) (PREVIEW_DAYS + lookback) * 2 + 30);

        MetricSeries series = computer.evaluate(parsed, priceType != null ? priceType : PriceType.RAW, from, to,
                id -> stockFilterQueryService.resolveTickers(id, null));

        List<LocalDate> dates = series.dates();
        double[] values = series.values();
        int n = dates.size();
        int start = Math.max(0, n - PREVIEW_DAYS);
        List<MetricPoint> points = new ArrayList<>(n - start);
        for (int i = start; i < n; i++) {
            points.add(new MetricPoint(dates.get(i).toString(),
                    Double.isNaN(values[i]) ? null : values[i]));
        }
        return points;
    }
}
