package com.dove.stockdata.service;

import com.dove.stockdata.entity.Stock;
import com.dove.stockdata.entity.StockData;
import com.dove.stockdata.entity.StockDate;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.repository.StockDataRepository;
import com.dove.stockdata.repository.StockDateRepository;
import com.dove.stockdata.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataSaveService {
    private final StockRepository stockRepository;
    private final StockDateRepository stockDateRepository;
    private final StockDataRepository stockDataRepository;

    /**
     * 주식 및 일별 주식 데이터를 저장하거나 업데이트합니다.
     * 모든 필수 파라미터의 유효성을 검사하며, 하나라도 누락되거나 유효하지 않으면
     * 데이터 저장을 건너뛰고 오류 로그를 남깁니다.
     *
     * @param baseDate     기준 날짜 ({@code null} 불가)
     * @param marketType   시장 타입 ({@code null} 불가)
     * @param stockCode    주식 코드 ({@code null} 또는 빈 문자열 불가)
     * @param stockName    주식 이름 ({@code null} 또는 빈 문자열 불가)
     * @param volume       거래량 ({@code null} 불가)
     * @param openingPrice 시가 ({@code null} 불가)
     * @param closingPrice 종가 ({@code null} 불가)
     * @param lowPrice     저가 ({@code null} 불가)
     * @param highPrice    고가 ({@code null} 불가)
     */
    @Transactional
    public void update(
            LocalDate baseDate, MarketType marketType,
            String stockCode, String stockName,
            Long volume,
            BigDecimal openingPrice, BigDecimal closingPrice,
            BigDecimal lowPrice, BigDecimal highPrice
    ) {
        // 입력 파라미터 유효성 검사
        if (!validateInputParameters(baseDate, marketType, stockCode, stockName,
                volume, openingPrice, closingPrice, lowPrice, highPrice)) {
            return;
        }

        // 주식 정보 저장 또는 업데이트
        Stock stock = stockRepository.findById_MarketTypeAndId_Code(marketType, stockCode)
                .map(existingStock -> existingStock.updateName(stockName))
                .orElseGet(() -> new Stock(marketType, stockCode, stockName));
        stockRepository.save(stock);

        // 주식 날짜 저장 또는 업데이트
        StockDate stockDate = stockDateRepository.findById(baseDate)
                .orElseGet(() -> new StockDate(baseDate));
        stockDateRepository.save(stockDate);

        // 주식 일별 데이터 저장
        stockDataRepository.save(new StockData(
                marketType, stockCode, baseDate, volume,
                openingPrice, closingPrice, lowPrice, highPrice
        ));
    }

    /**
     * 입력 파라미터의 유효성을 검사합니다.
     * 필수 파라미터 중 하나라도 누락되면 로그를 남기고 false를 반환합니다.
     *
     * @return 모든 파라미터가 유효하면 true, 그렇지 않으면 false
     */
    private boolean validateInputParameters(
            LocalDate baseDate, MarketType marketType, // Changed MarketType to StockMarketType
            String stockCode, String stockName,
            Long volume,
            BigDecimal openingPrice, BigDecimal closingPrice,
            BigDecimal lowPrice, BigDecimal highPrice
    ) {
        List<String> missingParams = new ArrayList<>();

        if (baseDate == null) missingParams.add("baseDate");
        if (marketType == null) missingParams.add("marketType");
        if (stockCode == null || stockCode.isEmpty()) missingParams.add("stockCode");
        if (stockName == null || stockName.isEmpty()) missingParams.add("stockName");
        if (volume == null) missingParams.add("volume");
        if (openingPrice == null) missingParams.add("openingPrice");
        if (closingPrice == null) missingParams.add("closingPrice");
        if (lowPrice == null) missingParams.add("lowPrice");
        if (highPrice == null) missingParams.add("highPrice");

        if (!missingParams.isEmpty()) {
            String errorMessage = "필수 파라미터 누락으로 데이터 저장을 건너뜁니다. 누락된 파라미터: " + String.join(", ", missingParams);
            log.error(errorMessage);
            return false;
        }
        return true;
    }
}
