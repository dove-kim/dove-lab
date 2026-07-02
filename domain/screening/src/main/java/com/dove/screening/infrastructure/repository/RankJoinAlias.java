package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.rank.entity.QStockRankDaily;

/**
 * 검색식 SQL 변환 시 생성한 STOCK_RANK_DAILY self-join 별칭 — 어느 거래일 오프셋에 붙는지 함께 담는다.
 *
 * @param offset 기준일로부터의 거래일 오프셋 (이 별칭이 붙는 피처 행)
 * @param alias  STOCK_RANK_DAILY join 별칭
 */
record RankJoinAlias(int offset, QStockRankDaily alias) {
}
