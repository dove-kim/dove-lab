package com.dove.krxmarketdata.acl;

import com.dove.krxmarketdata.application.dto.KrxStockInfo;
import com.dove.krxmarketdata.infrastructure.client.KrxStockResponse;
import com.dove.stockdata.domain.enums.MarketType;

/**
 * KRX API 응답(KrxStockResponse.Data)을 도메인 DTO(KrxStockInfo)로 변환하는 ACL 변환기.
 */
public class KrxStockDataTranslator {

    public static KrxStockInfo translate(KrxStockResponse.Data data, MarketType marketType) {
        return new KrxStockInfo(
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
