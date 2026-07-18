package com.dove.custommetric.infrastructure.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 큰 거래일 구간을 연 단위 청크로 쪼개는 유틸 — 단일 대형 집계 쿼리의 스캔 범위를 축소한다.
 */
public final class DateWindows {

    private DateWindows() {
    }

    /**
     * [fromInclusive, toInclusive]를 달력 연 경계로 잘라 겹침·틈 없는 창 목록으로 반환한다(오름차순).
     * fromInclusive가 null이면 하한을 모르므로 청크하지 않고 단일 창을 반환한다.
     * fromInclusive가 toInclusive보다 뒤면 빈 목록.
     *
     * @throws IllegalArgumentException toInclusive가 null인 경우
     */
    public static List<DateWindow> yearly(LocalDate fromInclusive, LocalDate toInclusive) {
        if (toInclusive == null) throw new IllegalArgumentException("toInclusive는 필수");
        if (fromInclusive == null) return List.of(new DateWindow(null, toInclusive));
        if (fromInclusive.isAfter(toInclusive)) return List.of();

        List<DateWindow> windows = new ArrayList<>();
        LocalDate cursor = fromInclusive;
        while (!cursor.isAfter(toInclusive)) {
            LocalDate yearEnd = LocalDate.of(cursor.getYear(), 12, 31);
            LocalDate end = yearEnd.isBefore(toInclusive) ? yearEnd : toInclusive;
            windows.add(new DateWindow(cursor, end));
            cursor = end.plusDays(1);
        }
        return windows;
    }

    /**
     * [fromInclusive, toInclusive]를 달력 월 경계로 잘라 겹침·틈 없는 창 목록으로 반환한다(오름차순).
     * fromInclusive가 null이면 하한을 모르므로 청크하지 않고 단일 창을 반환한다.
     * fromInclusive가 toInclusive보다 뒤면 빈 목록.
     *
     * @throws IllegalArgumentException toInclusive가 null인 경우
     */
    public static List<DateWindow> monthly(LocalDate fromInclusive, LocalDate toInclusive) {
        if (toInclusive == null) throw new IllegalArgumentException("toInclusive는 필수");
        if (fromInclusive == null) return List.of(new DateWindow(null, toInclusive));
        if (fromInclusive.isAfter(toInclusive)) return List.of();

        List<DateWindow> windows = new ArrayList<>();
        LocalDate cursor = fromInclusive;
        while (!cursor.isAfter(toInclusive)) {
            LocalDate monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
            LocalDate end = monthEnd.isBefore(toInclusive) ? monthEnd : toInclusive;
            windows.add(new DateWindow(cursor, end));
            cursor = end.plusDays(1);
        }
        return windows;
    }
}
