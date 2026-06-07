package com.dove.systemevent.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.domain.enums.SystemEventType;
import com.dove.systemevent.infrastructure.converter.EventDetailConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 운영 중 발생하는 시스템 이벤트(KRX API 장애 등) 기록.
 */
@Getter
@Entity
@Table(
        name = "SYSTEM_EVENT",
        indexes = {
                @Index(name = "IDX_SYSTEM_EVENT_OCCURRED_AT", columnList = "OCCURRED_AT"),
                @Index(name = "IDX_SYSTEM_EVENT_TYPE", columnList = "EVENT_TYPE")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("고유 ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    @Comment("이벤트 종류 (KRX_API_FAILURE 등)")
    private SystemEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET_TYPE", length = 10)
    @Comment("관련 시장 타입 (없으면 NULL)")
    private MarketType marketType;

    @Column(name = "OCCURRED_AT", nullable = false)
    @Comment("이벤트 발생 일시")
    private LocalDateTime occurredAt;

    @Convert(converter = EventDetailConverter.class)
    @Column(name = "DETAIL", columnDefinition = "JSON", nullable = false)
    @Comment("이벤트 상세 정보 (JSON)")
    private Map<String, String> detail;

    /**
     * 현재 시각으로 시스템 이벤트를 생성한다.
     */
    public static SystemEvent of(SystemEventType eventType, MarketType marketType,
                                 Map<String, String> detail) {
        SystemEvent event = new SystemEvent();
        event.eventType = eventType;
        event.marketType = marketType;
        event.occurredAt = LocalDateTime.now();
        event.detail = detail;
        return event;
    }
}
