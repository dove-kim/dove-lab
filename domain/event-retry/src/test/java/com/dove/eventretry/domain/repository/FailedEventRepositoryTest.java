package com.dove.eventretry.domain.repository;

import com.dove.eventretry.domain.entity.FailedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FailedEventRepositoryTest {

    @Autowired
    private FailedEventRepository repository;

    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");

    @Test
    @DisplayName("existsByEventTypeAndEventKey — 저장 후 존재 확인")
    void shouldCheckExistenceByEventTypeAndKey() {
        repository.save(FailedEvent.escalate(
                "STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17", "{}",
                "AUTH_FAILED", "401", now));

        assertThat(repository.existsByEventTypeAndEventKey(
                "STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17")).isTrue();
        assertThat(repository.existsByEventTypeAndEventKey(
                "STOCK_PRICE_QUERY", "other")).isFalse();
    }
}
