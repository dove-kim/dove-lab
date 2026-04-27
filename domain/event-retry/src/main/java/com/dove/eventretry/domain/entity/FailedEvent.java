package com.dove.eventretry.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/** 자동 재시도 한계 초과 또는 영구 실패로 수동 개입이 필요한 이벤트 큐. */
@Getter
@Entity
@Table(name = "FAILED_EVENT",
        indexes = @Index(name = "IDX_FAILED_EVENT_ESCALATED_AT", columnList = "ESCALATED_AT"),
        uniqueConstraints = @UniqueConstraint(name = "UK_FAILED_EVENT_EVENT",
                columnNames = {"EVENT_TYPE", "EVENT_KEY"}))
@Comment("수동 개입이 필요한 이벤트 큐 (DLQ)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    @Comment("Kafka 토픽 이름")
    private String eventType;

    @Column(name = "EVENT_KEY", nullable = false, length = 255)
    @Comment("원본 Kafka 메시지 키")
    private String eventKey;

    @Column(name = "EVENT_PAYLOAD", nullable = false, columnDefinition = "TEXT")
    @Comment("원본 Kafka 메시지 값")
    private String eventPayload;

    @Column(name = "REASON_CATEGORY", nullable = false, length = 30)
    @Comment("승격 사유 — 생산자가 정의한 문자열 (예: AUTH_FAILED, RETRY_LIMIT_EXCEEDED)")
    private String reasonCategory;

    @Column(name = "LAST_ERROR_DETAIL", length = 1000)
    @Comment("실패 상세")
    private String lastErrorDetail;

    @Column(name = "ESCALATED_AT", nullable = false)
    @Comment("수동 검토 큐 등록 시각")
    private Instant escalatedAt;

    private FailedEvent(String eventType, String eventKey, String eventPayload,
                        String reasonCategory, String lastErrorDetail, Instant escalatedAt) {
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.eventPayload = eventPayload;
        this.reasonCategory = reasonCategory;
        this.lastErrorDetail = lastErrorDetail;
        this.escalatedAt = escalatedAt;
    }

    public static FailedEvent escalate(String eventType, String eventKey, String eventPayload,
                                       String reason, String detail, Instant escalatedAt) {
        return new FailedEvent(eventType, eventKey, eventPayload, reason, detail, escalatedAt);
    }
}
