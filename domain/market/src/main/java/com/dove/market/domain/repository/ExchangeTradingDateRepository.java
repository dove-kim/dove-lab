package com.dove.market.domain.repository;

import com.dove.market.domain.entity.ExchangeTradingDate;
import com.dove.market.domain.entity.ExchangeTradingDateId;
import com.dove.market.domain.enums.Exchange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 거래소별 거래일 영속성 저장소.
 */
@Repository
public interface ExchangeTradingDateRepository extends JpaRepository<ExchangeTradingDate, ExchangeTradingDateId> {

    /**
     * 거래소·기간 내 거래일 목록을 반환한다.
     */
    List<ExchangeTradingDate> findById_ExchangeAndId_TradeDateBetween(
            Exchange exchange, LocalDate from, LocalDate to);

    /**
     * 거래소·기간 내 주가 미수집 날짜 목록을 반환한다.
     */
    List<ExchangeTradingDate> findByPricesSyncedFalseAndId_ExchangeAndId_TradeDateBetween(
            Exchange exchange, LocalDate from, LocalDate to);

    /**
     * 거래소의 가장 최근 거래일 기록을 반환한다.
     */
    Optional<ExchangeTradingDate> findTopByIdExchangeOrderByIdTradeDateDesc(Exchange exchange);

    /**
     * 거래소에서 onOrBefore 이하의 거래일을 최신순으로 반환한다(페이지로 개수 제한).
     */
    List<ExchangeTradingDate> findByIdExchangeAndIdTradeDateLessThanEqualOrderByIdTradeDateDesc(
            Exchange exchange, LocalDate onOrBefore, Pageable pageable);
}
