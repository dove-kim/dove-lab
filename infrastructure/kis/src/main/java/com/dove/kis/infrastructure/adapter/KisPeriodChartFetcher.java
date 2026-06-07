package com.dove.kis.infrastructure.adapter;

import com.dove.kis.KisDailyCandle;
import com.dove.kis.KisMarketCode;
import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisPeriodChartBar;
import com.dove.kis.infrastructure.client.dto.KisPeriodChartResponse;
import com.dove.datetime.dto.DateRange;
import com.dove.datetime.DateRanges;
import com.dove.kis.quota.KisErrorCodes;
import com.dove.kis.quota.KisGate;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.model.DailyCandle;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * 국내주식기간별시세 어댑터 (TR_ID: FHKST03010100).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisPeriodChartFetcher implements DailyPriceFetcher {

    /**
     * KIS 일봉 단일 호출 최대 건수.
     */
    public static final int MAX_CANDLES_PER_CALL = 100;

    private static final String TR_ID = "FHKST03010100";
    private static final String DAILY = "D";
    private static final String RAW_PRICE = "1";
    private static final String ADJUSTED_PRICE = "0";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisStockClient stockClient;
    private final KisGate kisGate;

    /**
     * from~to 구간을 100일 윈도우로 나눠 조회하고 각 청크를 consumer에 전달한다.
     */
    @Override
    public void fetchInWindows(StockExchange exchange, String ticker,
                               LocalDate from, LocalDate to, PriceType priceType,
                               Consumer<List<DailyCandle>> consumer) {
        for (DateRange window : DateRanges.split(from, to, MAX_CANDLES_PER_CALL)) {
            try {
                List<DailyCandle> chunk = kisGate.call(
                        () -> fetchDaily(exchange, ticker, window.from(), window.to(), priceType)
                                .stream().map(KisPeriodChartFetcher::toDomain).toList());
                consumer.accept(chunk);
            } catch (FeignException e) {
                // 재시도 소진된 KIS 일시오류는 상위로 전파, 그 외는 데이터 없음으로 스킵
                if (KisErrorCodes.isTransient(e)) throw e;
                log.debug("데이터 없음, 스킵: exchange={}, ticker={}, status={}", exchange, ticker, e.status());
            }
        }
    }

    @Override
    public void fetchAdjustedBackward(StockExchange exchange, String ticker,
                                      LocalDate from, LocalDate upTo,
                                      Consumer<List<DailyCandle>> consumer) {
        LocalDate lower = from.isBefore(ADJUSTED_DATA_START) ? ADJUSTED_DATA_START : from;
        LocalDate to = upTo;
        while (true) {
            final LocalDate windowTo = to;
            List<DailyCandle> chunk = kisGate.call(
                    () -> fetchDaily(exchange, ticker, lower, windowTo, PriceType.ADJUSTED)
                            .stream().map(KisPeriodChartFetcher::toDomain).toList());
            consumer.accept(chunk);
            if (chunk.size() < MAX_CANDLES_PER_CALL) break;
            LocalDate earliest = chunk.stream().map(DailyCandle::tradingDate)
                    .min(LocalDate::compareTo).orElseThrow();
            if (!earliest.isAfter(lower)) break; // 하한 도달
            to = earliest.minusDays(1);
        }
    }

    private static DailyCandle toDomain(KisDailyCandle c) {
        return new DailyCandle(c.tradingDate(),
                c.openPrice(), c.highPrice(), c.lowPrice(), c.closePrice(),
                c.accumulatedVolume(), c.accumulatedTurnover(), c.changeCode());
    }

    /**
     * 단일 100일 이하 구간을 조회한다.
     */
    public List<KisDailyCandle> fetchDaily(StockExchange exchange, String stockCode,
                                           LocalDate from, LocalDate to, PriceType priceType) {
        String marketCode = KisMarketCode.of(exchange).name();
        String orgAdjPrc = priceType == PriceType.ADJUSTED ? ADJUSTED_PRICE : RAW_PRICE;

        KisPeriodChartResponse response;
        try {
            response = stockClient.getPeriodChart(
                    TR_ID, marketCode, stockCode,
                    from.format(DATE_FMT), to.format(DATE_FMT), DAILY, orgAdjPrc);
        } catch (FeignException e) {
            log.debug("KIS 기간별시세 조회 실패: stockCode={}, market={}, status={}",
                    stockCode, marketCode, e.status());
            throw e;
        }
        if (!response.isSuccess()) {
            throw new KisApiException(response.getMessageCode(), response.getMessage());
        }
        if (response.getOutput2() == null) {
            return List.of();
        }
        return response.getOutput2().stream()
                .map(KisPeriodChartBar::toCandle)
                .toList();
    }
}
