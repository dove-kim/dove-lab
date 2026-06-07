package com.dove.scheduler.job;

import com.dove.scheduler.service.IndicatorComputeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * {@link IndicatorJob} 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IndicatorJob")
class IndicatorJobTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private IndicatorComputeService indicatorComputeService;

    @Nested
    @DisplayName("run() — 커서 기준 지표 계산")
    class Run {

        @Test
        @DisplayName("고정 Clock의 오늘 날짜로 computeAll을 호출한다")
        void shouldComputeAllWithClockDateWhenRun() {
            IndicatorJob job = new IndicatorJob(indicatorComputeService, CLOCK);

            job.run();

            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(indicatorComputeService).computeAll(dateCaptor.capture());
            assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.now(CLOCK));
        }
    }
}
