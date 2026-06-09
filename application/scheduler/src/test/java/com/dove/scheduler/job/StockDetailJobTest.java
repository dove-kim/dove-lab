package com.dove.scheduler.job;

import com.dove.concurrent.ParallelException;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.scheduler.service.InvestorCollectService;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.StockDetailCollectionService;
import com.dove.stockcollection.application.service.StockEventCollectionService;
import com.dove.systemevent.application.service.SystemEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link StockDetailJob} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockDetailJob")
class StockDetailJobTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    private static final String JOB = SchedulerJobName.STOCK_DETAIL.name();

    @Mock private StockDetailCollectionService stockDetailCollectionService;
    @Mock private InvestorCollectService investorCollectService;
    @Mock private StockEventCollectionService eventCollectionService;
    @Mock private JobStatusRegistry jobStatusRegistry;
    @Mock private SystemEventService systemEventService;

    private StockDetailJob job() {
        return new StockDetailJob(stockDetailCollectionService, investorCollectService,
                eventCollectionService, jobStatusRegistry, systemEventService, CLOCK);
    }

    @Nested
    @DisplayName("run() — 정상 수집")
    class RunSuccess {

        @Test
        @DisplayName("updateAll → complete → collectAll → 권리이벤트 collect를 순서대로 호출한다")
        void shouldCollectInOrderWhenAllSucceed() {
            job().run();

            InOrder order = inOrder(stockDetailCollectionService, jobStatusRegistry,
                    investorCollectService, eventCollectionService);
            order.verify(stockDetailCollectionService).updateAll(any(CollectionProgress.class));
            order.verify(jobStatusRegistry).complete(JOB);
            order.verify(investorCollectService).collectAll(TODAY);
            order.verify(eventCollectionService).collect(TODAY, TODAY, CollectionProgress.NOOP);
        }
    }

    @Nested
    @DisplayName("run() — KIS 오류")
    class RunKisFailure {

        @Test
        @DisplayName("updateAll이 ParallelException을 던지면 KIS 실패를 기록하고 이후 단계를 건너뛴다")
        void shouldRecordFailureAndSkipRestWhenUpdateAllThrows() {
            ParallelException boom = new ParallelException(new RuntimeException("KIS down"));
            doThrow(boom).when(stockDetailCollectionService).updateAll(any());

            assertThatThrownBy(job()::run).isSameAs(boom);

            verify(systemEventService).recordKisApiFailure(eq(JOB), anyString());
            verify(jobStatusRegistry).fail(eq(JOB), anyString());
            verify(investorCollectService, never()).collectAll(any());
            verify(eventCollectionService, never()).collect(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("run() — 권리이벤트 오류")
    class RunEventFailure {

        @Test
        @DisplayName("권리이벤트 collect가 예외를 던져도 잡은 정상 완료한다")
        void shouldSwallowWhenEventCollectThrows() {
            doThrow(new RuntimeException("권리이벤트 실패")).when(eventCollectionService)
                    .collect(any(), any(), any());

            job().run();

            verify(stockDetailCollectionService).updateAll(any(CollectionProgress.class));
            verify(jobStatusRegistry).complete(JOB);
            verify(investorCollectService).collectAll(TODAY);
            verify(eventCollectionService).collect(eq(TODAY), eq(TODAY), eq(CollectionProgress.NOOP));
        }
    }
}
