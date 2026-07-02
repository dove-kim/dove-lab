package com.dove.screening.infrastructure.repository;

import com.dove.modelserving.domain.entity.QStockModelScore;

/**
 * 검색식 SQL 변환 시 생성한 STOCK_MODEL_SCORE join 별칭 — 어느 거래일 오프셋·모델에 붙는지 함께 담는다.
 *
 * @param offset  기준일로부터의 거래일 오프셋 (이 별칭이 붙는 피처 행)
 * @param modelId 모델 식별자
 * @param alias   STOCK_MODEL_SCORE join 별칭
 */
record ModelScoreJoinAlias(int offset, long modelId, QStockModelScore alias) {
}
