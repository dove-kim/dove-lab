package com.dove.api.ops.collection.dto;

import com.dove.stockcollection.domain.entity.CollectionTask;

import java.time.LocalDateTime;

/**
 * 수집 작업 상태 응답.
 */
public record CollectionTaskResponse(
        Long id,
        String type,
        String scope,
        String status,
        int total,
        int done,
        int progressPercent,
        int adjustedTotal,
        int adjustedDone,
        String errorCode,
        String errorDetail,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
    public static CollectionTaskResponse from(CollectionTask t) {
        int percent = t.getTotal() > 0 ? (int) ((long) t.getDone() * 100 / t.getTotal()) : 0;
        return new CollectionTaskResponse(
                t.getId(),
                t.getType().name(),
                t.getScope(),
                t.getStatus().name(),
                t.getTotal(),
                t.getDone(),
                percent,
                t.getAdjustedTotal(),
                t.getAdjustedDone(),
                t.getErrorCode(),
                t.getErrorDetail(),
                t.getCreatedAt(),
                t.getStartedAt(),
                t.getFinishedAt()
        );
    }
}
