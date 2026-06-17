package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.Map;

/**
 * 검색식 SQL 변환 결과 — WHERE 술어 + 오프셋별 self-join 별칭.
 *
 * @param predicate     wide 테이블 WHERE 술어
 * @param offsetAliases 거래일 오프셋 → join 별칭 (0 = 기준 별칭)
 */
record TranslatedFilter(BooleanExpression predicate, Map<Integer, QStockFeatureDaily> offsetAliases) {
}
