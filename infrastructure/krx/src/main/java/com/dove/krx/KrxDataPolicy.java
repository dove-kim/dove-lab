package com.dove.krx;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * KRX 데이터 가용 정책.
 * KRX는 영업일 기준 +1 영업일 08:00에 데이터를 제공.
 * lastAvailableDate = prevWeekday(prevWeekday(today) - 1일)
 */
public final class KrxDataPolicy {

    private KrxDataPolicy() {}

    public static LocalDate lastAvailableDate(LocalDate today) {
        return weekdayAtOrBefore(weekdayAtOrBefore(today).minusDays(1));
    }

    private static LocalDate weekdayAtOrBefore(LocalDate date) {
        LocalDate d = date;
        while (isWeekend(d)) d = d.minusDays(1);
        return d;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
