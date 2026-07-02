package com.dove.indicator.application.exception;

import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;

/**
 * 날짜 커밋 시점에 상승비율 커서가 달라져 CAS 전진이 거부됐음을 알린다.
 */
public class BreadthCursorRewoundException extends RuntimeException {

    public BreadthCursorRewoundException(MarketUniverse universe, PriceType priceType) {
        super("상승비율 커서 rewind 감지: %s/%s".formatted(universe, priceType));
    }
}
