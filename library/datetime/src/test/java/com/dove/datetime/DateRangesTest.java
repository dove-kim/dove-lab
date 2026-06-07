package com.dove.datetime;

import com.dove.datetime.dto.DateRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangesTest {

    @Test
    @DisplayName("기간이 윈도우보다 짧으면 단일 구간으로 반환한다")
    void shouldReturnSingleRangeWhenShorterThanWindow() {
        List<DateRange> ranges = DateRanges.split(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10), 100);

        assertThat(ranges).containsExactly(
                new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10)));
    }

    @Test
    @DisplayName("기간을 윈도우 일수 단위로 나누고 마지막 구간은 to까지 채운다")
    void shouldSplitIntoWindows() {
        List<DateRange> ranges = DateRanges.split(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10), 4);

        assertThat(ranges).containsExactly(
                new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4)),
                new DateRange(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 8)),
                new DateRange(LocalDate.of(2024, 1, 9), LocalDate.of(2024, 1, 10)));
    }

    @Test
    @DisplayName("구간 경계가 정확히 떨어지면 마지막 구간도 윈도우 크기와 같다")
    void shouldSplitEvenly() {
        List<DateRange> ranges = DateRanges.split(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8), 4);

        assertThat(ranges).containsExactly(
                new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4)),
                new DateRange(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 8)));
    }

    @Test
    @DisplayName("from == to 이면 하루짜리 단일 구간")
    void shouldReturnSingleDayRange() {
        List<DateRange> ranges = DateRanges.split(
                LocalDate.of(2024, 3, 2), LocalDate.of(2024, 3, 2), 100);

        assertThat(ranges).containsExactly(
                new DateRange(LocalDate.of(2024, 3, 2), LocalDate.of(2024, 3, 2)));
    }

    @Test
    @DisplayName("from 이 to 보다 늦으면 빈 목록")
    void shouldReturnEmptyWhenFromAfterTo() {
        List<DateRange> ranges = DateRanges.split(
                LocalDate.of(2024, 3, 3), LocalDate.of(2024, 3, 2), 100);

        assertThat(ranges).isEmpty();
    }

    @Test
    @DisplayName("windowDays가 1 미만이면 예외를 던진다")
    void shouldThrowWhenWindowDaysNotPositive() {
        assertThatThrownBy(() -> DateRanges.split(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
