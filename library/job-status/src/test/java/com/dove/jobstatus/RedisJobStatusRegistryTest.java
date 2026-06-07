package com.dove.jobstatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RedisJobStatusRegistry")
class RedisJobStatusRegistryTest {

    private static final String KEY = "scheduler:job:status";

    private RedisJobStatusRegistry registry;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations hash = mock(HashOperations.class);
        Map<Object, Object> store = new HashMap<>();
        when(redis.opsForHash()).thenReturn(hash);
        doAnswer(inv -> store.put(inv.getArgument(1), inv.getArgument(2)))
                .when(hash).put(eq(KEY), any(), any());
        when(hash.get(eq(KEY), any())).thenAnswer(inv -> store.get(inv.getArgument(1)));
        when(hash.entries(KEY)).thenReturn(store);

        registry = new RedisJobStatusRegistry(redis, new ObjectMapper());
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        @DisplayName("shouldRecordRunningStateWhenStartCalled")
        void shouldRecordRunningStateWhenStartCalled() {
            registry.start("INDICATOR", 600);

            JobStatus status = registry.find("INDICATOR").orElseThrow();
            assertThat(status.getState()).isEqualTo(JobState.RUNNING);
            assertThat(status.getTotal()).isEqualTo(600);
            assertThat(status.getProcessed()).isZero();
        }
    }

    @Nested
    @DisplayName("progress")
    class Progress {

        @Test
        @DisplayName("shouldUpdateProcessedCountWhenProgressCalled")
        void shouldUpdateProcessedCountWhenProgressCalled() {
            registry.start("INDICATOR", 600);

            registry.progress("INDICATOR", 250);

            assertThat(registry.find("INDICATOR").orElseThrow().getProcessed()).isEqualTo(250);
        }

        @Test
        @DisplayName("shouldDoNothingWhenJobNotFound")
        void shouldDoNothingWhenJobNotFound() {
            registry.progress("NOT_STARTED", 100);

            assertThat(registry.find("NOT_STARTED")).isEmpty();
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("shouldSetProcessedEqualToTotalWhenCompleted")
        void shouldSetProcessedEqualToTotalWhenCompleted() {
            registry.start("INDICATOR", 600);
            registry.progress("INDICATOR", 250); // 스로틀 자투리

            registry.complete("INDICATOR");

            JobStatus status = registry.find("INDICATOR").orElseThrow();
            assertThat(status.getState()).isEqualTo(JobState.COMPLETED);
            assertThat(status.getProcessed()).isEqualTo(600);
        }

        @Test
        @DisplayName("shouldDoNothingWhenJobNotFound")
        void shouldDoNothingWhenJobNotFound() {
            registry.complete("NOT_STARTED");

            assertThat(registry.find("NOT_STARTED")).isEmpty();
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("shouldRecordFailedStateWithMessageWhenFailed")
        void shouldRecordFailedStateWithMessageWhenFailed() {
            registry.start("STOCK_DETAIL", 10);

            registry.fail("STOCK_DETAIL", "KIS rate limit");

            JobStatus status = registry.find("STOCK_DETAIL").orElseThrow();
            assertThat(status.getState()).isEqualTo(JobState.FAILED);
            assertThat(status.getMessage()).isEqualTo("KIS rate limit");
        }

        @Test
        @DisplayName("shouldCreateNewFailedEntryWhenNoJobExists")
        void shouldCreateNewFailedEntryWhenNoJobExists() {
            registry.fail("DAILY_PRICE", "unexpected error");

            JobStatus status = registry.find("DAILY_PRICE").orElseThrow();
            assertThat(status.getState()).isEqualTo(JobState.FAILED);
            assertThat(status.getMessage()).isEqualTo("unexpected error");
            assertThat(status.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("all")
    class All {

        @Test
        @DisplayName("shouldReturnAllStatusesWhenMultipleJobsRegistered")
        void shouldReturnAllStatusesWhenMultipleJobsRegistered() {
            registry.start("INDICATOR", 1);
            registry.start("STOCK_DETAIL", 2);

            assertThat(registry.all()).extracting(JobStatus::getName)
                    .containsExactlyInAnyOrder("INDICATOR", "STOCK_DETAIL");
        }

        @Test
        @DisplayName("shouldReturnEmptyListWhenNoJobsRegistered")
        void shouldReturnEmptyListWhenNoJobsRegistered() {
            assertThat(registry.all()).isEmpty();
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        @DisplayName("shouldReturnEmptyListWhenRedisThrows")
        void shouldReturnEmptyListWhenRedisThrows() {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations hash = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hash);
            when(hash.entries(KEY)).thenThrow(new RuntimeException("Redis connection refused"));
            RedisJobStatusRegistry faultyRegistry = new RedisJobStatusRegistry(redis, new ObjectMapper());

            assertThat(faultyRegistry.all()).isEmpty();
        }
    }

    @Nested
    @DisplayName("find")
    class Find {

        @Test
        @DisplayName("shouldReturnEmptyWhenJobNotFound")
        void shouldReturnEmptyWhenJobNotFound() {
            assertThat(registry.find("NONEXISTENT")).isEmpty();
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        @DisplayName("shouldReturnEmptyWhenRedisThrows")
        void shouldReturnEmptyWhenRedisThrows() {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations hash = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hash);
            when(hash.get(eq(KEY), any())).thenThrow(new RuntimeException("Redis connection refused"));
            RedisJobStatusRegistry faultyRegistry = new RedisJobStatusRegistry(redis, new ObjectMapper());

            assertThat(faultyRegistry.find("INDICATOR")).isEmpty();
        }
    }
}
