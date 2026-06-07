package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.CursorRewoundException;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 지표 계산의 한 청크를 저장한다(wide 피처 행 저장 + 그룹 커서 전진을 한 트랜잭션으로).
 */
@Service
@RequiredArgsConstructor
public class IndicatorChunkCommitService {

    private final StockFeatureDailyService featureService;
    private final IndicatorCursorService cursorService;

    /**
     * 청크의 wide 피처 행을 저장하고, 청크 마지막 거래일로 그룹 커서를 compare-and-set 전진한다.
     *
     * @param expected     계산 시점에 읽은 그룹 커서값 (CAS 비교용)
     * @param cursorExists 계산 시점에 커서가 존재했는지
     * @throws CursorRewoundException 커서가 expected와 달라 전진이 거부된 경우 (저장도 함께 롤백)
     */
    @Transactional
    public void commit(String ticker, StockExchange exchange, PriceType priceType,
                       List<StockFeatureDaily> features,
                       LocalDate expected, boolean cursorExists, LocalDate chunkLastDate) {
        featureService.saveAll(features);
        cursorService.advanceForwardCas(ticker, exchange, priceType, expected, cursorExists, chunkLastDate);
    }
}
