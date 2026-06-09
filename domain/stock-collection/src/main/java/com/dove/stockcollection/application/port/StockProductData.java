package com.dove.stockcollection.application.port;

/**
 * KIS 상품기본조회 결과의 도메인 표현.
 *
 * @param prdtName       상품명
 * @param prdtAbrvName   상품약어명
 * @param prdtEngName    상품영문명
 * @param shtnPdno       단축종목코드
 * @param prdtRiskGradCd 상품위험등급코드
 * @param prdtClsfCd     상품분류코드
 * @param prdtClsfName   상품분류명
 */
public record StockProductData(
        String prdtName,
        String prdtAbrvName,
        String prdtEngName,
        String shtnPdno,
        String prdtRiskGradCd,
        String prdtClsfCd,
        String prdtClsfName
) {}
