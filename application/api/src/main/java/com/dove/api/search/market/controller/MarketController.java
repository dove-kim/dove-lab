package com.dove.api.search.market.controller;

import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.search.market.dto.TradingDaysResponse;
import com.dove.userfeature.domain.capability.Capability;
import com.dove.market.application.service.ExchangeTradingDateService;
import com.dove.market.domain.enums.Exchange;
import com.dove.market.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 시장 거래일 조회 API.
 */
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
@RequireCapability(Capability.STOCK_VIEW)
public class MarketController {

    private static final List<MarketType> ALL_MARKETS = MarketType.KRX_MARKETS;

    private final ExchangeTradingDateService tradingDateService;

    /**
     * 최근 거래일 목록과 최신 거래일을 반환한다.
     */
    @GetMapping("/trading-days")
    public TradingDaysResponse getTradingDays(@RequestParam(defaultValue = "90") int limit) {
        LocalDate today = LocalDate.now();
        List<String> dates = tradingDateService
                .findRecentTradingDates(Exchange.KRX, today, limit)
                .stream()
                .map(LocalDate::toString)
                .toList();
        String latestDate = dates.isEmpty() ? today.toString() : dates.get(0);
        return new TradingDaysResponse(latestDate, dates);
    }

    /**
     * 시스템이 지원하는 시장 목록(KRX)을 반환한다.
     */
    @GetMapping("/available")
    public List<String> getAvailableMarkets() {
        return ALL_MARKETS.stream().map(MarketType::name).toList();
    }
}
