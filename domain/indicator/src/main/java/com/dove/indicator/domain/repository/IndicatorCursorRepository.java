package com.dove.indicator.domain.repository;

import com.dove.indicator.domain.entity.IndicatorCursor;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 그룹(종목·거래소·가격유형) 단위 지표 계산 커서 저장소.
 */
@Repository
public interface IndicatorCursorRepository extends JpaRepository<IndicatorCursor, Long> {

    Optional<IndicatorCursor> findByTickerAndExchangeAndPriceType(
            String ticker, StockExchange exchange, PriceType priceType);

    /**
     * 종목·거래소·가격유형의 커서 삭제 → 다음 배치가 처음부터 재계산.
     */
    void deleteByTickerAndExchangeAndPriceType(
            String ticker, StockExchange exchange, PriceType priceType);
}
