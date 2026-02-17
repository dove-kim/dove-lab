package com.dove.stockkrxdata.service;

import com.dove.stockkrxdata.dto.KrxStockInfo;
import com.dove.stockkrxdata.enums.KrxMarketType;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.service.StockDataSaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrxStockDailySaveServiceTest {

    @Mock
    private KrxStockService krxStockService;

    @Mock
    private StockDataSaveService stockDataSaveService;

    @InjectMocks
    private KrxStockDailySaveService krxStockDailySaveService;

    private LocalDate testDate;
    private MarketType testMarketType;
    private KrxMarketType testKrxMarketType;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2023, 10, 26);
        testMarketType = MarketType.KOSPI;
        testKrxMarketType = KrxMarketType.KOSPI;
    }

    private KrxStockInfo createMockKrxStockInfo(
            String stockCode, String stockName, LocalDate tradingDate,
            KrxMarketType krxMarketType, Long tradingVolume,
            BigDecimal openingPrice, BigDecimal closingPrice,
            BigDecimal lowestPrice, BigDecimal highestPrice) {
        return new KrxStockInfo(
                tradingDate, krxMarketType, stockName, stockCode,
                tradingVolume, openingPrice, closingPrice, lowestPrice, highestPrice
        );
    }

    @Test
    @DisplayName("KRX 주식 데이터를 성공적으로 조회하고 저장한다")
    void saveKrxDailyStockData_success() {
        // Given
        List<KrxStockInfo> mockKrxStockInfoList = Arrays.asList(
                createMockKrxStockInfo(
                        "005930", "삼성전자", testDate, testKrxMarketType,
                        1000000L, new BigDecimal("70000"), new BigDecimal("71000"),
                        new BigDecimal("69500"), new BigDecimal("71500")
                ),
                createMockKrxStockInfo(
                        "000660", "SK하이닉스", testDate, testKrxMarketType,
                        500000L, new BigDecimal("100000"), new BigDecimal("101000"),
                        new BigDecimal("99000"), new BigDecimal("102000")
                )
        );
        when(krxStockService.getStockListBy(eq(testKrxMarketType), eq(testDate)))
                .thenReturn(mockKrxStockInfoList);

        // When
        krxStockDailySaveService.saveKrxDailyStockData(testMarketType, testDate);

        // Then
        verify(krxStockService, times(1)).getStockListBy(eq(testKrxMarketType), eq(testDate));

        // stockDataSaveService.update가 각 KrxStockInfo에 대해 한 번씩 호출되었는지 검증
        // 총 2개의 KrxStockInfo가 있으므로 2번 호출되어야 함
        verify(stockDataSaveService, times(2)).update(
                any(LocalDate.class),
                any(MarketType.class),
                any(String.class),
                any(String.class),
                any(Long.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        );

        // 첫 번째 주식 정보에 대한 update 호출 검증 (세부 인자 확인)
        verify(stockDataSaveService, times(1)).update(
                eq(testDate),
                eq(testKrxMarketType.toMarketType()),
                eq("005930"),
                eq("삼성전자"),
                eq(1000000L),
                eq(new BigDecimal("70000")),
                eq(new BigDecimal("71000")),
                eq(new BigDecimal("69500")),
                eq(new BigDecimal("71500"))
        );

        // 두 번째 주식 정보에 대한 update 호출 검증 (세부 인자 확인)
        verify(stockDataSaveService, times(1)).update(
                eq(testDate),
                eq(testKrxMarketType.toMarketType()),
                eq("000660"),
                eq("SK하이닉스"),
                eq(500000L),
                eq(new BigDecimal("100000")),
                eq(new BigDecimal("101000")),
                eq(new BigDecimal("99000")),
                eq(new BigDecimal("102000"))
        );
    }

    @Test
    @DisplayName("KRX 주식 데이터 조회 결과가 없을 경우 저장을 시도하지 않는다")
    void saveKrxDailyStockData_emptyList() {
        // Given
        // krxStockService.getStockListBy가 빈 리스트를 반환하도록 모의 설정
        when(krxStockService.getStockListBy(eq(testKrxMarketType), eq(testDate)))
                .thenReturn(Collections.emptyList());

        // When
        krxStockDailySaveService.saveKrxDailyStockData(testMarketType, testDate);

        // Then
        // krxStockService.getStockListBy가 올바른 인자로 한 번 호출되었는지 검증
        verify(krxStockService, times(1)).getStockListBy(eq(testKrxMarketType), eq(testDate));

        // stockDataSaveService.update는 호출되지 않았음을 검증
        verify(stockDataSaveService, times(0)).update(
                any(LocalDate.class),
                any(MarketType.class),
                any(String.class),
                any(String.class),
                any(Long.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        );
    }
}
