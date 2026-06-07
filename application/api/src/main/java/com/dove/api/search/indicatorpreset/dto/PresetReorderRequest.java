package com.dove.api.search.indicatorpreset.dto;

import java.util.List;

/**
 * 지표 프리셋 표시 순서 변경 요청.
 *
 * @param ids 정렬된 프리셋 ID 목록
 */
public record PresetReorderRequest(List<Long> ids) {}
