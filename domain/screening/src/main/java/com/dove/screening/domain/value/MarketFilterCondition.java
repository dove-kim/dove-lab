package com.dove.screening.domain.value;

import com.dove.market.domain.enums.MarketType;

import java.util.List;

/**
 * 종목이 지정 시장 중 하나에 속하는지 보는 조건.
 *
 * @param markets 허용 시장 목록
 */
public record MarketFilterCondition(List<MarketType> markets) implements FilterNode {
}
