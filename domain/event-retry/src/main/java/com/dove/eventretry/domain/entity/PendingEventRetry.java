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

/** Kafka 이벤트 재시도 대기 큐. (eventType, eventKey) 단위로 멱등 유지. */
@Getter
@Entity
@Table(name = "PENDING_EVENT_RETRY",
        indexes = @Index(name = "IDX_PENDING_EVENT_RETRY_NEXT_RETRY_AT", columnList = "NEXT_RETRY_AT"),
        uniqueConstraints = @UniqueConstraint(name = "UK_PENDING_EVENT_RETRY_EVENT",
                columnNames = {"EVENT_TYPE", "EVENT_KEY"}))
@Comment("Kafka 이벤트 재시도 대기 큐")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingEventRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    @Comment("Kafka 토픽 이름 (재발행 대상)")
    private String eventType;

    @Column(name = "EVENT_KEY", nullable = false, length = 255)
    @Comment("원본 Kafka 메시지 키 (멱등성 식별자)")
    private String eventKey;

    @Column(name = "EVENT_PAYLOAD", nullable = false, columnDefinition = "TEXT")
    @Comment("원본 Kafka 메시지 값 (재발행 시 그대로 사용)")
    private String eventPayload;

    @Column(name = "REASON_CATEGORY", nullable = false, length = 30)
    @Comment("재시도 사유 카테고리 — 생산자가 정의한 문자열 (예: TRANSIENT, AUTH_FAILED)")
    private String reasonCategory;

    @Column(name = "NEXT_RETRY_AT", nullable = false)
    @Comment("다음 재시도 가능 시각")
    private Instant nextRetryAt;

    @Column(name = "RETRY_COUNT", nullable = false)
    @Comment("누적 재시도 횟수")
    private int retryCount;

    @Column(name = "FIRST_REQUESTED_AT", nullable = false)
    @Comment("최초 enqueue 시각 (전체 체류 시간 판정용)")
    private Instant firstRequestedAt;

    @Column(name = "LAST_ERROR_DETAIL", length = 500)
    @Comment("직전 실패의 상세 원인")
    private String lastErrorDetail;

    private PendingEventRetry(String eventType, String eventKey, String eventPayload,
                              String reasonCategory, Instant nextRetryAt, int retryCount,
                              Instant firstRequestedAt, String lastErrorDetail) {
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.eventPayload = eventPayload;
        this.reasonCategory = reasonCategory;
        this.nextRetryAt = nextRetryAt;
        this.retryCount = retryCount;
        this.firstRequestedAt = firstRequestedAt;
        this.lastErrorDetail = lastErrorDetail;
    }

    public static PendingEventRetry create(String eventType, String eventKey, String eventPayload,
                                           String reason, Instant nextRetryAt,
                                           Instant firstRequestedAt, String detail) {
        return new PendingEventRetry(eventType, eventKey, eventPayload, reason,
                nextRetryAt, 0, firstRequestedAt, detail);
    }

    public void reschedule(Instant nextRetryAt, String detail) {
        this.nextRetryAt = nextRetryAt;
        this.retryCount++;
        this.lastErrorDetail = detail;
    }
}
