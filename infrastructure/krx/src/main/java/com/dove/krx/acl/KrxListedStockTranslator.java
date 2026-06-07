package com.dove.krx.acl;

import com.dove.krx.StockListing;
import com.dove.krx.infrastructure.client.KrxListedStockItem;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;

/**
 * KRX 상장 종목 응답 항목을 도메인 StockListing으로 변환하는 ACL.
 */
public class KrxListedStockTranslator {

    private KrxListedStockTranslator() {}

    /**
     * KRX 응답 항목을 StockListing으로 변환한다.
     */
    public static StockListing translate(KrxListedStockItem item) {
        return new StockListing(
                item.getTicker().trim(),
                item.getIsin(),
                item.getListingDate(),
                item.getSecuGrpNm(),
                item.getKindStkCertTpNm()
        );
    }
}
