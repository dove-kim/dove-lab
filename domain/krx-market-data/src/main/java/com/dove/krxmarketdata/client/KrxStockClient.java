package com.dove.krxmarketdata.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(
        name = "krx-stock-api",
        url = "https://data-dbg.krx.co.kr/svc/apis",
        configuration = KrxStockClientConfig.class
)
public interface KrxStockClient {
    @GetMapping("/sto/stk_bydd_trd")
    KrxStockResponse getDailyKospiStockInfo(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

    @GetMapping("/sto/ksq_bydd_trd")
    KrxStockResponse getDailyKosdaqStockInfo(
            @RequestHeader("AUTH_KEY") String authKey,
            @RequestParam("basDd") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate basDd
    );

}
