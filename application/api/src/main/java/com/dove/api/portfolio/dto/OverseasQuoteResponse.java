package com.dove.api.portfolio.dto;

/**
 * 해외 종목 시세 검증 결과.
 *
 * @param valid 시세 조회 성공 여부(성공=시장·티커가 유효)
 * @param price 현재가(원통화, 실패면 null)
 */
public record OverseasQuoteResponse(boolean valid, Double price) {}
