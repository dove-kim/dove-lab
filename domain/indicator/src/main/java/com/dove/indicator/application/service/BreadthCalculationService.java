package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.BreadthCursorRewoundException;
import com.dove.indicator.domain.breadth.entity.BreadthCursor;
import com.dove.indicator.domain.breadth.entity.StockBreadthDaily;
import com.dove.indicator.domain.breadth.entity.StockBreadthDailyId;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.infrastructure.repository.RankSourceRepositorySupport;
import com.dove.indicator.infrastructure.repository.StockFeatureDailyRepositorySupport;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * (universe·가격유형)의 일자별 당일 상승비율을 커서 다음 날부터 지표 프런티어까지 계산·저장하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class BreadthCalculationService {

    private final BreadthCursorService cursorService;
    private final RankSourceRepositorySupport sourceSupport;
    private final StockFeatureDailyRepositorySupport featureSupport;
    private final BreadthDateCommitService commitService;

    /**
     * universe의 상승비율을 커서 다음 거래일부터 지표 프런티어(전 member 지표 완비 거래일)까지 날짜별로 계산·저장한다.
     * 상승비율은 union 전체에서 단일 값으로 계산하고, 그 값을 각 member 거래소 행으로 중복 저장한다.
     * 각 거래일은 저장 + 커서 CAS 전진이 한 트랜잭션이며, 그 사이 커서가 달라지면 그 universe 계산을 중단한다(멱등).
     */
    public void calculateUniverse(MarketUniverse universe, PriceType priceType) {
        Optional<BreadthCursor> cursorOpt = cursorService.findCursor(universe, priceType);
        LocalDate expected = cursorOpt.map(BreadthCursor::getCursorDate).orElse(null);
        boolean cursorExists = cursorOpt.isPresent();

        LocalDate frontier = sourceSupport.findIndicatorFrontier(universe.members(), priceType);
        if (frontier == null) return; // 확정 프런티어 없음(지표 미완비) → 다음 배치로

        List<LocalDate> dates = sourceSupport.findFeatureTradeDates(
                universe.members(), priceType, expected, frontier);

        for (LocalDate date : dates) {
            List<StockFeatureDaily> rows =
                    featureSupport.findByExchangesAndPriceTypeAndDate(universe.members(), priceType, date);
            List<StockBreadthDaily> breadthRows = breadthRowsForDate(universe, priceType, date, rows);
            try {
                commitService.commit(universe, priceType, breadthRows, expected, cursorExists, date);
            } catch (BreadthCursorRewoundException e) {
                break;
            }
            expected = date;
            cursorExists = true;
        }
    }

    /**
     * 한 거래일의 union 피처 행들로 RET_1D>0 종목 비율을 단일 값으로 계산하고, 그 값을 각 member 거래소 행으로 만든다.
     * RET_1D가 NULL이 아닌 종목이 하나도 없으면(첫 거래일 워밍업) 빈 목록을 반환한다(커서만 전진).
     */
    private List<StockBreadthDaily> breadthRowsForDate(MarketUniverse universe, PriceType priceType, LocalDate date,
                                                       List<StockFeatureDaily> rows) {
        int denom = 0;
        int num = 0;
        for (StockFeatureDaily row : rows) {
            Float ret1d = row.getRet1d();
            if (ret1d == null) continue;
            denom++;
            if (ret1d > 0f) num++;
        }
        if (denom == 0) return List.of();

        double ratio = (double) num / denom;
        LocalDateTime now = LocalDateTime.now();
        List<StockBreadthDaily> result = new ArrayList<>(universe.members().size());
        for (StockExchange member : universe.members()) {
            StockBreadthDailyId id = new StockBreadthDailyId(member, priceType, date);
            result.add(new StockBreadthDaily(id, ratio, now));
        }
        return result;
    }
}
