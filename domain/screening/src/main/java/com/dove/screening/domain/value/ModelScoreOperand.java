package com.dove.screening.domain.value;

/**
 * ML 모델 채점 점수 피연산자.
 *
 * @param modelId 모델 식별자
 * @param offset  거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record ModelScoreOperand(long modelId, int offset) implements FilterOperand {
}
