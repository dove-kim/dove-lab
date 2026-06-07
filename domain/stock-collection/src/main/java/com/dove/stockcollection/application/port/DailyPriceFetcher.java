package com.dove.stockcollection.application.port;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.model.DailyCandle;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * 일봉 주가 조회 포트. 인프라(KIS 등)에 대한 추상화.
 */
public interface DailyPriceFetcher {

    /**
     * 수정주가 데이터 기산일 (KIS 최초 제공일). 전체 재조회 시 하한.
     */
    LocalDate ADJUSTED_DATA_START = LocalDate.of(1985, 1, 1);

    /**
     * from~to 구간을 내부적으로 청킹하며 조회하고, 각 청크를 consumer에 전달한다.
     * 메모리에는 청크 1개치만 보유된다.
     */
    void fetchInWindows(StockExchange exchange, String ticker,
                        LocalDate from, LocalDate to, PriceType priceType,
                        Consumer<List<DailyCandle>> consumer);

    /**
     * 수정주가 이력을 upTo부터 from까지 역방향으로 페이징하며 consumer에 전달한다.
     *
     * @param from 재조회 하한(이 날짜 이전은 받지 않음). 전체 재조회는 {@link #ADJUSTED_DATA_START}.
     */
    void fetchAdjustedBackward(StockExchange exchange, String ticker,
                               LocalDate from, LocalDate upTo,
                               Consumer<List<DailyCandle>> consumer);
}
