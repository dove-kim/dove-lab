package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockShareCount;
import com.dove.stock.domain.entity.StockShareCountId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 상장주식수 변경이력 저장소.
 */
public interface StockShareCountRepository extends JpaRepository<StockShareCount, StockShareCountId> {

    /**
     * 기준일 이하 중 가장 최근(as-of) 상장주식수를 반환한다.
     */
    Optional<StockShareCount> findFirstByTickerAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String ticker, LocalDate date);

    /**
     * 기준일 이하 전 종목 변경이력을 발효일 오름차순으로 반환한다 — 종목별 as-of를 메모리에서 축약(배치용).
     */
    List<StockShareCount> findByEffectiveDateLessThanEqualOrderByEffectiveDateAsc(LocalDate date);
}
