package com.dove.dart.application.dto;

/**
 * DART 고유번호 ↔ 종목코드 매핑(상장사).
 *
 * @param corpCode  DART 고유번호(8자리)
 * @param corpName  회사명
 * @param stockCode 종목코드(6자리)
 */
public record CorpMapping(
        String corpCode,
        String corpName,
        String stockCode
) {
}
