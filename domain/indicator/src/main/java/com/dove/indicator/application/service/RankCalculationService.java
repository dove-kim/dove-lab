package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.RankCursorRewoundException;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.rank.PercentRankCalculator;
import com.dove.indicator.domain.rank.RankSourceExtractor;
import com.dove.indicator.domain.rank.entity.RankCursor;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.indicator.domain.rank.entity.StockRankDailyId;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.indicator.infrastructure.repository.RankSourceRepositorySupport;
import com.dove.indicator.infrastructure.repository.StockFeatureDailyRepositorySupport;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * (universe·가격유형)의 일자별 횡단면 percentile 순위를 커서 다음 날부터 지표 프런티어까지 계산·저장하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class RankCalculationService {

    private final RankCursorService cursorService;
    private final RankSourceRepositorySupport sourceSupport;
    private final StockFeatureDailyRepositorySupport featureSupport;
    private final RankDateCommitService commitService;
    private final PercentRankCalculator percentRankCalculator = new PercentRankCalculator();
    private final RankSourceExtractor sourceExtractor = new RankSourceExtractor();

    /**
     * universe의 순위를 커서 다음 거래일부터 지표 프런티어(전 member 지표 완비 거래일)까지 날짜별로 계산·저장한다.
     * 횡단면 percentile은 universe member 거래소를 풀링한 전체에서 계산하되, 각 순위 행은 그 종목의 native 거래소로 저장한다.
     * 각 거래일은 저장 + 커서 CAS 전진이 한 트랜잭션이며, 그 사이 커서가 달라지면 그 universe 계산을 중단한다(멱등).
     */
    public void calculateUniverse(MarketUniverse universe, PriceType priceType) {
        Optional<RankCursor> cursorOpt = cursorService.findCursor(universe, priceType);
        LocalDate expected = cursorOpt.map(RankCursor::getCursorDate).orElse(null);
        boolean cursorExists = cursorOpt.isPresent();

        LocalDate frontier = sourceSupport.findIndicatorFrontier(universe.members(), priceType);
        if (frontier == null) return; // 확정 프런티어 없음(지표 미완비) → 다음 배치로

        List<LocalDate> dates = sourceSupport.findFeatureTradeDates(
                universe.members(), priceType, expected, frontier);

        for (LocalDate date : dates) {
            List<StockFeatureDaily> rows =
                    featureSupport.findByExchangesAndPriceTypeAndDate(universe.members(), priceType, date);
            List<StockRankDaily> rankRows = rankRowsForDate(priceType, date, rows);
            try {
                commitService.commit(universe, priceType, rankRows, expected, cursorExists, date);
            } catch (RankCursorRewoundException e) {
                break;
            }
            expected = date;
            cursorExists = true;
        }
    }

    /**
     * 한 거래일의 union 피처 행들로 9개 순위를 횡단면 계산해 종목별 wide 순위 행을 만든다.
     * percentile은 union 전체에서 계산하고, 각 행은 그 종목의 native 거래소로 키한다.
     */
    private List<StockRankDaily> rankRowsForDate(PriceType priceType, LocalDate date,
                                                 List<StockFeatureDaily> rows) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, StockRankDaily> rankByTicker = new LinkedHashMap<>();
        for (StockFeatureDaily row : rows) {
            StockRankDailyId id = new StockRankDailyId(
                    row.getId().getTicker(), row.getId().getExchange(), priceType, date);
            rankByTicker.put(row.getId().getTicker(), new StockRankDaily(id, now));
        }

        for (RankType rankType : RankType.values()) {
            Map<String, Double> values = new LinkedHashMap<>();
            for (StockFeatureDaily row : rows) {
                values.put(row.getId().getTicker(), sourceExtractor.extract(row, rankType));
            }
            Map<String, Double> ranks = percentRankCalculator.percentRank(values);
            for (Map.Entry<String, Double> e : ranks.entrySet()) {
                rankByTicker.get(e.getKey()).set(rankType, e.getValue());
            }
        }
        return new ArrayList<>(rankByTicker.values());
    }
}
