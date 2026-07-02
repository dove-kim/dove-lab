package com.dove.indicator.domain.breadth.repository;

import com.dove.indicator.domain.breadth.entity.BreadthCursor;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * (universe·가격유형) 단위 당일 상승비율 계산 커서 저장소.
 */
@Repository
public interface BreadthCursorRepository extends JpaRepository<BreadthCursor, Long> {

    Optional<BreadthCursor> findByUniverseAndPriceType(MarketUniverse universe, PriceType priceType);
}
