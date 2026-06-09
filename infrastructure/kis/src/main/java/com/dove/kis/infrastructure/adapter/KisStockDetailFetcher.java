package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisStockClient;
import com.dove.kis.infrastructure.client.dto.KisProductInfoOutput;
import com.dove.kis.infrastructure.client.dto.KisProductInfoResponse;
import com.dove.kis.infrastructure.client.dto.KisStockInfoOutput;
import com.dove.kis.infrastructure.client.dto.KisStockInfoResponse;
import com.dove.kis.quota.KisGate;
import com.dove.stockcollection.application.port.StockDetailFetcher;
import com.dove.stockcollection.application.port.StockInfoData;
import com.dove.stockcollection.application.port.StockProductData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * KIS 종목 상세정보 어댑터 (CTPF1002R 주식기본, CTPF1604R 상품기본).
 */
@Component
@RequiredArgsConstructor
public class KisStockDetailFetcher implements StockDetailFetcher {

    private static final String TR_STOCK_INFO = "CTPF1002R";
    private static final String TR_PRODUCT_INFO = "CTPF1604R";
    private static final String PRDT_TYPE_STOCK = "300";

    private final KisStockClient kisStockClient;
    private final KisGate kisGate;

    /**
     * 주식기본조회 결과를 반환한다. 실패하거나 데이터 없으면 empty.
     */
    @Override
    public Optional<StockInfoData> fetchStockInfo(String ticker) {
        KisStockInfoResponse resp = kisGate.call(
                () -> kisStockClient.getStockInfo(TR_STOCK_INFO, ticker, PRDT_TYPE_STOCK));
        if (resp == null || !resp.isSuccess() || resp.getOutput() == null) return Optional.empty();
        return Optional.of(toStockInfoData(resp.getOutput()));
    }

    /**
     * 상품기본조회 결과를 반환한다. 실패하거나 데이터 없으면 empty.
     */
    @Override
    public Optional<StockProductData> fetchProductInfo(String ticker) {
        KisProductInfoResponse resp = kisGate.call(
                () -> kisStockClient.getProductInfo(TR_PRODUCT_INFO, ticker, PRDT_TYPE_STOCK));
        if (resp == null || !resp.isSuccess() || resp.getOutput() == null) return Optional.empty();
        return Optional.of(toStockProductData(resp.getOutput()));
    }

    private StockInfoData toStockInfoData(KisStockInfoOutput o) {
        return new StockInfoData(
                o.getListedShares(), o.getCapitalAmount(), o.getFaceValue(),
                o.getStckKindCd(), o.getEtfDvsnCd(), o.getReitsKindCd(), o.getKospi200ItemYn(),
                o.getIdxBztpLclsCd(), o.getIdxBztpMclsCd(), o.getIdxBztpSclsCd(),
                o.getIdxBztpLclsNm(), o.getIdxBztpMclsNm(), o.getIdxBztpSclsNm(),
                o.getStdIdstClsfCd(), o.getStdIdstClsfNm(),
                o.getFrnrPsnlLmtRt(), o.getTrStopYn(), o.getAdmnItemYn(),
                o.getLstgAbolDt(), o.getSctsMketLstgDt());
    }

    private StockProductData toStockProductData(KisProductInfoOutput o) {
        return new StockProductData(
                o.getPrdtName(), o.getPrdtAbrvName(), o.getPrdtEngName(),
                o.getShtnPdno(), o.getPrdtRiskGradCd(),
                o.getPrdtClsfCd(), o.getPrdtClsfName());
    }
}
