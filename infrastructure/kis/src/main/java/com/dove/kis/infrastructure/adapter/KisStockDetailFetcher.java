package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisProductInfoOutput;
import com.dove.kis.infrastructure.client.dto.KisProductInfoResponse;
import com.dove.kis.infrastructure.client.dto.KisStockInfoOutput;
import com.dove.kis.infrastructure.client.dto.KisStockInfoResponse;
import com.dove.kis.quota.KisGate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * KIS 종목 상세정보 어댑터 (CTPF1002R 주식기본, CTPF1604R 상품기본).
 */
@Component
@RequiredArgsConstructor
public class KisStockDetailFetcher {

    private static final String TR_STOCK_INFO = "CTPF1002R";
    private static final String TR_PRODUCT_INFO = "CTPF1604R";
    private static final String PRDT_TYPE_STOCK = "300";

    private final KisStockClient kisStockClient;
    private final KisGate kisGate;

    /** 주식기본조회. 실패하거나 데이터 없으면 empty. */
    public Optional<KisStockInfoOutput> fetchStockInfo(String ticker) {
        KisStockInfoResponse resp = kisGate.call(
                () -> kisStockClient.getStockInfo(TR_STOCK_INFO, ticker, PRDT_TYPE_STOCK));
        if (resp == null || !resp.isSuccess() || resp.getOutput() == null) return Optional.empty();
        return Optional.of(resp.getOutput());
    }

    /** 상품기본조회. 실패하거나 데이터 없으면 empty. */
    public Optional<KisProductInfoOutput> fetchProductInfo(String ticker) {
        KisProductInfoResponse resp = kisGate.call(
                () -> kisStockClient.getProductInfo(TR_PRODUCT_INFO, ticker, PRDT_TYPE_STOCK));
        if (resp == null || !resp.isSuccess() || resp.getOutput() == null) return Optional.empty();
        return Optional.of(resp.getOutput());
    }
}
