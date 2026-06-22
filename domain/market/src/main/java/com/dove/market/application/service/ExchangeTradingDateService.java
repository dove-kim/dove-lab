package com.dove.market.application.service;

import com.dove.market.domain.entity.ExchangeTradingDate;
import com.dove.market.domain.entity.ExchangeTradingDateId;
import com.dove.market.domain.enums.Exchange;
import com.dove.market.domain.repository.ExchangeTradingDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
     * 거래일을 등록한다(멱등). 이미 있으면 그대로 둔다 — pricesSynced를 되돌리지 않는다.
     */
    @Transactional
    public void register(Exchange exchange, LocalDate date) {
        ExchangeTradingDateId id = new ExchangeTradingDateId(exchange, date);
        if (!repository.existsById(id)) {
            repository.save(new ExchangeTradingDate(id));
        }
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
     * 해당 날짜가 거래일이면 true를 반환한다.
     */
    public boolean existsTradingDay(Exchange exchange, LocalDate date) {
        return repository.existsById(new ExchangeTradingDateId(exchange, date));
    }

    /**
     * 거래소·기간 내 거래일 목록을 반환한다.
     */
    public List<LocalDate> findTradingDatesInRange(Exchange exchange, LocalDate from, LocalDate to) {
        return repository.findById_ExchangeAndId_TradeDateBetween(exchange, from, to)
                .stream()
                .map(etd -> etd.getId().getTradeDate())
                .toList();
    }

    /**
     * 거래소에서 onOrBefore 이하의 최근 거래일 limit개를 내림차순으로 반환한다.
     */
    public List<LocalDate> findRecentTradingDates(Exchange exchange, LocalDate onOrBefore, int limit) {
        return repository.findByIdExchangeAndIdTradeDateLessThanEqualOrderByIdTradeDateDesc(
                        exchange, onOrBefore, PageRequest.of(0, limit))
                .stream()
                .map(etd -> etd.getId().getTradeDate())
                .toList();
    }

    /**
     * 거래소·기간 내 주가 미수집 날짜 목록을 반환한다.
     */
    public List<LocalDate> findUnsyncedPriceDates(Exchange exchange, LocalDate from, LocalDate to) {
        return repository.findByPricesSyncedFalseAndId_ExchangeAndId_TradeDateBetween(exchange, from, to)
                .stream()
                .map(etd -> etd.getId().getTradeDate())
                .toList();
    }

    /**
     * 거래소의 마지막 거래일을 반환한다. 레코드가 없으면 empty.
     */
    public Optional<LocalDate> findLastProcessedDate(Exchange exchange) {
        return repository.findTopByIdExchangeOrderByIdTradeDateDesc(exchange)
                .map(etd -> etd.getId().getTradeDate());
    }
}
