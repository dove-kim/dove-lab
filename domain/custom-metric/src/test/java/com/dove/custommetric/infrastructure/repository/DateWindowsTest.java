package com.dove.custommetric.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateWindows")
class DateWindowsTest {

    @Nested
    @DisplayName("yearly")
    class Yearly {

        @Test
        @DisplayName("같은 해 구간이면 창 하나로 그대로 반환한다")
        void shouldReturnSingleWindowWhenWithinSameYear() {
            List<DateWindow> windows = DateWindows.yearly(LocalDate.of(2024, 3, 10), LocalDate.of(2024, 8, 20));

            assertThat(windows).containsExactly(
                    new DateWindow(LocalDate.of(2024, 3, 10), LocalDate.of(2024, 8, 20)));
        }

        @Test
        @DisplayName("여러 해에 걸치면 연 경계로 나눈다")
        void shouldSplitAtYearBoundaries() {
            List<DateWindow> windows = DateWindows.yearly(LocalDate.of(2023, 11, 1), LocalDate.of(2025, 2, 15));

            assertThat(windows).containsExactly(
                    new DateWindow(LocalDate.of(2023, 11, 1), LocalDate.of(2023, 12, 31)),
                    new DateWindow(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                    new DateWindow(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 15)));
        }

        @Test
        @DisplayName("창들은 겹침·틈 없이 연속한다")
        void shouldBeContiguousWithoutGapOrOverlap() {
            List<DateWindow> windows = DateWindows.yearly(LocalDate.of(2020, 6, 1), LocalDate.of(2026, 7, 10));

            for (int i = 1; i < windows.size(); i++) {
                assertThat(windows.get(i).fromInclusive())
                        .isEqualTo(windows.get(i - 1).toInclusive().plusDays(1));
            }
            assertThat(windows.get(0).fromInclusive()).isEqualTo(LocalDate.of(2020, 6, 1));
            assertThat(windows.get(windows.size() - 1).toInclusive()).isEqualTo(LocalDate.of(2026, 7, 10));
        }

        @Test
        @DisplayName("하루짜리 구간도 창 하나로 반환한다")
        void shouldHandleSingleDay() {
            List<DateWindow> windows = DateWindows.yearly(LocalDate.of(2024, 5, 5), LocalDate.of(2024, 5, 5));

            assertThat(windows).containsExactly(
                    new DateWindow(LocalDate.of(2024, 5, 5), LocalDate.of(2024, 5, 5)));
        }

        @Test
        @DisplayName("from이 to보다 뒤면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenFromAfterTo() {
            assertThat(DateWindows.yearly(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1))).isEmpty();
        }

        @Test
        @DisplayName("from이 null이면 청크하지 않고 단일 창(null~to)을 반환한다")
        void shouldReturnSingleOpenWindowWhenFromNull() {
            List<DateWindow> windows = DateWindows.yearly(null, LocalDate.of(2026, 7, 10));

            assertThat(windows).containsExactly(new DateWindow(null, LocalDate.of(2026, 7, 10)));
        }

        @Test
        @DisplayName("to가 null이면 예외를 던진다")
        void shouldThrowWhenToNull() {
            assertThatThrownBy(() -> DateWindows.yearly(LocalDate.of(2024, 1, 1), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("monthly")
    class Monthly {

        @Test
        @DisplayName("같은 달 구간이면 창 하나로 그대로 반환한다")
        void shouldReturnSingleWindowWhenWithinSameMonth() {
            List<DateWindow> windows = DateWindows.monthly(LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 25));

            assertThat(windows).containsExactly(
                    new DateWindow(LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 25)));
        }

        @Test
        @DisplayName("여러 달에 걸치면 월 경계로 나눈다")
        void shouldSplitAtMonthBoundaries() {
            List<DateWindow> windows = DateWindows.monthly(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 3, 5));

            assertThat(windows).containsExactly(
                    new DateWindow(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 1, 31)),
                    new DateWindow(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)),
                    new DateWindow(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5)));
        }

        @Test
        @DisplayName("여러 해·달에 걸쳐도 겹침·틈 없이 연속한다")
        void shouldBeContiguousWithoutGapOrOverlap() {
            List<DateWindow> windows = DateWindows.monthly(LocalDate.of(2010, 1, 1), LocalDate.of(2026, 7, 10));

            for (int i = 1; i < windows.size(); i++) {
                assertThat(windows.get(i).fromInclusive())
                        .isEqualTo(windows.get(i - 1).toInclusive().plusDays(1));
            }
            assertThat(windows.get(0).fromInclusive()).isEqualTo(LocalDate.of(2010, 1, 1));
            assertThat(windows.get(windows.size() - 1).toInclusive()).isEqualTo(LocalDate.of(2026, 7, 10));
        }

        @Test
        @DisplayName("from이 to보다 뒤면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenFromAfterTo() {
            assertThat(DateWindows.monthly(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 4, 1))).isEmpty();
        }

        @Test
        @DisplayName("from이 null이면 청크하지 않고 단일 창(null~to)을 반환한다")
        void shouldReturnSingleOpenWindowWhenFromNull() {
            assertThat(DateWindows.monthly(null, LocalDate.of(2026, 7, 10)))
                    .containsExactly(new DateWindow(null, LocalDate.of(2026, 7, 10)));
        }

        @Test
        @DisplayName("to가 null이면 예외를 던진다")
        void shouldThrowWhenToNull() {
            assertThatThrownBy(() -> DateWindows.monthly(LocalDate.of(2024, 1, 1), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
