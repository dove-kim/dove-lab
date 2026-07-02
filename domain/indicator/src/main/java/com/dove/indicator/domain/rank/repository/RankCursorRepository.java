package com.dove.indicator.domain.rank.repository;

import com.dove.indicator.domain.rank.entity.RankCursor;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * (universe·가격유형) 단위 횡단면 순위 계산 커서 저장소.
 */
@Repository
public interface RankCursorRepository extends JpaRepository<RankCursor, Long> {

    Optional<RankCursor> findByUniverseAndPriceType(MarketUniverse universe, PriceType priceType);
}
