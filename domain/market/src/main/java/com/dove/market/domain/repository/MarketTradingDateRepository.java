package com.dove.market.domain.repository;

import com.dove.market.domain.entity.MarketTradingDate;
import com.dove.market.domain.entity.MarketTradingDateId;
import com.dove.market.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketTradingDateRepository extends JpaRepository<MarketTradingDate, MarketTradingDateId> {

    /** 시장·기간 내 개장일(IS_OPEN=true) 목록 반환. */
    List<MarketTradingDate> findByOpenTrueAndId_MarketTypeAndId_TradeDateBetween(
            MarketType marketType, LocalDate from, LocalDate to);

    /** 시장·기간 내 모든 MTD 레코드 반환 (개장/휴장 무관). */
    List<MarketTradingDate> findById_MarketTypeAndId_TradeDateBetween(
            MarketType marketType, LocalDate from, LocalDate to);

    /** 시장의 가장 최근 MTD 레코드 반환. 커서 대용으로 마지막 처리 날짜 조회에 사용. */
    java.util.Optional<MarketTradingDate> findTopByIdMarketTypeOrderByIdTradeDateDesc(MarketType marketType);
}
