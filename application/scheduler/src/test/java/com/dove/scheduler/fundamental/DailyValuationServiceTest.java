package com.dove.scheduler.fundamental;

import com.dove.fundamental.application.FundamentalCommandService;
import com.dove.fundamental.application.FundamentalTtmService;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockShareCountService;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stockcollection.application.service.CollectionProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyValuationServiceTest {

    @Mock
    private StockPriceQueryService priceQueryService;
    @Mock
    private FundamentalTtmService ttmService;
    @Mock
    private StockShareCountService shareCountService;
    @Mock
    private FundamentalCommandService fundamentalCommandService;
    @Mock
    private ObjectProvider<DailyValuationService> self;
    @InjectMocks
    private DailyValuationService service;

    @Nested
    @DisplayName("computeRange")
    class ComputeRange {

        @Test
        void shouldComputeEachDayAndReportTotalWhenRangeGiven() {
            // computeRange가 프록시(self) 경유로 하루 단위 compute를 호출
            when(self.getObject()).thenReturn(service);
            when(priceQueryService.findByExchangesAndDate(anyList(), eq(PriceType.RAW), any()))
                    .thenReturn(Map.of());
            CollectionProgress progress = mock(CollectionProgress.class);

            int saved = service.computeRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), progress);

            assertThat(saved).isZero();
            verify(progress).onTotal(3);
            verify(priceQueryService, times(3)).findByExchangesAndDate(anyList(), eq(PriceType.RAW), any());
            verify(progress, times(3)).onProgress(anyInt());
        }
    }
}
