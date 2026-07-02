package com.dove.dart.infrastructure.client;

import com.dove.dart.infrastructure.client.dto.DartListResponse;
import com.dove.dart.infrastructure.client.dto.FnlttResponse;
import com.dove.dart.infrastructure.client.dto.StockTotalsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * DART OpenAPI 클라이언트.
 */
@FeignClient(
        name = "dart",
        url = "${dart.base-url:https://opendart.fss.or.kr/api}",
        configuration = DartClientConfig.class
)
public interface DartClient {

    /**
     * 단일회사 전체 재무제표 조회 (전 계정, 표준계정코드 포함).
     */
    @GetMapping("/fnlttSinglAcntAll.json")
    FnlttResponse getFinancialStatements(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bsns_year") String businessYear,
            @RequestParam("reprt_code") String reportCode,
            @RequestParam("fs_div") String fsDiv
    );

    /**
     * 주식의 총수 현황 조회 (보통주/우선주 발행·감소·유통 주식수).
     */
    @GetMapping("/stockTotqySttus.json")
    StockTotalsResponse getStockTotals(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bsns_year") String businessYear,
            @RequestParam("reprt_code") String reportCode
    );

    /**
     * 공시검색 (정기공시 신규·정정 감시용).
     */
    @GetMapping("/list.json")
    DartListResponse getDisclosureList(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bgn_de") String beginDate,
            @RequestParam("end_de") String endDate,
            @RequestParam("pblntf_ty") String publicationType,
            @RequestParam("page_no") int pageNo,
            @RequestParam("page_count") int pageCount
    );
}
