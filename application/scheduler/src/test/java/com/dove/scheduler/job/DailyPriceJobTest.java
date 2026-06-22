package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.kis.infrastructure.adapter.KisTradingDayAdapter;
import com.dove.market.application.service.ExchangeTradingDateService;
import com.dove.market.domain.enums.Exchange;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.PriceCollectionService;
import com.dove.systemevent.application.service.SystemEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link DailyPriceJob} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPriceJob")
class DailyPriceJobTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.now(CLOCK);

    @Mock
    private PriceCollectionService priceCollectionService;

    @Mock
    private SystemEventService systemEventService;

    @Mock
    private JobStatusRegistry jobStatusRegistry;

    @Mock
    private KisTradingDayAdapter tradingDayAdapter;

    @Mock
    private ExchangeTradingDateService tradingDateService;

    private DailyPriceJob job() {
        return new DailyPriceJob(priceCollectionService, systemEventService,
                jobStatusRegistry, tradingDayAdapter, tradingDateService, CLOCK);
    }

    @Nested
    @DisplayName("run() — 휴장일")
    class WhenHoliday {

        @Test
        @DisplayName("휴장일이면 start·collect 없이 즉시 스킵한다")
        void shouldSkipWhenNotTradingDay() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(false);

            job().run();

            verify(jobStatusRegistry, never()).start(any(), anyLong());
            verify(priceCollectionService, never()).collect(any(), any(), any(), any(), any());
            verify(jobStatusRegistry, never()).complete(any());
            verify(tradingDateService, never()).register(any(), any());
        }
    }

    @Nested
    @DisplayName("run() — 거래일")
    class WhenTradingDay {

        @Test
        @DisplayName("거래소별로 당일 주가를 수집하고 start·complete한다")
        void shouldCollectEachExchangeAndCompleteWhenTradingDay() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);

            job().run();

            int exchangeCount = StockExchange.values().length;
            for (StockExchange exchange : StockExchange.values()) {
                verify(priceCollectionService).collect(eq(exchange), eq(TODAY), eq(TODAY),
                        eq(CollectionProgress.NOOP), eq(DailyPriceFetcher.ADJUSTED_DATA_START));
            }
            verify(jobStatusRegistry).start(SchedulerJobName.DAILY_PRICE.name(), exchangeCount);
            verify(jobStatusRegistry).complete(SchedulerJobName.DAILY_PRICE.name());
            verify(systemEventService, never()).recordKisApiFailure(any(), any());
            verify(tradingDateService).register(Exchange.KRX, TODAY);
            verify(tradingDateService).markPricesSynced(Exchange.KRX, TODAY);
        }

        @Test
        @DisplayName("collect가 ParallelException을 던지면 이벤트를 기록하고 다음 거래소를 계속 수집한다")
        void shouldRecordFailureAndContinueWhenCollectThrows() {
            given(tradingDayAdapter.isTradingDay(TODAY)).willReturn(true);
            StockExchange first = StockExchange.values()[0];
            ParallelException failure = new ParallelException(new IllegalStateException("KIS 호출 실패"));
            doThrow(failure).when(priceCollectionService)
                    .collect(eq(first), any(), any(), any(), any());

            job().run();

            int exchangeCount = StockExchange.values().length;
            verify(priceCollectionService, times(exchangeCount))
                    .collect(any(), any(), any(), any(), any());
            verify(systemEventService).recordKisApiFailure(first.name(), "KIS 호출 실패");
            verify(jobStatusRegistry).complete(SchedulerJobName.DAILY_PRICE.name());
            verify(tradingDateService).register(Exchange.KRX, TODAY);
            verify(tradingDateService, never()).markPricesSynced(any(), any());
        }
    }
}
