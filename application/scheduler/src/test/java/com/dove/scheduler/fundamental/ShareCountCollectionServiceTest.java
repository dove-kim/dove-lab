package com.dove.scheduler.fundamental;

import com.dove.stock.application.service.StockShareCountService;
import com.dove.stock.domain.entity.StockShareCount;
import com.dove.stockcollection.application.port.ShareCountFetcher;
import com.dove.stockcollection.application.port.ShareCountRow;
import com.dove.stockcollection.application.service.CollectionProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareCountCollectionServiceTest {

    @Mock
    private ShareCountFetcher fetcher;
    @Mock
    private StockShareCountService shareCountService;
    @InjectMocks
    private ShareCountCollectionService service;

    @Nested
    @DisplayName("collect — 변경이력 저장")
    class Collect {

        @Test
        @DisplayName("직전값과 다를 때만 저장한다(동일값은 건너뜀)")
        void shouldSaveOnlyOnChange() {
            LocalDate d1 = LocalDate.of(2024, 1, 1);
            LocalDate d2 = LocalDate.of(2024, 1, 2);
            LocalDate d3 = LocalDate.of(2024, 1, 3);
            when(fetcher.fetch(d1)).thenReturn(List.of(new ShareCountRow("005930", 100L)));
            when(fetcher.fetch(d2)).thenReturn(List.of(new ShareCountRow("005930", 100L)));
            when(fetcher.fetch(d3)).thenReturn(List.of(new ShareCountRow("005930", 200L)));
            // 첫 관측일에만 as-of 직전값 조회 → 없음
            when(shareCountService.findAsOf(eq("005930"), any())).thenReturn(Optional.empty());

            int saved = service.collect(d1, d3, CollectionProgress.NOOP);

            assertThat(saved).isEqualTo(2);   // d1(신규) + d3(변경), d2 동일값 skip
            ArgumentCaptor<StockShareCount> captor = ArgumentCaptor.forClass(StockShareCount.class);
            verify(shareCountService, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(StockShareCount::getEffectiveDate)
                    .containsExactly(d1, d3);
            assertThat(captor.getAllValues()).extracting(StockShareCount::getListedShares)
                    .containsExactly(100L, 200L);
        }
    }
}
