package com.dove.screening.domain.pipeline;

import java.util.List;

/**
 * 현재 후보를 정렬 후 상위 N개로 제한하는 파이프라인 단계.
 *
 * @param sortKeys 정렬 키 목록(앞선 키 우선)
 * @param limit    상위 개수(null=상한 없음, 정렬만)
 */
public record RankStage(List<SortKey> sortKeys, Integer limit) implements PipelineStage {
}
