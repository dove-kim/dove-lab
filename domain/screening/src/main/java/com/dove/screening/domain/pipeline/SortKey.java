package com.dove.screening.domain.pipeline;

/**
 * 정렬 키 하나 — 필드와 방향.
 *
 * @param field     정렬 기준 필드
 * @param direction 정렬 방향
 * @param modelId   MODEL_SCORE 정렬 시 대상 모델 ID(그 외 null)
 */
public record SortKey(SortField field, SortDirection direction, Long modelId) {
}
