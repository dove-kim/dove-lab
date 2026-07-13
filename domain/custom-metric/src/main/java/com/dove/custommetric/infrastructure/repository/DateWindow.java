package com.dove.custommetric.infrastructure.repository;

import java.time.LocalDate;

/**
 * 집계 조회를 나눠 실행할 거래일 구간(양끝 포함).
 *
 * @param fromInclusive 시작일(포함). 하한 없음이면 null
 * @param toInclusive   종료일(포함)
 */
public record DateWindow(LocalDate fromInclusive, LocalDate toInclusive) {
}
