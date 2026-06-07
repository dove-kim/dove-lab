package com.dove.api.search.stock.dto;

import com.dove.stock.domain.entity.StockPrice;

import java.time.LocalDate;

/**
 * 일자별 시세 바.
 *
 * @param date   거래일
 * @param status 거래 상태(정상/거래정지/상장폐지)
 * @param open   시가
 * @param high   고가
 * @param low    저가
 * @param close  종가
 * @param volume 거래량
 */
public record PriceBar(String date, PriceBarStatus status, Long open, Long high, Long low, Long close, Long volume) {

    public static PriceBar of(LocalDate date, StockPrice price) {
        if (price == null) {
            return new PriceBar(date.toString(), PriceBarStatus.DELISTED, null, null, null, null, null);
        }
        if (price.getOpenPrice() == null || price.getVolume() == null || price.getVolume() == 0L) {
            return new PriceBar(date.toString(), PriceBarStatus.HALTED,
                    null, null, null, price.getClosePrice(), null);
        }
        return new PriceBar(date.toString(), PriceBarStatus.TRADING,
                price.getOpenPrice(), price.getHighPrice(),
                price.getLowPrice(), price.getClosePrice(), price.getVolume());
    }
}
