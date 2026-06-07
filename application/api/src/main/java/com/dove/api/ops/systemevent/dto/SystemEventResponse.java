package com.dove.api.ops.systemevent.dto;

import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.domain.entity.SystemEvent;
import com.dove.systemevent.domain.enums.SystemEventType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 시스템 운영 이벤트 응답.
 *
 * @param id         이벤트 ID
 * @param eventType  이벤트 유형
 * @param marketType 시장 구분
 * @param occurredAt 발생 일시
 * @param detail     상세 정보
 */
public record SystemEventResponse(
        Long id,
        SystemEventType eventType,
        MarketType marketType,
        LocalDateTime occurredAt,
        Map<String, String> detail
) {
    public static SystemEventResponse from(SystemEvent entity) {
        return new SystemEventResponse(
                entity.getId(),
                entity.getEventType(),
                entity.getMarketType(),
                entity.getOccurredAt(),
                entity.getDetail()
        );
    }
}
