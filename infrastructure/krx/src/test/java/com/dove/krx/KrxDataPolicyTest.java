package com.dove.krx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KrxDataPolicyTest {

    @Test
    @DisplayName("화요일 → 월요일 (전 영업일)")
    void shouldReturnPrevWeekdayOnTuesday() {
        assertThat(KrxDataPolicy.lastAvailableDate(LocalDate.of(2026, 4, 28))) // 화
                .isEqualTo(LocalDate.of(2026, 4, 27)); // 월
    }

    @Test
    @DisplayName("월요일 → 금요일 (주말 건너뜀)")
    void shouldSkipWeekendOnMonday() {
        assertThat(KrxDataPolicy.lastAvailableDate(LocalDate.of(2026, 4, 27))) // 월
                .isEqualTo(LocalDate.of(2026, 4, 24)); // 금
    }

    @Test
    @DisplayName("토요일 → 목요일 (주말 양쪽 건너뜀)")
    void shouldSkipWeekendOnSaturday() {
        assertThat(KrxDataPolicy.lastAvailableDate(LocalDate.of(2026, 4, 25))) // 토
                .isEqualTo(LocalDate.of(2026, 4, 23)); // 목
    }

    @Test
    @DisplayName("일요일 → 목요일 (주말 양쪽 건너뜀)")
    void shouldSkipWeekendOnSunday() {
        assertThat(KrxDataPolicy.lastAvailableDate(LocalDate.of(2026, 4, 26))) // 일
                .isEqualTo(LocalDate.of(2026, 4, 23)); // 목
    }

    @Test
    @DisplayName("수요일 → 화요일")
    void shouldReturnPrevWeekdayOnWednesday() {
        assertThat(KrxDataPolicy.lastAvailableDate(LocalDate.of(2026, 1, 7))) // 수
                .isEqualTo(LocalDate.of(2026, 1, 6)); // 화
    }
}
