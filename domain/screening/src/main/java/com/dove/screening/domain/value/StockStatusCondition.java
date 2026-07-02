package com.dove.screening.domain.value;

import java.util.Set;

/**
 * 종목 상태(거래정지·관리종목) 제외 조건.
 *
 * @param exclude 제외할 상태 집합
 */
public record StockStatusCondition(Set<StockStatusType> exclude) implements FilterNode {

    /**
     * 종목의 거래정지·관리종목 여부로 통과 여부를 판단한다. 제외 대상에 걸리면 false.
     */
    public boolean passes(boolean halted, boolean admin) {
        if (exclude.contains(StockStatusType.TRADING_HALT) && halted) return false;
        if (exclude.contains(StockStatusType.ADMIN_ITEM) && admin) return false;
        return true;
    }
}
