package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findById_MarketTypeAndId_Code(MarketType marketType, String code);

    default Optional<Stock> findByMarketTypeAndCode(MarketType marketType, String code) {
        return findById_MarketTypeAndId_Code(marketType, code);
    }

    List<Stock> findAllById_MarketType(MarketType marketType);

    @Query("SELECT s FROM Stock s WHERE s.id.code IN :codes")
    List<Stock> findAllByCodeIn(@Param("codes") Collection<String> codes);
}
