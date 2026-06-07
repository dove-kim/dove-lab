package com.dove.kis.infrastructure.client;

import com.dove.kis.infrastructure.client.dto.KisCurrentPriceResponse;
import com.dove.kis.infrastructure.client.dto.KisHolidayResponse;
import com.dove.kis.infrastructure.client.dto.KisEstimateResponse;
import com.dove.kis.infrastructure.client.dto.KisInvestOpinionResponse;
import com.dove.kis.infrastructure.client.dto.KisInvestorResponse;
import com.dove.kis.infrastructure.client.dto.KisKsdResponse;
import com.dove.kis.infrastructure.client.dto.KisPeriodChartResponse;
import com.dove.kis.infrastructure.client.dto.KisProductInfoResponse;
import com.dove.kis.infrastructure.client.dto.KisStockInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "kis-stock",
        url = "${kis.base-url:https://openapi.koreainvestment.com:9443}",
        configuration = KisStockClientConfig.class
)
public interface KisStockClient {

    /**
     * 주식현재가 시세 조회 (TR_ID: FHKST01010100).
     * marketDivCode: J=KRX, NX=NXT, UN=통합
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-price")
    KisCurrentPriceResponse getCurrentPrice(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketDivCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode
    );

    /**
     * 국내주식기간별시세 조회 (TR_ID: FHKST03010100). 최대 100건.
     * periodDivCode: D=일봉 W=주봉 M=월봉 Y=년봉
     * orgAdjPrc: 0=수정주가 1=원주가
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
    KisPeriodChartResponse getPeriodChart(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketDivCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode,
            @RequestParam("FID_INPUT_DATE_1") String fromDate,
            @RequestParam("FID_INPUT_DATE_2") String toDate,
            @RequestParam("FID_PERIOD_DIV_CODE") String periodDivCode,
            @RequestParam("FID_ORG_ADJ_PRC") String orgAdjPrc
    );

    /**
     * 주식기본조회 (TR_ID: CTPF1002R).
     * prdtTypeCd: 300=주식, 301=선물옵션
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/search-stock-info")
    KisStockInfoResponse getStockInfo(
            @RequestHeader("tr_id") String trId,
            @RequestParam("PDNO") String stockCode,
            @RequestParam("PRDT_TYPE_CD") String prdtTypeCd
    );

    /**
     * 상품기본조회 (TR_ID: CTPF1604R).
     * prdtTypeCd: 300=국내주식
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/search-info")
    KisProductInfoResponse getProductInfo(
            @RequestHeader("tr_id") String trId,
            @RequestParam("PDNO") String stockCode,
            @RequestParam("PRDT_TYPE_CD") String prdtTypeCd
    );

    /**
     * 국내휴장일조회 (TR_ID: CTCA0903R).
     * 영업일·거래일·개장일·결제일 여부를 조회한다.
     * ※ KIS 원장 연동이므로 1일 1회만 호출 권장.
     * bassDate: 기준일자 (yyyyMMdd)
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/chk-holiday")
    KisHolidayResponse getHoliday(
            @RequestHeader("tr_id") String trId,
            @RequestParam("BASS_DT") String bassDate,
            @RequestParam("CTX_AREA_FK") String ctxAreaFk,
            @RequestParam("CTX_AREA_NK") String ctxAreaNk
    );

    /**
     * 종목별 투자자매매동향 조회 (TR_ID: FHKST01010900).
     * marketDivCode: J/NX/UN 모두 동일한 통합 데이터를 반환한다.
     * fromDate/toDate: yyyyMMdd
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-investor")
    KisInvestorResponse getInvestorTrend(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketDivCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode,
            @RequestParam("FID_INPUT_DATE_1") String fromDate,
            @RequestParam("FID_INPUT_DATE_2") String toDate
    );

    // ── 예탁원정보(KSD) 권리 이벤트 — F_DT~T_DT 기간 조회, SHT_CD 공백=전체 ──────────────

    /** 예탁원정보(배당일정) TR_ID: HHKDB669102C0. GB1: 0=배당전체. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/dividend")
    KisKsdResponse getKsdDividend(
            @RequestHeader("tr_id") String trId,
            @RequestParam("CTS") String cts,
            @RequestParam("GB1") String gb1,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("SHT_CD") String shtCd,
            @RequestParam("HIGH_GB") String highGb
    );

    /** 예탁원정보(유상증자일정) TR_ID: HHKDB669100C0. GB1: 1=청약일별, 2=기준일별. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/paidin-capin")
    KisKsdResponse getKsdPaidinCapin(
            @RequestHeader("tr_id") String trId,
            @RequestParam("CTS") String cts,
            @RequestParam("GB1") String gb1,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("SHT_CD") String shtCd
    );

    /** 예탁원정보(무상증자일정) TR_ID: HHKDB669101C0. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/bonus-issue")
    KisKsdResponse getKsdBonusIssue(
            @RequestHeader("tr_id") String trId,
            @RequestParam("CTS") String cts,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("SHT_CD") String shtCd
    );

    /** 예탁원정보(합병/분할일정) TR_ID: HHKDB669104C0. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/merger-split")
    KisKsdResponse getKsdMergerSplit(
            @RequestHeader("tr_id") String trId,
            @RequestParam("CTS") String cts,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("SHT_CD") String shtCd
    );

    /** 예탁원정보(액면교체일정) TR_ID: HHKDB669105C0. MARKET_GB: 0=전체. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/rev-split")
    KisKsdResponse getKsdRevSplit(
            @RequestHeader("tr_id") String trId,
            @RequestParam("SHT_CD") String shtCd,
            @RequestParam("CTS") String cts,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("MARKET_GB") String marketGb
    );

    /** 예탁원정보(자본감소일정) TR_ID: HHKDB669106C0. */
    @GetMapping("/uapi/domestic-stock/v1/ksdinfo/cap-dcrs")
    KisKsdResponse getKsdCapDcrs(
            @RequestHeader("tr_id") String trId,
            @RequestParam("CTS") String cts,
            @RequestParam("F_DT") String fromDt,
            @RequestParam("T_DT") String toDt,
            @RequestParam("SHT_CD") String shtCd
    );

    // ── 애널리스트 정보 (on-demand 조회) ──────────────────────────────────────────

    /** 국내주식 종목투자의견 TR_ID: FHKST663300C0. mrkt=J, scr=16633. */
    @GetMapping("/uapi/domestic-stock/v1/quotations/invest-opinion")
    KisInvestOpinionResponse getInvestOpinion(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketDivCode,
            @RequestParam("FID_COND_SCR_DIV_CODE") String screenDivCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode,
            @RequestParam("FID_INPUT_DATE_1") String fromDate,
            @RequestParam("FID_INPUT_DATE_2") String toDate
    );

    /** 국내주식 종목추정실적 TR_ID: HHKST668300C0. */
    @GetMapping("/uapi/domestic-stock/v1/quotations/estimate-perform")
    KisEstimateResponse getEstimatePerform(
            @RequestHeader("tr_id") String trId,
            @RequestParam("SHT_CD") String stockCode
    );
}
