package com.dove.stockcollection.application.service;

import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionLauncherTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK =
            Clock.fixed(LocalDate.of(2026, 5, 31).atStartOfDay(SEOUL).toInstant(), SEOUL); // 오늘=5/31

    private final PriceCollectionService priceCollectionService = mock(PriceCollectionService.class);
    private final StockCollectionService stockCollectionService = mock(StockCollectionService.class);
    private final StockEventCollectionService eventCollectionService = mock(StockEventCollectionService.class);
    private final InvestorCollectionService investorCollectionService = mock(InvestorCollectionService.class);
    private final CollectionTaskService taskService = mock(CollectionTaskService.class);
    private final CollectionLauncher launcher = new CollectionLauncher(
            priceCollectionService, stockCollectionService, eventCollectionService,
            investorCollectionService, taskService, CLOCK);

    @Test
    void shouldCapToYesterdayWhenRangeIncludesToday() {
        when(taskService.create(any(), any(), any(), any(), any())).thenReturn(1L);

        launcher.enqueuePriceCollection(StockExchange.KOSPI,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), 7L); // to=오늘

        // to 가 어제(5/30)로 캡되어 구조화 파라미터로 전달
        verify(taskService).create(eq(CollectionType.PRICE), eq(StockExchange.KOSPI),
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 30)), eq(7L));
    }

    @Test
    void shouldRejectWhenEntireRangeIsTodayOrFuture() {
        assertThatThrownBy(() -> launcher.enqueuePriceCollection(StockExchange.KOSPI,
                LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 30), 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_BACKFILL_RANGE");

        verify(taskService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReenqueuePriceFromStructuredParams() {
        CollectionTask source = new CollectionTask(CollectionType.PRICE, StockExchange.KOSDAQ,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 7L);
        when(taskService.find(1L)).thenReturn(Optional.of(source));
        when(taskService.create(any(), any(), any(), any(), any())).thenReturn(99L);

        launcher.reenqueue(1L, 7L);

        // 문자열 파싱 없이 구조화 컬럼(거래소·기간) 그대로 재등록
        verify(taskService).create(eq(CollectionType.PRICE), eq(StockExchange.KOSDAQ),
                eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 12, 31)), eq(7L));
    }

    @Test
    void shouldReenqueueEventFromStructuredParams() {
        CollectionTask source = new CollectionTask(CollectionType.EVENT, null,
                LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 31), 7L);
        when(taskService.find(2L)).thenReturn(Optional.of(source));
        when(taskService.create(any(), any(), any(), any(), any())).thenReturn(88L);

        launcher.reenqueue(2L, 7L);

        verify(taskService).create(eq(CollectionType.EVENT), eq(null),
                eq(LocalDate.of(2023, 3, 1)), eq(LocalDate.of(2023, 3, 31)), eq(7L));
    }

    @Test
    void shouldThrowWhenReenqueueSourceNotFound() {
        when(taskService.find(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> launcher.reenqueue(404L, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TASK_NOT_FOUND");
    }
}
