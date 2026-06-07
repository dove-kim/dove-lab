package com.dove.krx.quota;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyApiQuota")
@SuppressWarnings("unchecked")
class DailyApiQuotaTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    DailyApiQuota quota;

    static final String LIMIT_KEY = "krx:api:daily-limit";
    static final String LAST_LIMIT_KEY = "krx:api:last-rate-limit-at";
    static final int DEFAULT_LIMIT = 100;

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
        quota = new DailyApiQuota(redis, "krx:api", DEFAULT_LIMIT);
    }

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("shouldReturnTrueAndSetTtlWhenFirstCallOfDay")
        void shouldReturnTrueAndSetTtlWhenFirstCallOfDay() {
            given(valueOps.increment(anyString())).willReturn(1L);
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            assertThat(quota.tryAcquire()).isTrue();
            verify(redis).expire(anyString(), eq(Duration.ofHours(26)));
        }

        @Test
        @DisplayName("shouldReturnTrueWhenCountWithinLimit")
        void shouldReturnTrueWhenCountWithinLimit() {
            given(valueOps.increment(anyString())).willReturn((long) DEFAULT_LIMIT);
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            assertThat(quota.tryAcquire()).isTrue();
            verify(redis, never()).expire(anyString(), eq(Duration.ofHours(26)));
        }

        @Test
        @DisplayName("shouldReturnFalseAndDecrementWhenCountExceedsLimit")
        void shouldReturnFalseAndDecrementWhenCountExceedsLimit() {
            given(valueOps.increment(anyString())).willReturn((long) DEFAULT_LIMIT + 1);
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            assertThat(quota.tryAcquire()).isFalse();
            verify(valueOps).decrement(anyString());
        }

        @Test
        @DisplayName("shouldReturnFalseAndDecrementWhenIncrementReturnsNull")
        void shouldReturnFalseAndDecrementWhenIncrementReturnsNull() {
            given(valueOps.increment(anyString())).willReturn(null);
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            assertThat(quota.tryAcquire()).isFalse();
            verify(valueOps).decrement(anyString());
        }
    }

    @Nested
    @DisplayName("ensureLimit")
    class EnsureLimit {

        @Test
        @DisplayName("shouldInitializeLimitWhenAbsentInRedis")
        void shouldInitializeLimitWhenAbsentInRedis() {
            given(valueOps.get(LIMIT_KEY)).willReturn(null);

            quota.ensureLimit(DEFAULT_LIMIT, "TEST");

            verify(valueOps).set(LIMIT_KEY, String.valueOf(DEFAULT_LIMIT));
        }

        @Test
        @DisplayName("shouldUpdateLimitWhenStoredValueDiffers")
        void shouldUpdateLimitWhenStoredValueDiffers() {
            given(valueOps.get(LIMIT_KEY)).willReturn("50");

            quota.ensureLimit(DEFAULT_LIMIT, "TEST");

            verify(valueOps).set(LIMIT_KEY, String.valueOf(DEFAULT_LIMIT));
        }

        @Test
        @DisplayName("shouldNotUpdateLimitWhenStoredValueMatches")
        void shouldNotUpdateLimitWhenStoredValueMatches() {
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            quota.ensureLimit(DEFAULT_LIMIT, "TEST");

            verify(valueOps, never()).set(eq(LIMIT_KEY), anyString());
        }
    }

    @Nested
    @DisplayName("markRemoteLimited")
    class MarkRemoteLimited {

        @Test
        @DisplayName("shouldSetCountToLimitAndSetTtlAndRecordTimestamp")
        void shouldSetCountToLimitAndSetTtlAndRecordTimestamp() {
            given(valueOps.get(LIMIT_KEY)).willReturn(String.valueOf(DEFAULT_LIMIT));

            quota.markRemoteLimited();

            verify(valueOps).set(anyString(), eq(String.valueOf(DEFAULT_LIMIT)));
            verify(redis).expire(anyString(), eq(Duration.ofHours(26)));
            verify(valueOps).set(eq(LAST_LIMIT_KEY), anyString());
        }
    }

    @Nested
    @DisplayName("currentCount")
    class CurrentCount {

        @Test
        @DisplayName("shouldReturnZeroWhenNoCountStored")
        void shouldReturnZeroWhenNoCountStored() {
            given(valueOps.get(anyString())).willReturn(null);

            assertThat(quota.currentCount()).isZero();
        }

        @Test
        @DisplayName("shouldReturnParsedCountWhenStored")
        void shouldReturnParsedCountWhenStored() {
            given(valueOps.get(anyString())).willReturn("42");

            assertThat(quota.currentCount()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("lastLimitAt")
    class LastLimitAt {

        @Test
        @DisplayName("shouldReturnEmptyWhenNoLimitRecorded")
        void shouldReturnEmptyWhenNoLimitRecorded() {
            given(valueOps.get(LAST_LIMIT_KEY)).willReturn(null);

            assertThat(quota.lastLimitAt()).isEmpty();
        }

        @Test
        @DisplayName("shouldReturnParsedDateTimeWhenLimitRecorded")
        void shouldReturnParsedDateTimeWhenLimitRecorded() {
            ZonedDateTime expected = ZonedDateTime.parse("2026-06-07T10:00:00+09:00",
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            given(valueOps.get(LAST_LIMIT_KEY))
                    .willReturn("2026-06-07T10:00:00+09:00");

            Optional<ZonedDateTime> result = quota.lastLimitAt();

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }
    }
}
