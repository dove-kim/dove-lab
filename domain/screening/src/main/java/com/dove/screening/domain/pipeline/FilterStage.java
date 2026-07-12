package com.dove.screening.domain.pipeline;

import com.dove.screening.domain.value.FilterNode;

/**
 * 조건 평가로 현재 후보를 축소하는 파이프라인 단계.
 *
 * @param filter 통과 여부를 평가할 필터 트리
 */
public record FilterStage(FilterNode filter) implements PipelineStage {
}
