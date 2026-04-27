package com.dove.eventretry.application.service;

import com.dove.eventretry.domain.entity.PendingEventRetry;
import com.dove.eventretry.domain.repository.PendingEventRetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PendingEventRetryCommandService.class)
class PendingEventRetryCommandServiceTest {

    @Autowired
    private PendingEventRetryCommandService service;

    @Autowired
    private PendingEventRetryRepository repository;

    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");

    @Test
    @DisplayName("enqueueOrUpdate — 신규 event는 INSERT, retryCount=0")
    void shouldInsertWhenNew() {
        service.enqueueOrUpdate("TOPIC", "key-1", "{}",
                "UNCERTAIN", now.plusSeconds(600), now, "first");

        Optional<PendingEventRetry> saved = repository.findByEventTypeAndEventKey("TOPIC", "key-1");
        assertThat(saved).isPresent();
        assertThat(saved.get().getRetryCount()).isZero();
        assertThat(saved.get().getEventPayload()).isEqualTo("{}");
    }

    @Test
    @DisplayName("enqueueOrUpdate — 기존 event는 reschedule (retryCount 증가)")
    void shouldRescheduleWhenExisting() {
        service.enqueueOrUpdate("TOPIC", "key-1", "{}",
                "UNCERTAIN", now.plusSeconds(60), now, "first");
        service.enqueueOrUpdate("TOPIC", "key-1", "{}",
                "TRANSIENT", now.plusSeconds(3600), now, "second");

        PendingEventRetry reloaded = repository.findByEventTypeAndEventKey("TOPIC", "key-1").orElseThrow();
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
        assertThat(reloaded.getNextRetryAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(reloaded.getLastErrorDetail()).isEqualTo("second");
    }

    @Test
    @DisplayName("deleteByEvent — 존재하면 삭제, 없으면 no-op")
    void shouldDeleteIfExists() {
        service.enqueueOrUpdate("TOPIC", "key-1", "{}",
                "UNCERTAIN", now.plusSeconds(60), now, null);

        service.deleteByEvent("TOPIC", "key-1");

        assertThat(repository.findByEventTypeAndEventKey("TOPIC", "key-1")).isEmpty();
        service.deleteByEvent("TOPIC", "not-exists");
    }
}
