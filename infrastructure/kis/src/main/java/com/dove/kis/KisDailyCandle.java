package com.dove.kis;

import java.time.LocalDate;

/**
 * 한국투자증권 국내주식기간별시세 조회 결과 (일/주/월/년 봉).
 * changeCode: 00=해당없음 01=권리락 02=배당락 03=분배락 04=권배락 등
 * priceChangeSign: 1=상한 2=상승 3=보합 4=하한 5=하락
 */
public record KisDailyCandle(
        LocalDate tradingDate,
        long openPrice,
        long highPrice,
        long lowPrice,
        long closePrice,
        long accumulatedVolume,
        long accumulatedTurnover,
        String changeCode,
        String priceChangeSign,
        long priceChange
) {
    /**
     * 거래정지 여부 판별 (거래량=0이고 시·고·저가가 모두 종가와 동일).
     */
    public boolean isHalt() {
        return accumulatedVolume == 0
                && openPrice == closePrice
                && highPrice == closePrice
                && lowPrice == closePrice;
    }
}
