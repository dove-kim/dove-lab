package com.dove.modelserving.domain.meta;

import java.util.List;

/**
 * 채점 대상을 좁히는 진입존 정의.
 *
 * @param desc       설명
 * @param conditions 진입 조건식 목록(예: "rsi_14>=50")
 */
public record ModelEntryZone(String desc, List<String> conditions) {
}
