package com.dove.scheduler.job;

import com.dove.scheduler.service.InvestorCollectService;
import com.dove.scheduler.service.StockDetailService;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.StockEventCollectionService;
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

    @Mock
    private StockDetailService stockDetailService;

    @Mock
    private InvestorCollectService investorCollectService;

    @Mock
    private StockEventCollectionService eventCollectionService;

    private StockDetailJob job() {
        return new StockDetailJob(stockDetailService, investorCollectService,
                eventCollectionService, CLOCK);
    }

    @Nested
    @DisplayName("run() — 상세·투자자·권리이벤트 순차 수집")
    class Run {

        @Test
        @DisplayName("updateAll → collectAll → 권리이벤트 collect를 순서대로 호출한다")
        void shouldCollectInOrderWhenAllSucceed() {
            job().run();

            InOrder order = inOrder(stockDetailService, investorCollectService, eventCollectionService);
            order.verify(stockDetailService).updateAll();
            order.verify(investorCollectService).collectAll(TODAY);
            order.verify(eventCollectionService).collect(TODAY, TODAY, CollectionProgress.NOOP);
        }

        @Test
        @DisplayName("updateAll이 예외를 던지면 이후 단계를 건너뛰고 예외를 전파한다")
        void shouldPropagateAndSkipRestWhenUpdateAllThrows() {
            RuntimeException boom = new RuntimeException("상세 갱신 실패");
            doThrow(boom).when(stockDetailService).updateAll();

            assertThatThrownBy(job()::run).isSameAs(boom);

            verify(investorCollectService, never()).collectAll(any());
            verify(eventCollectionService, never()).collect(any(), any(), any());
        }

        @Test
        @DisplayName("권리이벤트 collect가 예외를 던져도 잡은 정상 완료한다")
        void shouldSwallowWhenEventCollectThrows() {
            doThrow(new RuntimeException("권리이벤트 실패")).when(eventCollectionService)
                    .collect(any(), any(), any());

            job().run();

            verify(stockDetailService).updateAll();
            verify(investorCollectService).collectAll(TODAY);
            verify(eventCollectionService).collect(eq(TODAY), eq(TODAY), eq(CollectionProgress.NOOP));
        }
    }
}
