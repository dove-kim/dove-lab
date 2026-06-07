package com.dove.screening.domain.value;

/**
 * 수치 범위.
 *
 * @param min          하한
 * @param max          상한
 * @param minInclusive 하한 포함 여부
 * @param maxInclusive 상한 포함 여부
 */
public record FilterRange(double min, double max, boolean minInclusive, boolean maxInclusive) {

    /**
     * 값이 범위 안에 있는지 여부를 반환한다.
     */
    public boolean contains(double v) {
        boolean lo = minInclusive ? v >= min : v > min;
        boolean hi = maxInclusive ? v <= max : v < max;
        return lo && hi;
    }
}
