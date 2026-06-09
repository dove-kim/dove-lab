package com.dove.stockcollection.application.port;

/**
 * KIS 주식기본조회 결과의 도메인 표현.
 *
 * @param listedShares    상장주수
 * @param capitalAmount   자본금
 * @param faceValue       액면가
 * @param stockKindCd     주식종류코드
 * @param etfDvsnCd       ETF구분코드
 * @param reitsKindCd     REITs종류코드
 * @param kospi200ItemYn  KOSPI200 편입 여부
 * @param idxBztpLclsCd   업종 대분류 코드
 * @param idxBztpMclsCd   업종 중분류 코드
 * @param idxBztpSclsCd   업종 소분류 코드
 * @param idxBztpLclsNm   업종 대분류명
 * @param idxBztpMclsNm   업종 중분류명
 * @param idxBztpSclsNm   업종 소분류명
 * @param stdIdstClsfCd   표준산업분류코드
 * @param stdIdstClsfNm   표준산업분류명
 * @param frnrPsnlLmtRt   외국인 개인 한도 비율
 * @param trStopYn        거래정지 여부
 * @param admnItemYn      관리종목 여부
 * @param lstgAbolDt      상장폐지일
 * @param sctsMketLstgDt  증권시장 상장일
 */
public record StockInfoData(
        Long listedShares,
        Long capitalAmount,
        Long faceValue,
        String stockKindCd,
        String etfDvsnCd,
        String reitsKindCd,
        String kospi200ItemYn,
        String idxBztpLclsCd,
        String idxBztpMclsCd,
        String idxBztpSclsCd,
        String idxBztpLclsNm,
        String idxBztpMclsNm,
        String idxBztpSclsNm,
        String stdIdstClsfCd,
        String stdIdstClsfNm,
        String frnrPsnlLmtRt,
        String trStopYn,
        String admnItemYn,
        String lstgAbolDt,
        String sctsMketLstgDt
) {}
