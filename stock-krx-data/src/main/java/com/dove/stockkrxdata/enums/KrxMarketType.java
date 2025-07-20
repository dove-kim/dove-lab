package com.dove.stockkrxdata.enums;

import com.dove.stockkrxdata.client.KrxStockClient;
import com.dove.stockkrxdata.client.KrxStockResponse;
import com.dove.stockdata.enums.MarketType;

import java.time.LocalDate;
import java.util.function.BiFunction;

public enum KrxMarketType {
    KOSPI {
        @Override
        // KOSPI 시장에 맞는 KrxStockClient 메서드를 반환
        public BiFunction<String, LocalDate, KrxStockResponse> getClientMethod(KrxStockClient client) {
            return client::getDailyKospiStockInfo;
        }
    },
    KOSDAQ {
        @Override
        // KOSDAQ 시장에 맞는 KrxStockClient 메서드를 반환
        public BiFunction<String, LocalDate, KrxStockResponse> getClientMethod(KrxStockClient client) {
            return client::getDailyKosdaqStockInfo;
        }
    },
    KONEX {
        @Override
        // KONEX 시장은 현재 KrxStockClient에서 지원하지 않으므로 UnsupportedOperationException 발생
        public BiFunction<String, LocalDate, KrxStockResponse> getClientMethod(KrxStockClient client) {
            return (authKey, baseDate) -> {
                throw new UnsupportedOperationException("KONEX market data retrieval is not supported via KRX client.");
            };
        }
    };

    // 각 enum 상수가 구현해야 할 추상 메서드
    public abstract BiFunction<String, LocalDate, KrxStockResponse> getClientMethod(KrxStockClient client);


    public static KrxMarketType of(MarketType marketType) { // MarketType -> StockMarketType
        switch (marketType) {
            case KOSPI:
                return KrxMarketType.KOSPI;
            case KOSDAQ:
                return KrxMarketType.KOSDAQ;
            case KONEX:
                return KrxMarketType.KONEX;
            default:
                throw new IllegalArgumentException("marketType is not valid");
        }
    }

    public MarketType toMarketType() { // MarketType -> StockMarketType
        switch (this) {
            case KOSPI:
                return MarketType.KOSPI;
            case KOSDAQ:
                return MarketType.KOSDAQ;
            case KONEX:
                return MarketType.KONEX;
        }
        throw new IllegalArgumentException("marketType is not valid");
    }
}
