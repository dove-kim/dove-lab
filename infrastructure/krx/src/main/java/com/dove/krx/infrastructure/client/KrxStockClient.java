package com.dove.krx.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * KRX 한국거래소 Open API Feign 클라이언트.
 */
@FeignClient(
        name = "krx-stock-api",
        url = "https://data-dbg.krx.co.kr/svc/apis",
        configuration = KrxStockClientConfig.class
)
public interface KrxStockClient {
    @GetMapping("/sto/stk_bydd_trd")
    KrxDailyPriceResponse getDailyKospiStockInfo(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/ksq_bydd_trd")
    KrxDailyPriceResponse getDailyKosdaqStockInfo(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/knx_bydd_trd")
    KrxDailyPriceResponse getDailyKonexStockInfo(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/stk_isu_base_info")
    KrxListedStockResponse getKospiListedStocks(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/ksq_isu_base_info")
    KrxListedStockResponse getKosdaqListedStocks(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/knx_isu_base_info")
    KrxListedStockResponse getKonexListedStocks(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );
}
