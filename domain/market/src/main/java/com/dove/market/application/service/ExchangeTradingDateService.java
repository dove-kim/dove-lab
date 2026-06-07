package com.dove.market.application.service;

import com.dove.market.domain.entity.ExchangeTradingDate;
import com.dove.market.domain.entity.ExchangeTradingDateId;
import com.dove.market.domain.enums.Exchange;
import com.dove.market.domain.repository.ExchangeTradingDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 거래소별 거래일을 등록·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeTradingDateService {

    private final ExchangeTradingDateRepository repository;

    /**
     * 거래일 등록 또는 갱신(closed → open 방향만 허용).
     */
    @Transactional
    public void upsert(Exchange exchange, LocalDate date, boolean isOpen) {
        ExchangeTradingDateId id = new ExchangeTradingDateId(exchange, date);
        repository.findById(id).ifPresentOrElse(
                existing -> {
                    if (!existing.isOpen() && isOpen) {
                        existing.markOpen();
                    }
                },
                () -> repository.save(new ExchangeTradingDate(id, isOpen))
        );
    }

    /**
     * 해당 날짜 전 종목 주가 수집 완료 마킹.
     */
    @Transactional
    public void markPricesSynced(Exchange exchange, LocalDate date) {
        repository.findById(new ExchangeTradingDateId(exchange, date))
                .ifPresent(ExchangeTradingDate::markPricesSynced);
    }

    /**
     * 해당 날짜가 개장일이면 true를 반환한다.
     */
    public boolean existsOpenDay(Exchange exchange, LocalDate date) {
        return repository.findById(new ExchangeTradingDateId(exchange, date))
                .map(ExchangeTradingDate::isOpen)
                .orElse(false);
    }

    /**
     * 거래소·기간 내 개장일 날짜 목록을 반환한다.
     */
    public List<LocalDate> findOpenDatesInRange(Exchange exchange, LocalDate from, LocalDate to) {
        return repository.findByOpenTrueAndId_ExchangeAndId_TradeDateBetween(exchange, from, to)
                .stream()
                .map(etd -> etd.getId().getTradeDate())
                .toList();
    }

    /**
     * 거래소·기간 내 개장 + 주가 미수집 날짜 목록을 반환한다.
     */
    public List<LocalDate> findUnsyncedPriceDates(Exchange exchange, LocalDate from, LocalDate to) {
        return repository.findByOpenTrueAndPricesSyncedFalseAndId_ExchangeAndId_TradeDateBetween(exchange, from, to)
                .stream()
                .map(etd -> etd.getId().getTradeDate())
                .toList();
    }

    /**
     * 거래소의 마지막 처리 날짜를 반환한다. 레코드가 없으면 empty.
     */
    public Optional<LocalDate> findLastProcessedDate(Exchange exchange) {
        return repository.findTopByIdExchangeOrderByIdTradeDateDesc(exchange)
                .map(etd -> etd.getId().getTradeDate());
    }
}
