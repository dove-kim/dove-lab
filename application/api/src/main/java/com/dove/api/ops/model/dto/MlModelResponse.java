package com.dove.api.ops.model.dto;

import com.dove.modelserving.domain.entity.MlModel;
import com.dove.stock.domain.enums.StockExchange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 모델 메타·상태·채점 진행 정보 응답(아티팩트 바이트·meta.json 원본은 제외).
 *
 * @param id             모델 식별자
 * @param name           모델 이름
 * @param version        버전
 * @param outputType     출력 의미(PROBABILITY/REGRESSION)
 * @param scoreExchanges 채점 대상 거래소 이름 목록
 * @param scorePriceType 채점 대상 주가유형
 * @param status         채점 활성 상태(ACTIVE/INACTIVE)
 * @param scoreCursor    마지막으로 채점 완료된 거래일(null이면 미시작)
 * @param lastScoredAt   마지막 채점 성공 일시(null이면 성공 이력 없음)
 * @param lastError      마지막 채점 실패 사유(코드:메시지). 성공 시 null
 * @param createdBy      등록자
 * @param createdAt      생성 일시
 * @param updatedAt      최종 갱신 일시
 */
public record MlModelResponse(
        Long id,
        String name,
        String version,
        String outputType,
        List<String> scoreExchanges,
        String scorePriceType,
        String status,
        LocalDate scoreCursor,
        LocalDateTime lastScoredAt,
        String lastError,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 모델 엔티티를 응답 DTO로 변환한다.
     */
    public static MlModelResponse from(MlModel model) {
        return new MlModelResponse(
                model.getId(),
                model.getName(),
                model.getVersion(),
                model.getOutputType().name(),
                model.getScoreExchanges().stream().map(StockExchange::name).toList(),
                model.getScorePriceType().name(),
                model.getStatus().name(),
                model.getScoreCursor(),
                model.getLastScoredAt(),
                model.getLastError(),
                model.getCreatedBy(),
                model.getCreatedAt(),
                model.getUpdatedAt());
    }
}
