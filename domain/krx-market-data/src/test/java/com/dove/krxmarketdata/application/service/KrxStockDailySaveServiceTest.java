package com.dove.krxmarketdata.application.service;

import com.dove.stockdata.domain.entity.MarketCalendar;
import com.dove.stockdata.domain.enums.MarketDayType;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.MarketCalendarRepository;
import com.dove.stockdata.application.service.StockDataSaveService;
import com.dove.krxmarketdata.application.dto.KrxStockInfo;
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
import java.util.Optional;

import com.dove.stockdata.domain.entity.MarketCalendarId;

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
    @DisplayName("saveDailyMarketDataByMarket - 단일 시장 조회 및 저장")
    class SaveDailyMarketDataByMarket {

        @Test
        @DisplayName("KOSPI 단일 시장 조회 및 저장")
        void shouldSaveStockDataForSingleMarket() {
            // Given
            List<KrxStockInfo> kospiList = List.of(
                    createMockKrxStockInfo("005930", "삼성전자", testDate, MarketType.KOSPI,
                            1000000L, 70000L, 71000L, 69500L, 71500L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(kospiList);

            // When
            List<String> result = krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSPI);

            // Then
            assertThat(result).containsExactly("005930");
            verify(stockDataSaveService, times(1)).update(
                    eq(testDate), eq(MarketType.KOSPI), eq("005930"), eq("삼성전자"),
                    eq(1000000L), eq(70000L), eq(71000L), eq(69500L), eq(71500L));
        }

        @Test
        @DisplayName("KOSDAQ 단일 시장 조회 및 저장")
        void shouldSaveKosdaqStockData() {
            // Given
            List<KrxStockInfo> kosdaqList = List.of(
                    createMockKrxStockInfo("069110", "3H", testDate, MarketType.KOSDAQ,
                            39144L, 1050L, 1030L, 1015L, 1070L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSDAQ), eq(testDate)))
                    .thenReturn(kosdaqList);

            // When
            List<String> result = krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSDAQ);

            // Then
            assertThat(result).containsExactly("069110");
            verify(stockDataSaveService, times(1)).update(
                    eq(testDate), eq(MarketType.KOSDAQ), eq("069110"), eq("3H"),
                    eq(39144L), eq(1050L), eq(1030L), eq(1015L), eq(1070L));
        }

        @Test
        @DisplayName("데이터가 있으면 해당 시장을 TRADING으로 기록한다")
        void shouldSaveTradingCalendarWhenMarketHasData() {
            // Given
            List<KrxStockInfo> kospiList = List.of(
                    createMockKrxStockInfo("005930", "삼성전자", testDate, MarketType.KOSPI,
                            1000000L, 70000L, 71000L, 69500L, 71500L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(kospiList);

            // When
            krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSPI);

            // Then
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDate()).isEqualTo(testDate);
            assertThat(captor.getValue().getMarketType()).isEqualTo(MarketType.KOSPI);
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.TRADING);
        }

        @Test
        @DisplayName("데이터가 없으면 해당 시장을 HOLIDAY로 기록한다")
        void shouldSaveHolidayCalendarWhenMarketDataIsEmpty() {
            // Given
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(Collections.emptyList());

            // When
            List<String> result = krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSPI);

            // Then
            assertThat(result).isEmpty();
            ArgumentCaptor<MarketCalendar> captor = ArgumentCaptor.forClass(MarketCalendar.class);
            verify(marketCalendarRepository).save(captor.capture());
            assertThat(captor.getValue().getDate()).isEqualTo(testDate);
            assertThat(captor.getValue().getMarketType()).isEqualTo(MarketType.KOSPI);
            assertThat(captor.getValue().getDayType()).isEqualTo(MarketDayType.HOLIDAY);

            verify(stockDataSaveService, never()).update(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 저장된 날짜+시장으로 다시 호출하면 기존 MarketCalendar의 dayType을 업데이트한다")
        void shouldUpdateExistingMarketCalendarWhenCalledAgain() {
            // Given
            MarketCalendar existing = new MarketCalendar(testDate, MarketType.KOSPI, MarketDayType.HOLIDAY);
            when(marketCalendarRepository.findById(new MarketCalendarId(testDate, MarketType.KOSPI)))
                    .thenReturn(Optional.of(existing));
            List<KrxStockInfo> kospiList = List.of(
                    createMockKrxStockInfo("005930", "삼성전자", testDate, MarketType.KOSPI,
                            1000000L, 70000L, 71000L, 69500L, 71500L)
            );
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(kospiList);

            // When
            krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSPI);

            // Then
            assertThat(existing.getDayType()).isEqualTo(MarketDayType.TRADING);
        }

        @Test
        @DisplayName("KOSPI 요청 시 KOSDAQ API를 호출하지 않는다")
        void shouldNotCallOtherMarketApi() {
            // Given
            when(krxStockService.getStockListBy(eq(MarketType.KOSPI), eq(testDate)))
                    .thenReturn(Collections.emptyList());

            // When
            krxStockDailySaveService.saveDailyMarketDataByMarket(testDate, MarketType.KOSPI);

            // Then
            verify(krxStockService, times(1)).getStockListBy(eq(MarketType.KOSPI), eq(testDate));
            verify(krxStockService, never()).getStockListBy(eq(MarketType.KOSDAQ), any());
        }
    }
}
