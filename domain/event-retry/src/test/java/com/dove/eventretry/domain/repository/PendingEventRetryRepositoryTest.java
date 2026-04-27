package com.dove.eventretry.domain.repository;

import com.dove.eventretry.domain.entity.PendingEventRetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PendingEventRetryRepositoryTest {

    @Autowired
    private PendingEventRetryRepository repository;

    private final Instant now = Instant.parse("2026-04-21T00:00:00Z");

    @Test
    @DisplayName("findByEventTypeAndEventKey — 저장 후 (eventType, eventKey)로 조회")
    void shouldFindByEventTypeAndEventKey() {
        repository.save(PendingEventRetry.create(
                "STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17", "{}",
                "UNCERTAIN", now.plusSeconds(600), now, "test"));

        Optional<PendingEventRetry> found = repository.findByEventTypeAndEventKey(
                "STOCK_PRICE_QUERY", "daily-KOSPI-2026-04-17");

        assertThat(found).isPresent();
        assertThat(found.get().getEventPayload()).isEqualTo("{}");
        assertThat(found.get().getRetryCount()).isZero();
    }

    @Test
    @DisplayName("findAllByNextRetryAtLessThanEqualOrderByNextRetryAtAsc — due 항목만 오름차순")
    void shouldFindDueItemsOrderedByNextRetryAsc() {
        repository.save(PendingEventRetry.create(
                "T", "past-1", "{}", "UNCERTAIN", now.minusSeconds(300), now, null));
        repository.save(PendingEventRetry.create(
                "T", "past-2", "{}", "UNCERTAIN", now.minusSeconds(60), now, null));
        repository.save(PendingEventRetry.create(
                "T", "future", "{}", "UNCERTAIN", now.plusSeconds(300), now, null));

        List<PendingEventRetry> due = repository.findAllByNextRetryAtLessThanEqualOrderByNextRetryAtAsc(now);

        assertThat(due).hasSize(2);
        assertThat(due).extracting(PendingEventRetry::getEventKey)
                .containsExactly("past-1", "past-2");
    }

    @Test
    @DisplayName("reschedule — nextRetryAt 갱신 + retryCount 증가 + lastErrorDetail 업데이트")
    void shouldRescheduleIncrementCount() {
        PendingEventRetry saved = repository.save(PendingEventRetry.create(
                "T", "k", "{}", "UNCERTAIN", now.plusSeconds(60), now, "first"));

        Instant next = now.plusSeconds(3600);
        saved.reschedule(next, "second");
        repository.flush();

        PendingEventRetry reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
        assertThat(reloaded.getNextRetryAt()).isEqualTo(next);
        assertThat(reloaded.getLastErrorDetail()).isEqualTo("second");
    }
}
