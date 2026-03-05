package com.dove.technicalindicator.application.service;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataQueryRepository;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicalIndicatorCalculationService 테스트")
class TechnicalIndicatorCalculationServiceTest {

    @Mock
    private StockDataQueryRepository stockDataQueryRepository;

    @Mock
    private TechnicalIndicatorRepository technicalIndicatorRepository;

    @Mock
    private TechnicalIndicatorCalculator calculator;

    private TechnicalIndicatorCalculationService service;

    private final MarketType marketType = MarketType.KOSPI;
    private final String stockCode = "005930";
    private final LocalDate tradeDate = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        service = new TechnicalIndicatorCalculationService(
                stockDataQueryRepository, technicalIndicatorRepository, List.of(calculator));
    }

    private StockData createStockData(LocalDate date, long closePrice) {
        return new StockData(MarketType.KOSPI, stockCode, date,
                1000L, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("데이터를 조회하고 계산하여 저장한다")
    void shouldCalculateAndSaveSmaForStock() {
        // Given
        when(calculator.requiredDataSize()).thenReturn(5);

        List<StockData> recentData = List.of(
                createStockData(LocalDate.of(2024, 1, 15), 500),
                createStockData(LocalDate.of(2024, 1, 14), 400),
                createStockData(LocalDate.of(2024, 1, 13), 300),
                createStockData(LocalDate.of(2024, 1, 12), 200),
                createStockData(LocalDate.of(2024, 1, 11), 100));

        when(stockDataQueryRepository.findRecentStockData(
                eq(marketType), eq(stockCode), eq(tradeDate), eq(5)))
                .thenReturn(recentData);

        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 300.0));

        // When
        service.calculateForStock(marketType, stockCode, tradeDate);

        // Then
        ArgumentCaptor<TechnicalIndicator> captor = ArgumentCaptor.forClass(TechnicalIndicator.class);
        verify(technicalIndicatorRepository).save(captor.capture());

        TechnicalIndicator saved = captor.getValue();
        assertThat(saved.getId().getIndicatorName()).isEqualTo(IndicatorType.SMA_5);
        assertThat(saved.getIndicatorValue()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("데이터가 부족하면 계산을 건너뛴다")
    void shouldSkipCalculationWhenInsufficientData() {
        // Given
        when(calculator.requiredDataSize()).thenReturn(5);

        List<StockData> insufficientData = List.of(
                createStockData(LocalDate.of(2024, 1, 15), 500),
                createStockData(LocalDate.of(2024, 1, 14), 400));

        when(stockDataQueryRepository.findRecentStockData(
                eq(marketType), eq(stockCode), eq(tradeDate), eq(5)))
                .thenReturn(insufficientData);

        // When
        service.calculateForStock(marketType, stockCode, tradeDate);

        // Then
        verify(calculator, never()).calculate(any());
        verify(technicalIndicatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("하나의 계산기가 실패해도 나머지는 계속 실행한다")
    void shouldContinueWithOtherCalculatorsWhenOneFails() {
        // Given
        TechnicalIndicatorCalculator failingCalc = mock(TechnicalIndicatorCalculator.class);
        TechnicalIndicatorCalculator succeedingCalc = mock(TechnicalIndicatorCalculator.class);

        TechnicalIndicatorCalculationService svc = new TechnicalIndicatorCalculationService(
                stockDataQueryRepository, technicalIndicatorRepository,
                List.of(failingCalc, succeedingCalc));

        List<StockData> data = List.of(
                createStockData(LocalDate.of(2024, 1, 15), 500),
                createStockData(LocalDate.of(2024, 1, 14), 400),
                createStockData(LocalDate.of(2024, 1, 13), 300),
                createStockData(LocalDate.of(2024, 1, 12), 200),
                createStockData(LocalDate.of(2024, 1, 11), 100));

        when(failingCalc.requiredDataSize()).thenReturn(5);
        when(failingCalc.getName()).thenReturn("FAILING");
        when(succeedingCalc.requiredDataSize()).thenReturn(5);

        when(stockDataQueryRepository.findRecentStockData(any(), any(), any(), anyInt()))
                .thenReturn(data);
        when(failingCalc.calculate(any())).thenThrow(new RuntimeException("계산 오류"));
        when(succeedingCalc.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 300.0));

        // When
        svc.calculateForStock(marketType, stockCode, tradeDate);

        // Then
        verify(succeedingCalc).calculate(any());
        verify(technicalIndicatorRepository).save(any());
    }

    @Test
    @DisplayName("계산기에 데이터를 날짜 오름차순으로 전달한다")
    @SuppressWarnings("unchecked")
    void shouldPassDataInAscendingDateOrder() {
        // Given
        when(calculator.requiredDataSize()).thenReturn(3);

        List<StockData> descData = List.of(
                createStockData(LocalDate.of(2024, 1, 15), 300),
                createStockData(LocalDate.of(2024, 1, 14), 200),
                createStockData(LocalDate.of(2024, 1, 13), 100));

        when(stockDataQueryRepository.findRecentStockData(any(), any(), any(), anyInt()))
                .thenReturn(descData);
        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 200.0));

        // When
        service.calculateForStock(marketType, stockCode, tradeDate);

        // Then
        ArgumentCaptor<List<StockData>> captor = ArgumentCaptor.forClass(List.class);
        verify(calculator).calculate(captor.capture());

        List<StockData> passedData = captor.getValue();
        assertThat(passedData.get(0).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 13));
        assertThat(passedData.get(1).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 14));
        assertThat(passedData.get(2).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    @DisplayName("비순차 데이터 도착 시 후속 날짜도 재계산한다")
    void shouldRecalculateFutureDatesWhenDataArrivesLate() {
        // Given - 10월 1일 데이터가 늦게 도착, 10월 2일/3일 데이터는 이미 존재
        LocalDate oct1 = LocalDate.of(2024, 10, 1);
        LocalDate oct2 = LocalDate.of(2024, 10, 2);
        LocalDate oct3 = LocalDate.of(2024, 10, 3);

        when(stockDataQueryRepository.findTradeDatesAfter(marketType, stockCode, oct1))
                .thenReturn(List.of(oct2, oct3));

        when(calculator.requiredDataSize()).thenReturn(2);

        List<StockData> dataForOct1 = List.of(
                createStockData(oct1, 100), createStockData(LocalDate.of(2024, 9, 30), 90));
        List<StockData> dataForOct2 = List.of(
                createStockData(oct2, 110), createStockData(oct1, 100));
        List<StockData> dataForOct3 = List.of(
                createStockData(oct3, 120), createStockData(oct2, 110));

        when(stockDataQueryRepository.findRecentStockData(eq(marketType), eq(stockCode), eq(oct1), anyInt()))
                .thenReturn(dataForOct1);
        when(stockDataQueryRepository.findRecentStockData(eq(marketType), eq(stockCode), eq(oct2), anyInt()))
                .thenReturn(dataForOct2);
        when(stockDataQueryRepository.findRecentStockData(eq(marketType), eq(stockCode), eq(oct3), anyInt()))
                .thenReturn(dataForOct3);

        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 100.0));

        // When
        service.calculateWithRecalculation(marketType, stockCode, oct1);

        // Then - 10/1, 10/2, 10/3 총 3회 계산
        verify(calculator, times(3)).calculate(any());
        verify(technicalIndicatorRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("후속 날짜가 없으면 해당 날짜만 계산한다")
    void shouldOnlyCalculateCurrentDateWhenNoFutureDates() {
        // Given
        when(stockDataQueryRepository.findTradeDatesAfter(marketType, stockCode, tradeDate))
                .thenReturn(List.of());

        when(calculator.requiredDataSize()).thenReturn(2);

        List<StockData> data = List.of(
                createStockData(tradeDate, 100),
                createStockData(tradeDate.minusDays(1), 90));

        when(stockDataQueryRepository.findRecentStockData(any(), any(), any(), anyInt()))
                .thenReturn(data);
        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 95.0));

        // When
        service.calculateWithRecalculation(marketType, stockCode, tradeDate);

        // Then
        verify(calculator, times(1)).calculate(any());
    }
}
