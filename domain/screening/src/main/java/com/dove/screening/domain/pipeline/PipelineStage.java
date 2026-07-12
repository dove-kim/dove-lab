package com.dove.screening.domain.pipeline;

/**
 * 검색 파이프라인의 한 실행 단계.
 */
public sealed interface PipelineStage permits FilterStage, RankStage {
}
