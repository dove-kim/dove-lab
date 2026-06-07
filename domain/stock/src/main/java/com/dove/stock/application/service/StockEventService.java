package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockEvent;
import com.dove.stock.domain.enums.StockEventType;
import com.dove.stock.domain.repository.StockEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 종목 권리 이벤트 저장·조회.
 */
@Service
@RequiredArgsConstructor
public class StockEventService {

    private final StockEventRepository repository;

    /**
     * 해당 이벤트가 없으면 저장한다. 중복(동일 ticker·type·date)은 무시한다.
     *
     * @return 실제로 저장했으면 true, 중복·무효 입력이면 false
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveIfAbsent(String ticker, StockEventType eventType, LocalDate eventDate,
                                String summary, String detail) {
        if (ticker == null || ticker.isBlank() || eventDate == null) return false;
        if (repository.existsByTickerAndEventTypeAndEventDate(ticker, eventType, eventDate)) return false;
        try {
            repository.save(new StockEvent(ticker, eventType, eventDate, summary, detail, "KIS_KSD"));
            return true;
        } catch (DataIntegrityViolationException ignore) {
            // 동시 수집 시 UNIQUE 충돌 — 무시 (멱등)
            return false;
        }
    }

    /**
     * 종목의 권리 이벤트를 최신순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<StockEvent> findByTicker(String ticker) {
        return repository.findByTickerOrderByEventDateDesc(ticker);
    }
}
