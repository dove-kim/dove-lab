package com.dove.screening.domain.value;

import java.util.List;

/**
 * 자식 노드들을 결합 연산자로 묶는 그룹.
 *
 * @param children 자식 노드 (순서대로)
 * @param ops      i번째(1부터) 자식을 누적 결과에 결합할 연산자 (크기 = children 수 - 1)
 */
public record FilterGroup(List<FilterNode> children, List<FilterChildOp> ops) implements FilterNode {
}
