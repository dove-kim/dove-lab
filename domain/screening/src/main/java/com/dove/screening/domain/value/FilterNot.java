package com.dove.screening.domain.value;

/**
 * 자식 노드 결과를 부정한다.
 *
 * @param inner 부정 대상 노드
 */
public record FilterNot(FilterNode inner) implements FilterNode {
}
