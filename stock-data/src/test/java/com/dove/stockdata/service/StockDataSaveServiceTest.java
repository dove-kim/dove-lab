package com.dove.stockdata.service;

import com.dove.stockdata.entity.*;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.repository.StockDataRepository;
import com.dove.stockdata.repository.StockDateRepository;
import com.dove.stockdata.repository.StockRepository;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


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
        MarketType testMarketType = MarketType.KOSPI;
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
        // Stock 엔티티 검증 (복합 키 사용)
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

        // StockData 엔티티 검증 (복합 키 사용)
        StockDataId dailyDataId = new StockDataId(testMarketType, testStockCode, testBaseDate);
        Optional<StockData> foundDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(foundDailyData).isPresent();
        assertThat(foundDailyData.get().getVolume()).isEqualTo(testVolume);
        assertThat(foundDailyData.get().getOpenPrice()).isEqualByComparingTo(testOpeningPrice);
        assertThat(foundDailyData.get().getClosePrice()).isEqualByComparingTo(testClosingPrice);
        assertThat(foundDailyData.get().getLowPrice()).isEqualByComparingTo(testLowPrice);
        assertThat(foundDailyData.get().getHighPrice()).isEqualByComparingTo(testHighPrice);
    }

    @Test
    @Transactional
    @DisplayName("주식 데이터, 날짜 모두 업데이트 (기존 레코드 수정)")
    void testUpdateExistingStockDateAndExistingDailyData() {
        // Given
        LocalDate testBaseDate = LocalDate.of(2023, 10, 26);
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        MarketType testMarketType = MarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        // 첫 번째 저장 (초기 데이터)
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );
        long initialStockDateCount = stockDateRepository.count();
        long initialDailyDataCount = stockDataRepository.count();
        long initialStockCount = stockRepository.count();

        // 업데이트할 값
        Long updatedVolume = 2000000L;
        BigDecimal updatedClosingPrice = new BigDecimal("72500.00");
        String updatedStockName = "십만전자";

        // When
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, updatedStockName,
                updatedVolume, testOpeningPrice, updatedClosingPrice, testLowPrice, testHighPrice
        );

        // Then:
        // Stock 엔티티가 업데이트되었는지 확인 (새로운 Stock 레코드가 생성되지 않음)
        assertThat(stockRepository.count()).isEqualTo(initialStockCount);
        Optional<Stock> updatedStock = stockRepository.findById(new StockId(testMarketType, testStockCode));
        assertThat(updatedStock).isPresent();
        assertThat(updatedStock.get().getName()).isEqualTo(updatedStockName);

        // StockDate는 새로 생성되지 않고 기존 것이 사용되었음을 확인
        assertThat(stockDateRepository.count()).isEqualTo(initialStockDateCount);

        // StockData가 업데이트되었는지 확인 (새로운 레코드 생성되지 않음)
        assertThat(stockDataRepository.count()).isEqualTo(initialDailyDataCount);
        StockDataId dailyDataId = new StockDataId(testMarketType, testStockCode, testBaseDate);
        Optional<StockData> updatedDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(updatedDailyData).isPresent();
        assertThat(updatedDailyData.get().getVolume()).isEqualTo(updatedVolume);
        assertThat(updatedDailyData.get().getClosePrice()).isEqualByComparingTo(updatedClosingPrice);
    }

    @Test
    @Transactional
    @DisplayName("다른 MarketType으로 동일 StockCode 업데이트 시 새로운 Stock 엔티티 생성")
    void testSaveStockWithDifferentMarketType() {
        // Given
        LocalDate testBaseDate = LocalDate.of(2023, 10, 26);
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        MarketType initialMarketType = MarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        // KOSPI 삼성전자 저장
        stockDataSaveService.update(
                testBaseDate, initialMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );
        assertThat(stockRepository.count()).isEqualTo(1);

        // When
        MarketType newMarketType = MarketType.KOSDAQ;
        stockDataSaveService.update(
                testBaseDate, newMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );

        // Then
        // Stock 엔티티가 새로 생성되어 총 2개가 되었는지 확인
        assertThat(stockRepository.count()).isEqualTo(2);

        // 기존 KOSPI 삼성전자 확인
        Optional<Stock> kospiStock = stockRepository.findById(new StockId(initialMarketType, testStockCode));
        assertThat(kospiStock).isPresent();
        assertThat(kospiStock.get().getId().getMarketType()).isEqualTo(initialMarketType);

        // 새로운 KONEX 삼성전자 확인
        Optional<Stock> konexStock = stockRepository.findById(new StockId(newMarketType, testStockCode));
        assertThat(konexStock).isPresent();
        assertThat(konexStock.get().getId().getMarketType()).isEqualTo(newMarketType);

        // StockData는 두 개의 다른 (stockCode, baseDate) 쌍으로 저장되었을 것임
        assertThat(stockDataRepository.count()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (baseDate null)")
    void testUpdate_missingBaseDate_skipsSave() {
        // Given
        LocalDate nullBaseDate = null;
        String testStockCode = "005930";
        String testStockName = "삼성전자";
        MarketType testMarketType = MarketType.KOSPI;
        Long testVolume = 1000000L;
        BigDecimal testOpeningPrice = new BigDecimal("70000.00");
        BigDecimal testClosingPrice = new BigDecimal("71000.00");
        BigDecimal testLowPrice = new BigDecimal("69500.00");
        BigDecimal testHighPrice = new BigDecimal("71500.00");

        // When
        // 예외가 발생하지 않고 메서드가 정상적으로 종료되는지 확인
        assertDoesNotThrow(() -> stockDataSaveService.update(
                nullBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        ));

        // Then
        // 어떤 데이터도 저장되지 않았음을 검증
        assertThat(stockRepository.count()).isZero();
        assertThat(stockDateRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }
}
