package com.dove.frankfurter.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Frankfurter 환율 API Feign 클라이언트.
 */
@FeignClient(name = "frankfurter", url = "${frankfurter.base-url:https://api.frankfurter.dev}")
public interface FrankfurterClient {

    /**
     * 기준 통화에 대한 대상 통화들의 최신 환율을 조회한다.
     *
     * @param base    기준 통화 코드
     * @param symbols 대상 통화 코드(콤마 구분)
     * @return 환율 응답
     */
    @GetMapping("/v1/latest")
    FrankfurterResponse latest(@RequestParam("base") String base, @RequestParam("symbols") String symbols);
}
