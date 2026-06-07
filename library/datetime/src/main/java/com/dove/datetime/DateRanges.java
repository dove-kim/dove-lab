package com.dove.datetime;

import com.dove.datetime.dto.DateRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 날짜 구간 분할 유틸리티.
 */
public final class DateRanges {

    private DateRanges() {
    }

    /**
     * from~to 구간을 windowDays 일 단위로 나눈다 (마지막 구간은 to까지).
     *
     * @throws IllegalArgumentException windowDays가 1 미만인 경우
     */
    public static List<DateRange> split(LocalDate from, LocalDate to, int windowDays) {
        if (windowDays < 1) throw new IllegalArgumentException("windowDays must be >= 1: " + windowDays);
        List<DateRange> ranges = new ArrayList<>();
        LocalDate start = from;
        while (!start.isAfter(to)) {
            LocalDate end = start.plusDays(windowDays - 1);
            if (end.isAfter(to)) end = to;
            ranges.add(new DateRange(start, end));
            start = end.plusDays(1);
        }
        return ranges;
    }
}
