package com.dove.screening.domain.value;

/**
 * 해석할 수 없는 조건. 인메모리 평가는 false, SQL 변환은 폴백으로 처리한다.
 */
public record UnknownCondition() implements FilterNode {
}
