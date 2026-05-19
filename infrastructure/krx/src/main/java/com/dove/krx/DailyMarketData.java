package com.dove.krx;

import java.time.LocalDate;
import java.util.List;

/**
 * 하루치 시장 데이터.
 *
 * <p>{@code listings}와 {@code prices}는 {@link DayStatus#OPEN}일 때만 채워진다.
 * {@link DayStatus#CLOSED}·{@link DayStatus#UNCONFIRMED}에서는 항상 빈 리스트.
 */
public record DailyMarketData(
        LocalDate date,
        DayStatus status,
        List<StockListing> listings,
        List<StockPrice> prices
) {
    public static DailyMarketData open(LocalDate date, List<StockListing> listings, List<StockPrice> prices) {
        return new DailyMarketData(date, DayStatus.OPEN, listings, prices);
    }

    public static DailyMarketData closed(LocalDate date) {
        return new DailyMarketData(date, DayStatus.CLOSED, List.of(), List.of());
    }

    public static DailyMarketData unconfirmed(LocalDate date) {
        return new DailyMarketData(date, DayStatus.UNCONFIRMED, List.of(), List.of());
    }
}
