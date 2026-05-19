package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketTradingDate;
import com.dove.market.domain.entity.MarketTradingDateId;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketTradingDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketTradingDateService {

    private final MarketTradingDateRepository repository;

    // ── Command ──────────────────────────────────────────────────────────────

    /** closed → open 방향 업데이트만 허용. 한번 개장으로 확정된 날짜는 되돌리지 않는다. */
    @Transactional
    public void upsert(MarketType market, LocalDate date, boolean isOpen) {
        MarketTradingDateId id = new MarketTradingDateId(market, date);
        repository.findById(id).ifPresentOrElse(
                existing -> {
                    if (!existing.isOpen() && isOpen) {
                        existing.markOpen();
                    }
                },
                () -> repository.save(new MarketTradingDate(id, isOpen))
        );
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * 시장의 마지막 처리 날짜를 반환한다.
     * MTD 테이블의 최신 레코드에서 파생하므로 별도 커서 테이블이 불필요하다.
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> findLastProcessedDate(MarketType market) {
        return repository.findTopByIdMarketTypeOrderByIdTradeDateDesc(market)
                .map(mtd -> mtd.getId().getTradeDate());
    }

    /** 해당 날짜가 개장일(IS_OPEN=true)이면 true. */
    @Transactional(readOnly = true)
    public boolean existsOpenDay(MarketType market, LocalDate date) {
        return repository.findById(new MarketTradingDateId(market, date))
                .map(MarketTradingDate::isOpen)
                .orElse(false);
    }

    /** MTD 레코드 존재 여부 (개장/휴장 무관). 미존재 = 해당일 처리 미완료. */
    @Transactional(readOnly = true)
    public boolean existsTradingDateRecord(MarketType market, LocalDate date) {
        return repository.existsById(new MarketTradingDateId(market, date));
    }

    /** 시장·기간 내 개장일(IS_OPEN=true) 날짜 목록 반환. */
    @Transactional(readOnly = true)
    public List<LocalDate> findOpenDatesInRange(MarketType market, LocalDate from, LocalDate to) {
        return repository.findByOpenTrueAndId_MarketTypeAndId_TradeDateBetween(market, from, to)
                .stream()
                .map(mtd -> mtd.getId().getTradeDate())
                .toList();
    }

    /** 시장·기간 내 모든 MTD 상태 반환. key=날짜, value=개장 여부. 레코드 없는 날짜는 포함되지 않는다. */
    @Transactional(readOnly = true)
    public Map<LocalDate, Boolean> findStatusesInRange(MarketType market, LocalDate from, LocalDate to) {
        return repository.findById_MarketTypeAndId_TradeDateBetween(market, from, to)
                .stream()
                .collect(Collectors.toMap(
                        mtd -> mtd.getId().getTradeDate(),
                        MarketTradingDate::isOpen
                ));
    }
}
