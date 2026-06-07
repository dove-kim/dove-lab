package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockEvent;
import com.dove.stock.domain.enums.StockEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * StockEvent 엔티티 저장소.
 */
@Repository
public interface StockEventRepository extends JpaRepository<StockEvent, Long> {

    boolean existsByTickerAndEventTypeAndEventDate(
            String ticker, StockEventType eventType, LocalDate eventDate);

    List<StockEvent> findByTickerOrderByEventDateDesc(String ticker);
}
