package com.dove.screening.domain.pipeline;

/**
 * 정렬 키 하나 — 필드와 방향.
 *
 * @param field     정렬 기준 필드
 * @param direction 정렬 방향
 */
public record SortKey(SortField field, SortDirection direction) {
}
