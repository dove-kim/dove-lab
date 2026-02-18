package com.dove.krxmarketdata.service;

import com.dove.stockdata.entity.MarketCalendar;
import com.dove.stockdata.enums.MarketDayType;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.repository.MarketCalendarRepository;
import com.dove.stockdata.service.StockDataSaveService;
import com.dove.krxmarketdata.dto.KrxStockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrxStockDailySaveServiceTest {

    @Mock
    private KrxStockService krxStockService;

    @Mock
    private StockDataSaveService stockDataSaveService;

    @Mock
    private MarketCalendarRepository marketCalendarRepository;

    @InjectMocks
    private KrxStockDailySaveService krxStockDailySaveService;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2023, 10, 26);
    }

    private KrxStockInfo createMockKrxStockInfo(
            String stockCode, String stockName, LocalDate tradingDate,
            MarketType marketType, Long tradingVolume,
            Long openingPrice, Long closingPrice,
            Long lowestPrice, Long highestPrice) {
        return new KrxStockInfo(
                tradingDate, marketType, stockName, stockCode,
                tradingVolume, openingPrice, closingPrice, lowestPrice, highestPrice
        );
    }

    @Nested
    @DisplayName("saveDailyMarketData - KOSPI+KOSDAQ 통합 조회 및 휴장일 판별")
    class SaveDailyMarketData {

        @Test
        @DisplayName("KOSPI와 KOSDAQ 모두 빈 리스트이면 HOLIDAY로 기록한다")
        void shouldSaveHolidayWhenBothMarketsEmpty() {
            // Given
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(Collections.emptyList());
            when(krxStockService.getStockListBy(eq(MarketType.KOSDAQ), eq(testDate)))
                    .thenReturn(Collections.emptyList());

            // When
            krxStockDailySaveService.saveDailyMarketData(testDate);

            // Then
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDate()).isEqualTo(testDate);
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.HOLIDAY);

            verify(stockDataSaveService, never()).update(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("KOSPI에 데이터가 있으면 TRADING으로 기록하고 저장한다")
        void shouldSaveTradingWhenKospiHasData() {
            // Given
            List<KrxStockInfo> kospiList = List.of(
                    createMockKrxStockInfo("005930", "삼성전자", testDate, MarketType.KOSPI,
                            1000000L, 70000L, 71000L, 69500L, 71500L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(kospiList);
            when(krxStockService.getStockListBy(eq(MarketType.KOSDAQ), eq(testDate)))
                    .thenReturn(Collections.emptyList());

            // When
            krxStockDailySaveService.saveDailyMarketData(testDate);

            // Then — TRADING으로 기록
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDate()).isEqualTo(testDate);
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.TRADING);

            // KOSPI 데이터가 저장됨
            verify(stockDataSaveService, times(1)).update(
                    eq(testDate), eq(MarketType.KOSPI), eq("005930"), eq("삼성전자"),
                    eq(1000000L), eq(70000L), eq(71000L), eq(69500L), eq(71500L));
        }

        @Test
        @DisplayName("KOSDAQ에만 데이터가 있어도 저장한다")
        void shouldSaveTradingWhenOnlyKosdaqHasData() {
            // Given
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(Collections.emptyList());
            List<KrxStockInfo> kosdaqList = List.of(
                    createMockKrxStockInfo("069110", "3H", testDate, MarketType.KOSDAQ,
                            39144L, 1050L, 1030L, 1015L, 1070L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSDAQ), eq(testDate)))
                    .thenReturn(kosdaqList);

            // When
            krxStockDailySaveService.saveDailyMarketData(testDate);

            // Then — TRADING으로 기록
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDate()).isEqualTo(testDate);
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.TRADING);

            // KOSDAQ 데이터가 저장됨
            verify(stockDataSaveService, times(1)).update(
                    eq(testDate), eq(MarketType.KOSDAQ), eq("069110"), eq("3H"),
                    eq(39144L), eq(1050L), eq(1030L), eq(1015L), eq(1070L));
        }

        @Test
        @DisplayName("KOSPI와 KOSDAQ 모두 데이터가 있으면 전부 저장한다")
        void shouldSaveAllWhenBothMarketsHaveData() {
            // Given
            List<KrxStockInfo> kospiList = List.of(
                    createMockKrxStockInfo("005930", "삼성전자", testDate, MarketType.KOSPI,
                            1000000L, 70000L, 71000L, 69500L, 71500L)
            );
            List<KrxStockInfo> kosdaqList = List.of(
                    createMockKrxStockInfo("069110", "3H", testDate, MarketType.KOSDAQ,
                            39144L, 1050L, 1030L, 1015L, 1070L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(kospiList);
            when(krxStockService.getStockListBy(eq(MarketType.KOSDAQ), eq(testDate)))
                    .thenReturn(kosdaqList);

            // When
            krxStockDailySaveService.saveDailyMarketData(testDate);

            // Then — TRADING으로 기록
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.TRADING);

            // 두 시장 데이터 모두 저장됨
            verify(stockDataSaveService, times(2)).update(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }
    }
}
