package com.dove.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Parallel")
class ParallelTest {

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("shouldProcessAllTasksWhenRunning")
        void shouldProcessAllTasksWhenRunning() {
            List<Integer> items = IntStream.range(0, 1000).boxed().toList();
            ConcurrentLinkedQueue<Integer> processed = new ConcurrentLinkedQueue<>();

            Parallel.run(items, 16, processed::add);

            assertThat(processed).hasSize(1000);
            assertThat(processed).containsExactlyInAnyOrderElementsOf(items);
        }

        @Test
        @DisplayName("shouldNotExceedConcurrencyLimit")
        void shouldNotExceedConcurrencyLimit() {
            List<Integer> items = IntStream.range(0, 500).boxed().toList();
            int concurrency = 8;
            AtomicInteger inFlight = new AtomicInteger();
            AtomicInteger maxObserved = new AtomicInteger();

            Parallel.run(items, concurrency, i -> {
                int now = inFlight.incrementAndGet();
                maxObserved.accumulateAndGet(now, Math::max);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                inFlight.decrementAndGet();
            });

            assertThat(maxObserved.get()).isLessThanOrEqualTo(concurrency);
        }

        @Test
        @DisplayName("shouldEnforceSequentialExecutionWhenConcurrencyIsOne")
        void shouldEnforceSequentialExecutionWhenConcurrencyIsOne() {
            List<Integer> items = IntStream.range(0, 50).boxed().toList();
            AtomicInteger inFlight = new AtomicInteger();
            AtomicInteger maxObserved = new AtomicInteger();

            Parallel.run(items, 1, i -> {
                int now = inFlight.incrementAndGet();
                maxObserved.accumulateAndGet(now, Math::max);
                inFlight.decrementAndGet();
            });

            assertThat(maxObserved.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("shouldThrowParallelExceptionWrappingCauseWhenHandlerThrows")
        void shouldThrowParallelExceptionWrappingCauseWhenHandlerThrows() {
            List<Integer> items = IntStream.range(0, 100).boxed().toList();

            assertThatThrownBy(() -> Parallel.run(items, 4, i -> {
                if (i == 50) throw new IllegalStateException("boom");
            }))
                    .isInstanceOf(ParallelException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("boom");
        }

        @Test
        @DisplayName("shouldStopSubmittingTasksWhenErrorOccurs")
        void shouldStopSubmittingTasksWhenErrorOccurs() {
            List<Integer> items = IntStream.range(0, 100_000).boxed().toList();
            AtomicInteger processed = new AtomicInteger();

            assertThatThrownBy(() -> Parallel.run(items, 4, i -> {
                processed.incrementAndGet();
                if (i == 0) throw new RuntimeException("early fail");
            })).isInstanceOf(ParallelException.class);

            assertThat(processed.get()).isLessThan(items.size());
        }

        @Test
        @DisplayName("shouldDoNothingWhenTaskListIsEmpty")
        void shouldDoNothingWhenTaskListIsEmpty() {
            Parallel.run(List.of(), 4, i -> {
                throw new AssertionError("호출되면 안 됨");
            });
        }
    }
}
