package com.dove.krx.acl;

import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.stockprice.application.port.StockInfo;
import com.dove.market.domain.enums.MarketType;

/** KRX 응답을 중립 StockInfo로 변환하는 ACL. */
public class KrxDailyStockPriceTranslator {

    public static StockInfo translate(KrxDailyPriceResponse.Data data, MarketType marketType) {
        return new StockInfo(
                data.getBaseDate(),
                marketType,
                data.getStockName(),
                data.getStockCode(),
                data.getTradingVolume(),
                data.getOpeningPrice(),
                data.getClosingPrice(),
                data.getLowPrice(),
                data.getHighPrice()
        );
    }
}
