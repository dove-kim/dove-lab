package com.dove.kis.infrastructure.client;

import com.dove.kis.infrastructure.client.dto.KisOverseasDailyResponse;
import com.dove.kis.infrastructure.client.dto.KisOverseasPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * KIS 해외주식 시세 API Feign 클라이언트. 인증 헤더는 KisStockClientConfig 인터셉터가 부착한다.
 */
@FeignClient(
        name = "kis-overseas",
        url = "${kis.base-url:https://openapi.koreainvestment.com:9443}",
        configuration = KisStockClientConfig.class
)
public interface KisOverseasClient {

    /**
     * 해외주식 현재체결가를 조회한다.
     *
     * @param trId   거래 ID
     * @param auth   사용자권한정보(빈 문자열)
     * @param excd   거래소 코드(NAS/NYS/AMS/HKS/TSE/SHS/SZS)
     * @param symbol 종목 코드
     * @return 현재체결가 응답
     */
    @GetMapping("/uapi/overseas-price/v1/quotations/price")
    KisOverseasPriceResponse getPrice(
            @RequestHeader("tr_id") String trId,
            @RequestParam("AUTH") String auth,
            @RequestParam("EXCD") String excd,
            @RequestParam("SYMB") String symbol);

    /**
     * 해외주식 기간별시세를 조회한다. 기준일로부터 과거로 거슬러 올라가며 한 번에 최대 100건을 반환한다.
     *
     * @param trId   거래 ID
     * @param auth   사용자권한정보(빈 문자열)
     * @param excd   거래소 코드(NAS/NYS/AMS/HKS/TSE/SHS/SZS)
     * @param symbol 종목 코드
     * @param gubn   기간 구분(0=일, 1=주, 2=월)
     * @param bymd   조회 기준일자(yyyyMMdd)
     * @param modp   수정주가 반영 여부(0=미반영, 1=반영)
     * @return 기간별시세 응답
     */
    @GetMapping("/uapi/overseas-price/v1/quotations/dailyprice")
    KisOverseasDailyResponse getDailyPrice(
            @RequestHeader("tr_id") String trId,
            @RequestParam("AUTH") String auth,
            @RequestParam("EXCD") String excd,
            @RequestParam("SYMB") String symbol,
            @RequestParam("GUBN") String gubn,
            @RequestParam("BYMD") String bymd,
            @RequestParam("MODP") String modp);
}
