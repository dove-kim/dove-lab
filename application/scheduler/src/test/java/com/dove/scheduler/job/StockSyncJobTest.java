package com.dove.scheduler.job;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.service.StockSyncService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link StockSyncJob} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockSyncJob")
class StockSyncJobTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TO = LocalDate.now(CLOCK);
    private static final LocalDate FROM = TO.minusDays(30);

    @Mock
    private StockSyncService stockSyncService;

    @Mock
    private JobStatusRegistry jobStatusRegistry;

    @Nested
    @DisplayName("run() — 최근 30일 시장별 동기화")
    class Run {

        @Test
        @DisplayName("시장 수만큼 syncRange를 호출하고 total=일수×시장수로 start·complete한다")
        void shouldSyncEachMarketAndCompleteWhenAllSucceed() {
            StockSyncJob job = new StockSyncJob(stockSyncService, jobStatusRegistry, CLOCK);

            job.run();

            int marketCount = MarketType.KRX_MARKETS.size();
            long expectedTotal = 31L * marketCount;
            for (MarketType market : MarketType.KRX_MARKETS) {
                verify(stockSyncService).syncRange(eq(market), eq(FROM), eq(TO), any());
            }
            verify(stockSyncService, times(marketCount)).syncRange(any(), any(), any(), any());
            verify(jobStatusRegistry).start(SchedulerJobName.STOCK_SYNC.name(), expectedTotal);
            verify(jobStatusRegistry).complete(SchedulerJobName.STOCK_SYNC.name());
            verify(jobStatusRegistry, never()).fail(any(), any());
        }

        @Test
        @DisplayName("syncRange가 RuntimeException을 던지면 fail을 기록하고 예외를 전파한다")
        void shouldFailAndRethrowWhenSyncRangeThrows() {
            RuntimeException boom = new RuntimeException("동기화 실패");
            doThrow(boom).when(stockSyncService).syncRange(any(), any(), any(), any());
            StockSyncJob job = new StockSyncJob(stockSyncService, jobStatusRegistry, CLOCK);

            assertThatThrownBy(job::run).isSameAs(boom);

            verify(jobStatusRegistry).fail(SchedulerJobName.STOCK_SYNC.name(), "동기화 실패");
            verify(jobStatusRegistry, never()).complete(any());
        }
    }
}
