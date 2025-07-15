package com.dpot.stockdata.domain.service;

import com.dpot.stockdata.domain.enums.StockMarketType;
import com.dpot.stockdata.repository.StockDataRepository;
import com.dpot.stockdata.repository.StockDateRepository;
import com.dpot.stockdata.repository.StockRepository;
import com.dpot.stockdata.repository.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({StockDataSaveService.class})
class StockDataSaveServiceTest {

    @Autowired
    private StockDataSaveService stockDataSaveService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockDateRepository stockDateRepository;

    @Autowired
    private StockDataRepository stockDataRepository;

    @Test
    @Transactional
    @DisplayName("새로운 주식 코드, 날짜 저장 성공")
    void testSaveNewStockAndDailyData() {
        // Given
        LocalDate testBaseDate = LocalDate.of(2023, 10, 26);
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        StockMarketType testMarketType = StockMarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        // When
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );

        // Then
        Optional<Stock> foundStock = stockRepository
                .findById(new StockId(testMarketType, testStockCode));
        assertThat(foundStock).isPresent();
        assertThat(foundStock.get().getId().getCode()).isEqualTo(testStockCode);
        assertThat(foundStock.get().getId().getMarketType()).isEqualTo(testMarketType);
        assertThat(foundStock.get().getName()).isEqualTo(testStockName);

        // StockDate 엔티티 검증
        Optional<StockDate> foundStockDate = stockDateRepository.findById(testBaseDate);
        assertThat(foundStockDate).isPresent();
        assertThat(foundStockDate.get().getDate()).isEqualTo(testBaseDate);

        // StockDailyData 엔티티 검증
        StockDataId dailyDataId = new StockDataId(testStockCode, testBaseDate);
        Optional<StockData> foundDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(foundDailyData).isPresent();
        assertThat(foundDailyData.get().getVolume()).isEqualTo(testVolume);
        assertThat(foundDailyData.get().getOpenPrice()).isEqualByComparingTo(testOpeningPrice);
        assertThat(foundDailyData.get().getClosePrice()).isEqualByComparingTo(testClosingPrice);
        assertThat(foundDailyData.get().getLowPrice()).isEqualByComparingTo(testLowPrice);
        assertThat(foundDailyData.get().getHighPrice()).isEqualByComparingTo(testHighPrice);

        // 관계가 올바르게 설정되었는지 확인
        assertThat(foundDailyData.get().getId().getStockCode()).isEqualTo(testStockCode);
        assertThat(foundDailyData.get().getId().getTradeDate()).isEqualTo(testBaseDate);
    }

    @Test
    @Transactional
    @DisplayName("주식 데이터, 날짜 모두 업데이트")
    void testUpdateExistingStockDateAndExistingDailyData() {
        // Given
        LocalDate testBaseDate = LocalDate.of(2023, 10, 26);
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        StockMarketType testMarketType = StockMarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );
        long initialStockDateCount = stockDateRepository.count(); // 1개
        long initialDailyDataCount = stockDataRepository.count(); // 1개

        // 업데이트할 값
        Long updatedVolume = 2000000L;
        BigDecimal updatedClosingPrice = new BigDecimal("72500.00");

        // When
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName, // Stock 정보는 그대로
                updatedVolume, testOpeningPrice, updatedClosingPrice, testLowPrice, testHighPrice
        );

        // Then:
        // StockDate는 새로 생성되지 않고 기존 것이 사용되었음을 확인
        assertThat(stockDateRepository.count()).isEqualTo(initialStockDateCount); // 여전히 1개

        // StockData가 업데이트되었는지 확인 (새로운 레코드 생성되지 않음)
        assertThat(stockDataRepository.count()).isEqualTo(initialDailyDataCount); // 여전히 1개
        StockDataId dailyDataId = new StockDataId(testStockCode, testBaseDate);
        Optional<StockData> updatedDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(updatedDailyData).isPresent();
        assertThat(updatedDailyData.get().getVolume()).isEqualTo(updatedVolume); // 거래량 업데이트 확인
        assertThat(updatedDailyData.get().getClosePrice()).isEqualByComparingTo(updatedClosingPrice); // 종가 업데이트 확인
    }

    @Test
    @Transactional
    @DisplayName("날짜는 업데이트, 주식 정보는 신규")
    void testUpdateStockMarketTypeForExistingStock() {
        // Given
        LocalDate testBaseDate = LocalDate.of(2023, 10, 26);
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        StockMarketType testMarketType = StockMarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        stockDataSaveService.update(
                testBaseDate, StockMarketType.KOSPI, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );

        // When
        StockMarketType newMarketType = StockMarketType.KONEX;
        stockDataSaveService.update(
                testBaseDate, newMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );

        // Then
        assertThat(stockRepository.count()).isEqualTo(2);
        Optional<Stock> foundStock = stockRepository
                .findById(new StockId(newMarketType, testStockCode));
        assertThat(foundStock).isPresent();
        assertThat(foundStock.get().getId().getMarketType()).isEqualTo(newMarketType);
    }
}