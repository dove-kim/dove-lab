package com.dove.scheduler.service;

import com.dove.indicator.application.service.IndicatorBulkCalculateService;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link IndicatorComputeService} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class IndicatorComputeServiceTest {

    @Mock StockQueryService stockQueryService;
    @Mock IndicatorBulkCalculateService bulkCalculateService;
    @Mock JobStatusRegistry jobStatusRegistry;

    @InjectMocks IndicatorComputeService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 7);
    private static final String JOB = SchedulerJobName.INDICATOR.name();

    /** 한 티커당 그룹 수 = 거래소 × 가격유형. */
    private static final int GROUPS_PER_TICKER =
            StockExchange.values().length * PriceType.values().length;

    @BeforeEach
    void setUp() {
        // @Value 미주입 시 0 → Parallel.run의 Semaphore(0)가 영구 블로킹되므로 직접 주입.
        ReflectionTestUtils.setField(service, "concurrency", 4);
        // 기본은 시작일 하한 없음(전체 이력).
        ReflectionTestUtils.setField(service, "startDateProp", "");
    }

    @Nested
    @DisplayName("computeAll — 정상 계산")
    class ComputeAllSuccess {

        @Test
        @DisplayName("티커마다 거래소×가격유형 그룹 수만큼 calculateGroup 호출하고 start·complete 호출")
        void shouldCalculateEveryGroupAndCompleteWhenTickersExist() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));

            service.computeAll(TODAY);

            verify(bulkCalculateService, times(GROUPS_PER_TICKER))
                    .calculateGroup(eq("005930"), any(), any(), eq(TODAY), any());
            verify(jobStatusRegistry).start(JOB, GROUPS_PER_TICKER);
            verify(jobStatusRegistry).complete(JOB);
        }
    }

    @Nested
    @DisplayName("computeAll — 그룹 예외 격리 (best-effort)")
    class ComputeAllSwallowsGroupErrors {

        @Test
        @DisplayName("한 그룹이 예외를 던져도 삼키고 나머지를 계속 계산한 뒤 complete 호출")
        void shouldSwallowGroupErrorAndCompleteWhenCalculateThrows() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));
            // 모든 그룹 호출이 예외를 던져도 ParallelException으로 전파되지 않아야 한다.
            willThrow(new RuntimeException("계산 실패"))
                    .given(bulkCalculateService)
                    .calculateGroup(anyString(), any(), any(), any(), any());

            assertThatNoException().isThrownBy(() -> service.computeAll(TODAY));

            verify(bulkCalculateService, times(GROUPS_PER_TICKER))
                    .calculateGroup(anyString(), any(), any(), any(), any());
            verify(jobStatusRegistry).complete(JOB);
        }
    }

    @Nested
    @DisplayName("computeAll — 시작일 하한 전달")
    class StartFloorPropagation {

        @Test
        @DisplayName("start-date 설정 시 해당 LocalDate를 startFloor로 전달")
        void shouldPassParsedStartFloorWhenStartDateConfigured() {
            ReflectionTestUtils.setField(service, "startDateProp", "2026-01-01");
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));

            service.computeAll(TODAY);

            ArgumentCaptor<LocalDate> floor = ArgumentCaptor.forClass(LocalDate.class);
            verify(bulkCalculateService, times(GROUPS_PER_TICKER))
                    .calculateGroup(anyString(), any(), any(), eq(TODAY), floor.capture());
            assertThat(floor.getAllValues()).containsOnly(LocalDate.of(2026, 1, 1));
        }

        @Test
        @DisplayName("start-date가 빈 문자열이면 startFloor로 null 전달")
        void shouldPassNullStartFloorWhenStartDateBlank() {
            ReflectionTestUtils.setField(service, "startDateProp", "");
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));

            service.computeAll(TODAY);

            ArgumentCaptor<LocalDate> floor = ArgumentCaptor.forClass(LocalDate.class);
            verify(bulkCalculateService, times(GROUPS_PER_TICKER))
                    .calculateGroup(anyString(), any(), any(), eq(TODAY), floor.capture());
            assertThat(floor.getAllValues()).containsOnlyNulls();
        }
    }
}
