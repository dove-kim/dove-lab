package com.dove.eventretry.application.service;

import com.dove.eventretry.domain.entity.FailedEvent;
import com.dove.eventretry.domain.repository.FailedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(FailedEventCommandService.class)
class FailedEventCommandServiceTest {

    @Autowired
    private FailedEventCommandService service;

    @Autowired
    private FailedEventRepository repository;

    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");

    @Test
    @DisplayName("escalate — 신규는 INSERT")
    void shouldInsertWhenNew() {
        service.escalate("TOPIC", "key-1", "{}",
                "AUTH_FAILED", "401", now);

        List<FailedEvent> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getEventKey()).isEqualTo("key-1");
        assertThat(all.get(0).getReasonCategory()).isEqualTo("AUTH_FAILED");
    }

    @Test
    @DisplayName("escalate — 기존 (eventType, eventKey)는 중복 저장 안 함")
    void shouldNotDuplicate() {
        service.escalate("TOPIC", "key-1", "{}",
                "AUTH_FAILED", "401", now);
        service.escalate("TOPIC", "key-1", "{}",
                "RETRY_LIMIT_EXCEEDED", "second", now.plusSeconds(3600));

        assertThat(repository.findAll()).hasSize(1);
    }
}
