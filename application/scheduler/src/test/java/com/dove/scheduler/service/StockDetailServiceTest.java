package com.dove.scheduler.service;

import com.dove.concurrent.ParallelException;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.kis.infrastructure.adapter.KisStockDetailFetcher;
import com.dove.kis.infrastructure.client.dto.KisProductInfoOutput;
import com.dove.kis.infrastructure.client.dto.KisStockInfoOutput;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.enums.TagField;
import com.dove.systemevent.application.service.SystemEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * StockDetailService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class StockDetailServiceTest {

    @Mock KisStockDetailFetcher fetcher;
    @Mock StockQueryService stockQueryService;
    @Mock StockCommandService stockDetailCommandService;
    @Mock StockTagValueService tagValueCommandService;
    @Mock SystemEventService systemEventService;
    @Mock JobStatusRegistry jobStatusRegistry;
    @InjectMocks StockDetailService service;

    private static final String JOB = SchedulerJobName.STOCK_DETAIL.name();

    @BeforeEach
    void setUp() {
        // @Value 미주입 시 0 → Parallel의 Semaphore(0)로 영구 블로킹되므로 직접 설정
        ReflectionTestUtils.setField(service, "concurrency", 4);
    }

    @Nested
    @DisplayName("updateAll — 정상 수집")
    class UpdateAllSuccess {

        @Test
        @DisplayName("종목별 상세를 적용하고 jobStatus start/complete를 호출하며 태그를 distinct로 등록한다")
        void shouldApplyDetailsAndRegisterDistinctTagsWhenSuccess() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930", "000660"));

            KisStockInfoOutput stockInfo = mock(KisStockInfoOutput.class);
            given(stockInfo.getIdxBztpLclsNm()).willReturn("전기전자");
            KisProductInfoOutput productInfo = mock(KisProductInfoOutput.class);
            given(productInfo.getPrdtClsfName()).willReturn("주식");

            // 한 종목은 정보 존재, 다른 종목은 empty(미존재 경로)
            given(fetcher.fetchStockInfo("005930")).willReturn(Optional.of(stockInfo));
            given(fetcher.fetchStockInfo("000660")).willReturn(Optional.empty());
            given(fetcher.fetchProductInfo("005930")).willReturn(Optional.of(productInfo));
            given(fetcher.fetchProductInfo("000660")).willReturn(Optional.empty());

            service.updateAll();

            verify(jobStatusRegistry).start(JOB, 2);
            verify(jobStatusRegistry).complete(JOB);
            verify(stockDetailCommandService).applyStockInfo(eq("005930"),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            verify(stockDetailCommandService).applyProductInfo(eq("005930"),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
            verify(tagValueCommandService).registerIfAbsent(TagField.INDUSTRY_LCLS.name(), "전기전자");
            verify(tagValueCommandService).registerIfAbsent(TagField.PRDT_CLSF.name(), "주식");
            verify(systemEventService, never()).recordKisApiFailure(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("updateAll — KIS 오류")
    class UpdateAllFailure {

        @Test
        @DisplayName("fetch 예외 시 KIS 실패를 기록하고 jobStatus.fail 후 ParallelException을 rethrow한다")
        void shouldRecordFailureAndRethrowWhenFetchThrows() {
            given(stockQueryService.findAllTickers()).willReturn(List.of("005930"));
            given(fetcher.fetchStockInfo("005930")).willThrow(new RuntimeException("KIS down"));

            assertThatThrownBy(() -> service.updateAll())
                    .isInstanceOf(ParallelException.class);

            verify(systemEventService).recordKisApiFailure(eq(JOB), anyString());
            verify(jobStatusRegistry).fail(eq(JOB), anyString());
            verify(jobStatusRegistry, never()).complete(JOB);
        }
    }
}
