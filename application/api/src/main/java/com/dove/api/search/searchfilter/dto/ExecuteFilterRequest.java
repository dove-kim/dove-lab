package com.dove.api.search.searchfilter.dto;

import java.time.LocalDate;

/**
 * 검색 필터 실행 요청.
 *
 * @param referenceDate 기준일
 */
public record ExecuteFilterRequest(LocalDate referenceDate) {}
