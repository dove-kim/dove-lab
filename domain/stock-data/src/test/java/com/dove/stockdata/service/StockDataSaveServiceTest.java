package com.dove.stockdata.service;

import com.dove.stockdata.entity.*;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.repository.StockDataRepository;
import com.dove.stockdata.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

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
        Long testOpeningPrice = 70000L;
        Long testClosingPrice = 71000L;
        Long testLowPrice = 69500L;
        Long testHighPrice = 71500L;

        // When
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );

        // Then
        // Stock 엔티티 검증
        Optional<Stock> foundStock = stockRepository
                .findById(new StockId(testMarketType, testStockCode));
        assertThat(foundStock).isPresent();
        assertThat(foundStock.get().getId().getCode()).isEqualTo(testStockCode);
        assertThat(foundStock.get().getId().getMarketType()).isEqualTo(testMarketType);
        assertThat(foundStock.get().getName()).isEqualTo(testStockName);

        // StockData 엔티티 검증
        StockDataId dailyDataId = new StockDataId(testMarketType, testStockCode, testBaseDate);
        Optional<StockData> foundDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(foundDailyData).isPresent();
        assertThat(foundDailyData.get().getVolume()).isEqualTo(testVolume);
        assertThat(foundDailyData.get().getOpenPrice()).isEqualTo(testOpeningPrice);
        assertThat(foundDailyData.get().getClosePrice()).isEqualTo(testClosingPrice);
        assertThat(foundDailyData.get().getLowPrice()).isEqualTo(testLowPrice);
        assertThat(foundDailyData.get().getHighPrice()).isEqualTo(testHighPrice);
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
        Long testOpeningPrice = 70000L;
        Long testClosingPrice = 71000L;
        Long testLowPrice = 69500L;
        Long testHighPrice = 71500L;

        // 첫 번째 저장
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, testStockName,
                testVolume, testOpeningPrice, testClosingPrice, testLowPrice, testHighPrice
        );
        long initialDailyDataCount = stockDataRepository.count();
        long initialStockCount = stockRepository.count();

        // 업데이트할 값
        Long updatedVolume = 2000000L;
        Long updatedClosingPrice = 72500L;
        String updatedStockName = "십만전자";

        // When
        stockDataSaveService.update(
                testBaseDate, testMarketType, testStockCode, updatedStockName,
                updatedVolume, testOpeningPrice, updatedClosingPrice, testLowPrice, testHighPrice
        );

        // Then
        assertThat(stockRepository.count()).isEqualTo(initialStockCount);
        Optional<Stock> updatedStock = stockRepository.findById(new StockId(testMarketType, testStockCode));
        assertThat(updatedStock).isPresent();
        assertThat(updatedStock.get().getName()).isEqualTo(updatedStockName);

        assertThat(stockDataRepository.count()).isEqualTo(initialDailyDataCount);
        StockDataId dailyDataId = new StockDataId(testMarketType, testStockCode, testBaseDate);
        Optional<StockData> updatedDailyData = stockDataRepository.findById(dailyDataId);
        assertThat(updatedDailyData).isPresent();
        assertThat(updatedDailyData.get().getVolume()).isEqualTo(updatedVolume);
        assertThat(updatedDailyData.get().getClosePrice()).isEqualTo(updatedClosingPrice);
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
        Long testOpeningPrice = 70000L;
        Long testClosingPrice = 71000L;
        Long testLowPrice = 69500L;
        Long testHighPrice = 71500L;

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
        assertThat(stockRepository.count()).isEqualTo(2);
        assertThat(stockDataRepository.count()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (baseDate null)")
    void testUpdate_missingBaseDate_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                null, MarketType.KOSPI, "005930", "삼성전자",
                1000000L, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (marketType null)")
    void testUpdate_missingMarketType_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                LocalDate.of(2023, 10, 26), null, "005930", "삼성전자",
                1000000L, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (stockCode null)")
    void testUpdate_missingStockCode_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                LocalDate.of(2023, 10, 26), MarketType.KOSPI, null, "삼성전자",
                1000000L, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (stockCode 빈 문자열)")
    void testUpdate_emptyStockCode_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                LocalDate.of(2023, 10, 26), MarketType.KOSPI, "", "삼성전자",
                1000000L, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (stockName null)")
    void testUpdate_missingStockName_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                LocalDate.of(2023, 10, 26), MarketType.KOSPI, "005930", null,
                1000000L, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("필수 파라미터 누락 시 데이터 저장을 건너뛴다 (volume null)")
    void testUpdate_missingVolume_skipsSave() {
        assertDoesNotThrow(() -> stockDataSaveService.update(
                LocalDate.of(2023, 10, 26), MarketType.KOSPI, "005930", "삼성전자",
                null, 70000L, 71000L, 69500L, 71500L
        ));

        assertThat(stockRepository.count()).isZero();
        assertThat(stockDataRepository.count()).isZero();
    }
}
