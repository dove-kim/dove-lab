package com.dove.indicator.application.exception;

import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;

/**
 * 날짜 커밋 시점에 순위 커서가 달라져 CAS 전진이 거부됐음을 알린다.
 */
public class RankCursorRewoundException extends RuntimeException {

    public RankCursorRewoundException(MarketUniverse universe, PriceType priceType) {
        super("순위 커서 rewind 감지: %s/%s".formatted(universe, priceType));
    }
}
