package com.dove.datetime.dto;

import java.time.LocalDate;

/**
 * 시작일·종료일로 이루어진 날짜 구간 (양끝 포함).
 */
public record DateRange(LocalDate from, LocalDate to) {
}
