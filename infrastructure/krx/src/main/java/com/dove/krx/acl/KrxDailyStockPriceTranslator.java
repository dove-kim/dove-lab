package com.dove.krx.acl;

import com.dove.krx.StockPrice;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;

/** KRX 주가 응답을 StockPrice로 변환하는 ACL. */
public class KrxDailyStockPriceTranslator {

    public static StockPrice translate(KrxDailyPriceResponse.Data data) {
        return new StockPrice(
                data.getStockCode(),
                data.getTradingVolume(),
                data.getOpeningPrice(),
                data.getClosingPrice(),
                data.getLowPrice(),
                data.getHighPrice()
        );
    }
}
