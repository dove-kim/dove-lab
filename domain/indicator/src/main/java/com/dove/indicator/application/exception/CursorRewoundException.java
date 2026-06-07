package com.dove.indicator.application.exception;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;

/**
 * 청크 커밋 시점에 커서가 rewind(또는 삭제)되어 CAS 전진이 거부됐음을 알린다.
 */
public class CursorRewoundException extends RuntimeException {

    public CursorRewoundException(String ticker, StockExchange exchange, PriceType priceType) {
        super("커서 rewind 감지: %s/%s/%s".formatted(ticker, exchange, priceType));
    }
}
