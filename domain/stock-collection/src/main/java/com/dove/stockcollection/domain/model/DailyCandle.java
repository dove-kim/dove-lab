package com.dove.stockcollection.domain.model;

import java.time.LocalDate;

/**
 * 일봉 가격 데이터. 인프라(KIS 등) 중립적인 도메인 타입.
 *
 * @param adjustmentCode 수정주가 이벤트 코드 (00=해당없음, 01=권리락, 02=배당락 등)
 */
public record DailyCandle(
        LocalDate tradingDate,
        long openPrice,
        long highPrice,
        long lowPrice,
        long closePrice,
        long accumulatedVolume,
        long accumulatedTurnover,
        String adjustmentCode
) {
    /**
     * 거래정지 여부: 거래량=0 + 시가=고가=저가=종가.
     */
    public boolean isHalt() {
        return accumulatedVolume == 0
                && openPrice == closePrice
                && highPrice == closePrice
                && lowPrice == closePrice;
    }
}
