package com.dove.api.search.searchfilter.dto;

import java.util.List;

/**
 * 검색 필터 표시 순서 변경 요청.
 *
 * @param ids 정렬된 필터 ID 목록
 */
public record FilterReorderRequest(List<Long> ids) {}
