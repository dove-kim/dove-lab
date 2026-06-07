package com.dove.screening.application.service;

import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterNode;
import com.dove.screening.infrastructure.repository.StockFeatureFilterRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 검색식을 wide 피처 테이블 조건으로 밀어넣어 매칭 종목을 조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockFeatureFilterQueryService {

    private final StockFeatureFilterRepository repository;

    /**
     * 검색식을 컬럼 조건으로 변환해 만족하는 wide 피처 행을 조회한다. 식을 컬럼으로 표현할 수 없으면 빈 값을
     * 반환한다 (호출 측 인메모리 폴백 신호).
     */
    public Optional<List<FeatureMatch>> findMatchingByExpression(Collection<StockExchange> exchanges,
                                                                 PriceType priceType, LocalDate date,
                                                                 FilterNode expression) {
        return repository.findMatchingByExpression(exchanges, priceType, date, expression);
    }

    /**
     * 거래소 집합·가격유형·날짜의 전 종목 수를 반환한다 (검색 대상 모수).
     */
    public long countByExchangesAndDate(Collection<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        return repository.countByExchangesAndDate(exchanges, priceType, date);
    }
}
