package com.dove.krx.acl;

import com.dove.krx.StockListing;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;

/** KRX 종목 응답을 StockListing으로 변환하는 ACL. */
public class KrxListedStockTranslator {

    public static StockListing translate(KrxListedStockResponse.Item item) {
        return new StockListing(
                item.getTicker().trim(),
                item.getIsin(),
                item.getStockName(),
                item.getListingDate()
        );
    }
}
