package com.dove.api.ops.model.dto;

/**
 * 점수 삭제 결과.
 *
 * @param deleted 삭제된 점수 행 수
 */
public record DeleteScoresResponse(long deleted) {}
