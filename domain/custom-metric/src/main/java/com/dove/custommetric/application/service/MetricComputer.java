package com.dove.custommetric.application.service;

import com.dove.custommetric.domain.MetricEvaluator;
import com.dove.custommetric.domain.spec.AggNode;
import com.dove.custommetric.domain.spec.MetricSpec;
import com.dove.custommetric.domain.spec.MetricSpecNodes;
import com.dove.custommetric.infrastructure.repository.CustomMetricSourceSupport;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 계산식(MetricSpec)을 시장 거래일 구간에 대해 평가해 시계열을 내는 계산기. 야간 계산·미리보기가 공유한다.
 * universe 해석은 {@link UniverseResolver}로 주입받아 screening에 직접 의존하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MetricComputer {

    private final CustomMetricSourceSupport sourceSupport;

    /**
     * [fromInclusive, toInclusive] 시장 거래일에 대해 스펙을 평가한다. 횡단 집계 leaf는 universe별로 사전 로드해 채운다.
     */
    @Transactional(readOnly = true)
    public MetricSeries evaluate(MetricSpec spec, PriceType priceType,
                                 LocalDate fromInclusive, LocalDate toInclusive, UniverseResolver universeResolver) {
        List<LocalDate> dates = sourceSupport.marketTradeDates(priceType, fromInclusive, toInclusive);
        if (dates.isEmpty()) return new MetricSeries(dates, new double[0]);
        LocalDate first = dates.get(0);
        LocalDate last = dates.get(dates.size() - 1);

        Map<AggNode, double[]> aggSeries = new HashMap<>();
        for (AggNode node : MetricSpecNodes.aggNodes(spec)) {
            Set<String> tickers = universeResolver.tickers(node.universeFilterId());
            Map<LocalDate, Double> byDate = tickers.isEmpty() ? Map.of()
                    : sourceSupport.aggregate(node.agg(), node.colA(), node.colB(), tickers, priceType, first, last);
            aggSeries.put(node, align(byDate, dates));
        }

        double[] out = MetricEvaluator.evaluate(spec, dates.size(), node -> aggSeries.get(node));
        return new MetricSeries(dates, out);
    }

    private double[] align(Map<LocalDate, Double> byDate, List<LocalDate> dates) {
        double[] s = new double[dates.size()];
        for (int i = 0; i < dates.size(); i++) {
            Double v = byDate.get(dates.get(i));
            s[i] = v == null ? Double.NaN : v;
        }
        return s;
    }
}
