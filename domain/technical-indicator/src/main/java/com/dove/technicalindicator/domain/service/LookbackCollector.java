package com.dove.technicalindicator.domain.service;

import com.dove.market.application.service.MarketTradingDateQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LookbackCollector {

    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final MarketTradingDateQueryService marketTradingDateQueryService;

    /**
     * nextDay를 포함해 과거로 size개의 DailyStockPrice를 수집한다.
     * 결과는 날짜 오름차순(oldest → nextDay)으로 반환한다.
     * 갭 구간에서 시장이 열렸는데 이 종목 주가가 없으면 수집 중단 → 반환 목록 크기가 size 미만일 수 있다.
     */
    @Transactional(readOnly = true)
    public List<DailyStockPrice> collect(MarketType market, String code, LocalDate nextDay, int size) {
        List<DailyStockPrice> candidates =
                dailyStockPriceQueryService.findRecentDailyStockPrice(market, code, nextDay, size);

        List<DailyStockPrice> pool = new ArrayList<>();
        LocalDate prevDate = nextDay.plusDays(1);

        for (DailyStockPrice price : candidates) {
            LocalDate priceDate = price.getId().getTradeDate();

            if (hasBreakInGap(market, prevDate, priceDate)) {
                Collections.reverse(pool);
                return pool;
            }

            pool.add(price);
            prevDate = priceDate;

            if (pool.size() == size) break;
        }

        Collections.reverse(pool);
        return pool;
    }

    /**
     * prevDate(exclusive)와 priceDate(exclusive) 사이에 시장이 열렸던 날이 있으면 true.
     * 시장이 열렸는데 이 종목 주가가 없다는 뜻이므로 연속성 단절.
     * 주말·휴장은 MarketTradingDate.isOpen=false로 자연히 스킵된다.
     */
    private boolean hasBreakInGap(MarketType market, LocalDate prevDate, LocalDate priceDate) {
        LocalDate gapDate = prevDate.minusDays(1);
        while (gapDate.isAfter(priceDate)) {
            if (marketTradingDateQueryService.existsOpenDay(market, gapDate)) {
                return true;
            }
            gapDate = gapDate.minusDays(1);
        }
        return false;
    }
}
