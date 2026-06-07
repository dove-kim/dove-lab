package com.dove.api.search.stock.dto;

import com.dove.stock.domain.entity.StockEvent;

import java.time.LocalDate;

/**
 * 종목 권리 이벤트 응답 (권리 이벤트 탭용).
 */
public record StockEventResponse(
        String eventType,
        String eventTypeLabel,
        LocalDate eventDate,
        String summary
) {
    public static StockEventResponse from(StockEvent e) {
        return new StockEventResponse(
                e.getEventType().name(),
                e.getEventType().label(),
                e.getEventDate(),
                e.getSummary()
        );
    }
}
