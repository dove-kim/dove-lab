package com.dove.eventsupport.domain.entity;

import com.dove.eventsupport.domain.enums.FailedEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "FAILED_EVENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TOPIC", nullable = false, length = 100)
    private String topic;

    @Column(name = "MESSAGE_KEY", length = 255)
    private String messageKey;

    @Column(name = "PAYLOAD", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "ERROR_TYPE", nullable = false, length = 255)
    private String errorType;

    @Column(name = "ERROR_MESSAGE", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "LISTENER_NAME", nullable = false, length = 255)
    private String listenerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private FailedEventStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    private FailedEvent(String topic, String messageKey, String payload,
                        String errorType, String errorMessage, String listenerName) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.listenerName = listenerName;
        this.status = FailedEventStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static FailedEvent of(String topic, String messageKey, String payload,
                                  String errorType, String errorMessage, String listenerName) {
        return new FailedEvent(topic, messageKey, payload, errorType, errorMessage, listenerName);
    }

    public void resolve() {
        this.status = FailedEventStatus.RESOLVED;
    }
}
